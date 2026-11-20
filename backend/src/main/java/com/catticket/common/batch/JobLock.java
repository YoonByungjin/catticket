package com.catticket.common.batch;

import java.util.Optional;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 二重起動の防止（05 の 1.1、06 の 5.2）。MySQL の名前付きロック {@code catticket-{task}} を専用の接続で取る。
 * 名前付きロックは接続に付き、コミット・ロールバックでは外れないので、ジョブの終わりまで同じ接続を持ち続ける。
 * トランザクションの中で呼ばない（Spring のトランザクションの接続を共有してしまうため）。
 */
@Component
public class JobLock {

	private static final Logger log = LoggerFactory.getLogger(JobLock.class);

	private final SqlSessionFactory sqlSessionFactory;

	public JobLock(SqlSessionFactory sqlSessionFactory) {
		this.sqlSessionFactory = sqlSessionFactory;
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
		boolean acquired = false;
		try {
			Integer result = session.getMapper(JobLockMapper.class).getLock(name);
			acquired = result != null && result == 1;
			return acquired ? Optional.of(new Held(session, name)) : Optional.empty();
		}
		finally {
			if (!acquired) {
				session.close();
			}
		}
	}

	/** 取ったロック。close で RELEASE_LOCK してから接続を閉じる */
	public static final class Held implements AutoCloseable {

		private final SqlSession session;

		private final String name;

		private boolean closed;

		private Held(SqlSession session, String name) {
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
			try {
				session.getMapper(JobLockMapper.class).releaseLock(name);
			}
			catch (RuntimeException e) {
				// 外せなくても接続は閉じる。ジョブの結果は変えない
				log.error("Failed to release lock {}: {}", name, e.toString());
			}
			finally {
				session.close();
			}
		}

	}

}
