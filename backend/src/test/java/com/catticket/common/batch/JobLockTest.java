package com.catticket.common.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
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

	/** ロックは MySQL の全体で共有されるので、ほかのチェックアウトのテストとぶつからない名前にする */
	private static String uniqueName() {
		return "catticket-test-" + UUID.randomUUID().toString().substring(0, 8);
	}

	@Test
	void secondAcquireFailsWhileHeldAndSucceedsAfterRelease() {
		JobLock jobLock = new JobLock(sqlSessionFactory);
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
		JobLock jobLock = new JobLock(sqlSessionFactory);
		try (JobLock.Held a = jobLock.tryAcquire(uniqueName()).orElseThrow();
				JobLock.Held b = jobLock.tryAcquire(uniqueName()).orElseThrow()) {
			assertThat(a.name()).isNotEqualTo(b.name());
		}
	}

	@Test
	void closeTwiceIsHarmless() {
		JobLock jobLock = new JobLock(sqlSessionFactory);
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

}
