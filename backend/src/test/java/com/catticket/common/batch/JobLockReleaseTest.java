package com.catticket.common.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.zaxxer.hikari.HikariDataSource;

/**
 * JobLock の外し方と接続の捨て方（06 の 5.2）。SqlSession・HikariDataSource などはモック。
 */
class JobLockReleaseTest {

	private static final String NAME = "catticket-test-release";

	private SqlSessionFactory factory;

	private SqlSession session;

	private JobLockMapper lockMapper;

	private HikariDataSource pool;

	private Connection connection;

	private JobLock jobLock;

	@BeforeEach
	void setUp() throws SQLException {
		factory = mock(SqlSessionFactory.class);
		session = mock(SqlSession.class);
		lockMapper = mock(JobLockMapper.class);
		pool = mock(HikariDataSource.class);
		connection = mock(Connection.class);
		DataSource dataSource = mock(DataSource.class);
		when(dataSource.unwrap(HikariDataSource.class)).thenReturn(pool);
		when(factory.openSession()).thenReturn(session);
		when(session.getMapper(JobLockMapper.class)).thenReturn(lockMapper);
		when(session.getConnection()).thenReturn(connection);
		jobLock = new JobLock(factory, dataSource);
	}

	private JobLock.Held acquire() {
		when(lockMapper.getLock(NAME)).thenReturn(1);
		return jobLock.tryAcquire(NAME).orElseThrow();
	}

	@Test
	void releasedConnectionReturnsToPool() {
		JobLock.Held held = acquire();
		when(lockMapper.releaseLock(NAME)).thenReturn(1);

		held.close();

		verify(pool, never()).evictConnection(any());
		verify(session).close();
	}

	@Test
	void releaseReturningZeroEvictsBeforeClose() {
		JobLock.Held held = acquire();
		when(lockMapper.releaseLock(NAME)).thenReturn(0);

		held.close();

		InOrder order = inOrder(pool, session);
		order.verify(pool).evictConnection(connection);
		order.verify(session).close();
	}

	@Test
	void releaseReturningNullEvictsBeforeClose() {
		JobLock.Held held = acquire();
		when(lockMapper.releaseLock(NAME)).thenReturn(null);

		held.close();

		InOrder order = inOrder(pool, session);
		order.verify(pool).evictConnection(connection);
		order.verify(session).close();
	}

	@Test
	void releaseExceptionEvictsBeforeCloseAndDoesNotThrow() {
		JobLock.Held held = acquire();
		when(lockMapper.releaseLock(NAME)).thenThrow(new IllegalStateException("connection lost"));

		assertThatCode(held::close).doesNotThrowAnyException();

		InOrder order = inOrder(pool, session);
		order.verify(pool).evictConnection(connection);
		order.verify(session).close();
	}

	@Test
	void evictExceptionStillClosesAndDoesNotThrow() {
		JobLock.Held held = acquire();
		when(lockMapper.releaseLock(NAME)).thenReturn(0);
		doThrow(new IllegalStateException("evict failed")).when(pool).evictConnection(connection);

		assertThatCode(held::close).doesNotThrowAnyException();

		verify(session).close();
	}

	@Test
	void secondCloseDoesNothing() {
		JobLock.Held held = acquire();
		when(lockMapper.releaseLock(NAME)).thenReturn(1);

		held.close();
		held.close();

		verify(lockMapper).releaseLock(NAME);
		verify(session).close();
	}

	@Test
	void getLockExceptionEvictsAndRethrows() {
		IllegalStateException failure = new IllegalStateException("timeout");
		when(lockMapper.getLock(NAME)).thenThrow(failure);

		assertThatThrownBy(() -> jobLock.tryAcquire(NAME)).isSameAs(failure);

		InOrder order = inOrder(pool, session);
		order.verify(pool).evictConnection(connection);
		order.verify(session).close();
	}

	@Test
	void getLockNotAcquiredClosesWithoutEvict() {
		when(lockMapper.getLock(NAME)).thenReturn(0);
		assertThat(jobLock.tryAcquire(NAME)).isEqualTo(Optional.empty());

		when(lockMapper.getLock(NAME)).thenReturn(null);
		assertThat(jobLock.tryAcquire(NAME)).isEmpty();

		verify(pool, never()).evictConnection(any());
		verify(session, org.mockito.Mockito.times(2)).close();
	}

	@Test
	void nonHikariDataSourceFailsAtStartup() throws SQLException {
		DataSource other = mock(DataSource.class);
		when(other.unwrap(HikariDataSource.class)).thenThrow(new SQLException("not a wrapper"));

		assertThatThrownBy(() -> new JobLock(factory, other)).isInstanceOf(IllegalStateException.class);
	}

}
