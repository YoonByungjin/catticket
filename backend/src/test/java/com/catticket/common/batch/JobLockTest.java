package com.catticket.common.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * JobLock（実際の MySQL の GET_LOCK）。テストのトランザクションの中だと接続が1つに共有され、
 * 同じ接続の GET_LOCK は2回目も取れてしまうので、トランザクションなしで動かす（本番もトランザクションの外で呼ぶ）。
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JobLockTest {

	@Autowired
	SqlSessionFactory sqlSessionFactory;

	@Autowired
	DataSource dataSource;

	@Autowired
	JdbcTemplate jdbcTemplate;

	/** ロックは MySQL の全体で共有されるので、ほかのチェックアウトのテストとぶつからない名前にする */
	private static String uniqueName() {
		return "catticket-test-" + UUID.randomUUID().toString().substring(0, 8);
	}

	@Test
	void secondAcquireFailsWhileHeldAndSucceedsAfterRelease() {
		JobLock jobLock = new JobLock(sqlSessionFactory, dataSource);
		String name = uniqueName();

		Optional<JobLock.Held> first = jobLock.tryAcquire(name);
		assertThat(first).isPresent();
		try {
			// 別の接続からは取れない
			assertThat(jobLock.tryAcquire(name)).isEmpty();
		}
		finally {
			first.get().close();
		}

		Optional<JobLock.Held> again = jobLock.tryAcquire(name);
		assertThat(again).isPresent();
		again.get().close();
	}

	@Test
	void differentNamesDoNotBlockEachOther() {
		JobLock jobLock = new JobLock(sqlSessionFactory, dataSource);
		try (JobLock.Held a = jobLock.tryAcquire(uniqueName()).orElseThrow();
				JobLock.Held b = jobLock.tryAcquire(uniqueName()).orElseThrow()) {
			assertThat(a.name()).isNotEqualTo(b.name());
		}
	}

	@Test
	void closeTwiceIsHarmless() {
		JobLock jobLock = new JobLock(sqlSessionFactory, dataSource);
		String name = uniqueName();
		JobLock.Held held = jobLock.tryAcquire(name).orElseThrow();
		held.close();
		held.close();
		jobLock.tryAcquire(name).orElseThrow().close();
	}

	@Test
	void lockNameIsCatticketPrefixedTaskName() {
		assertThat(JobLock.lockName(TaskName.IMPORT_SUBWAY)).isEqualTo("catticket-import-subway");
		// MySQL のロック名の上限 64 文字以内（06 の 5.2）
		for (TaskName task : TaskName.values()) {
			assertThat(JobLock.lockName(task).length()).isLessThanOrEqualTo(64);
		}
	}

	@Test
	void sessionConnectionIsHikariProxy() {
		// evictConnection はクラス名が com.zaxxer.hikari で始まらない接続を黙って無視する。
		// DataSource をラップするライブラリを入れたときにここで気づく（06 の 5.2）
		try (SqlSession session = sqlSessionFactory.openSession()) {
			assertThat(session.getConnection().getClass().getName()).startsWith("com.zaxxer.hikari");
		}
	}

	@Test
	void discardedConnectionReleasesLock() throws InterruptedException {
		JobLock jobLock = new JobLock(sqlSessionFactory, dataSource);
		String held = uniqueName();
		String notHeld = uniqueName();
		SqlSession session = sqlSessionFactory.openSession();
		assertThat(session.getMapper(JobLockMapper.class).getLock(held)).isEqualTo(1);

		// 取っていない名前を外す → NULL → 接続ごと捨てる
		jobLock.releaseAndClose(session, notHeld);

		// 物理接続は HikariCP の別のスレッドで閉じるので、少し待つ。
		// GET_LOCK は同じセッションなら2回目も取れ、プールは同じスレッドに同じ接続を返しやすいので、
		// tryAcquire だけでは捨てなくても通ってしまう。どの接続から見ても空いているか（IS_FREE_LOCK）で確かめる
		long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
		boolean free = isFree(held);
		while (!free && System.nanoTime() < deadline) {
			Thread.sleep(100);
			free = isFree(held);
		}
		assertThat(free).as("lock should be released when the connection is discarded").isTrue();
		Optional<JobLock.Held> again = jobLock.tryAcquire(held);
		assertThat(again).isPresent();
		again.get().close();
	}

	/** 1 = だれも持っていない。持っている接続から聞いても 0 になる */
	private boolean isFree(String name) {
		Integer result = jdbcTemplate.queryForObject("SELECT IS_FREE_LOCK(?)", Integer.class, name);
		return result != null && result == 1;
	}

}
