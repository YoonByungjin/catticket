package com.catticket.common.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.Scheduled;

import com.catticket.collect.batch.DailyCollectJob;

/**
 * ScheduledTasks（06 の 5.1）: catticket.scheduler.enabled が true のときだけ作られ、毎日 06:00（Asia/Seoul）に B-01 を呼ぶ。
 */
class ScheduledTasksTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(DailyCollectJob.class, () -> mock(DailyCollectJob.class))
		.withUserConfiguration(ScheduledTasks.class);

	@Test
	void createdOnlyWhenSchedulerIsEnabled() {
		contextRunner.withPropertyValues("catticket.scheduler.enabled=true")
			.run(ctx -> assertThat(ctx).hasSingleBean(ScheduledTasks.class));
		contextRunner.withPropertyValues("catticket.scheduler.enabled=false")
			.run(ctx -> assertThat(ctx).doesNotHaveBean(ScheduledTasks.class));
		// 設定がなければ作らない（自動で動かすのは明示したときだけ）
		contextRunner.run(ctx -> assertThat(ctx).doesNotHaveBean(ScheduledTasks.class));
	}

	@Test
	void dailyCollectRunsAtSixInSeoul() throws Exception {
		Scheduled scheduled = ScheduledTasks.class.getMethod("dailyCollect").getAnnotation(Scheduled.class);

		assertThat(scheduled.cron()).isEqualTo("0 0 6 * * *");
		assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
		// B-04 は auth の Issue で足す。今はこの1つだけ
		assertThat(java.util.Arrays.stream(ScheduledTasks.class.getDeclaredMethods())
			.filter(m -> m.isAnnotationPresent(Scheduled.class))).hasSize(1);
	}

	@Test
	void dailyCollectCallsTheJobAndDoesNotThrow() {
		DailyCollectJob job = mock(DailyCollectJob.class);
		when(job.run()).thenReturn(JobResult.PARTIAL_FAILURE);
		new ScheduledTasks(job).dailyCollect();
		verify(job).run();

		when(job.run()).thenThrow(new IllegalStateException("db down"));
		new ScheduledTasks(job).dailyCollect();
	}

}
