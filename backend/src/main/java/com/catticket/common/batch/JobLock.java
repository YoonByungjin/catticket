package com.catticket.common.batch;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;

/**
 * 二重起動の防止（05 の 1.1、06 の 5.2）。MySQL の名前付きロック {@code catticket-{task}} を専用の接続で取る。
 * 名前付きロックは接続に付き、コミット・ロールバックでは外れないので、ジョブの終わりまで同じ接続を持ち続ける。
 * トランザクションの中で呼ばない（Spring のトランザクションの接続を共有してしまうため）。
 * 外せたかわからない接続はプールに戻さずに捨てる（ロック付きの接続が残ると、次の実行がずっと LOCKED になるため）。
 */
@Component
public class JobLock {

	private static final Logger log = LoggerFactory.getLogger(JobLock.class);

	private final SqlSessionFactory sqlSessionFactory;

	private final HikariDataSource pool;

	public JobLock(SqlSessionFactory sqlSessionFactory, DataSource dataSource) {
		this.sqlSessionFactory = sqlSessionFactory;
		try {
			this.pool = dataSource.unwrap(HikariDataSource.class);
		}
		catch (SQLException e) {
			// 接続を捨てられないまま動かさない
			throw new IllegalStateException("JobLock requires HikariDataSource to evict connections", e);
		}
	}

	public static String lockName(TaskName task) {
		return "catticket-" + task.value();
	}

	/** 取れたら Held（close で外す）。同じバッチが動いていれば空 */
	public Optional<Held> tryAcquire(TaskName task) {
		return tryAcquire(lockName(task));
	}

	/** ロック名を直接指定する（テストで、ほかのチェックアウトのテストとぶつからない名前を使うため） */
	Optional<Held> tryAcquire(String name) {
		SqlSession session = sqlSessionFactory.openSession();
		Integer result;
		try {
			result = session.getMapper(JobLockMapper.class).getLock(name);
		}
		catch (RuntimeException e) {
			// ロックが付いたかわからないので、接続ごと捨てる
			log.error("Failed to get lock {}: {}", name, BatchRunRecorder.describe(e));
			discard(session, name);
			throw e;
		}
		if (result != null && result == 1) {
			return Optional.of(new Held(this, session, name));
		}
		session.close();
		return Optional.empty();
	}

	/** RELEASE_LOCK して、外れたときだけ接続をプールに戻す */
	void releaseAndClose(SqlSession session, String lockName) {
		Integer result;
		try {
			result = session.getMapper(JobLockMapper.class).releaseLock(lockName);
		}
		catch (RuntimeException e) {
			log.error("Failed to release lock {}: {}", lockName, BatchRunRecorder.describe(e));
			discard(session, lockName);
			return;
		}
		if (result != null && result == 1) {
			session.close();
			return;
		}
		// 0・NULL は同じセッションで取ったロックなら起きない。接続の状態がわからないので捨てる
		log.error("Failed to release lock {}: RELEASE_LOCK returned {}", lockName, result);
		discard(session, lockName);
	}

	/**
	 * 接続をプールから捨てる。閉じる前（借りている間）に evictConnection を呼ぶとすぐに外され、
	 * 物理接続が閉じてセッションが終わり、MySQL がロックを外す。閉じた後だとほかのスレッドが借りうるので順番を守る。
	 * ジョブの結果を変えないよう、例外はログだけにする。
	 */
	void discard(SqlSession session, String lockName) {
		try {
			Connection connection = session.getConnection();
			pool.evictConnection(connection);
		}
		catch (RuntimeException e) {
			log.error("Failed to evict connection of lock {}: {}", lockName, BatchRunRecorder.describe(e));
		}
		finally {
			try {
				// MyBatis-Spring の資源を片付けるため省かない（捨てたプロキシを閉じるだけ）
				session.close();
			}
			catch (RuntimeException e) {
				log.error("Failed to close session of lock {}: {}", lockName, BatchRunRecorder.describe(e));
			}
		}
	}

	/** 取ったロック。close で RELEASE_LOCK してから接続を閉じる */
	public static final class Held implements AutoCloseable {

		private final JobLock owner;

		private final SqlSession session;

		private final String name;

		private boolean closed;

		private Held(JobLock owner, SqlSession session, String name) {
			this.owner = owner;
			this.session = session;
			this.name = name;
		}

		public String name() {
			return name;
		}

		@Override
		public void close() {
			if (closed) {
				return;
			}
			closed = true;
			owner.releaseAndClose(session, name);
		}

	}

}
