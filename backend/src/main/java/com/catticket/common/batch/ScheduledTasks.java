package com.catticket.common.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.catticket.collect.batch.DailyCollectJob;

/**
 * バッチの自動の実行（05 の 1.1、06 の 5.1）。catticket.scheduler.enabled が true のときだけ作る
 * （テストと手での実行（--catticket.task=）では false にして動かさない）。
 * 二重起動は BatchJobRunner のロックが防ぐ。
 */
@Component
@ConditionalOnProperty(name = "catticket.scheduler.enabled", havingValue = "true")
public class ScheduledTasks {

	static final String DAILY_COLLECT_CRON = "0 0 6 * * *";

	static final String ZONE = "Asia/Seoul";

	private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

	private final DailyCollectJob dailyCollectJob;

	public ScheduledTasks(DailyCollectJob dailyCollectJob) {
		this.dailyCollectJob = dailyCollectJob;
	}

	/** B-01 日次収集。毎日 06:00（Asia/Seoul） */
	@Scheduled(cron = DAILY_COLLECT_CRON, zone = ZONE)
	public void dailyCollect() {
		try {
			JobResult result = dailyCollectJob.run();
			log.info("Scheduled {} finished: {}", TaskName.DAILY_COLLECT.value(), result);
		}
		catch (RuntimeException e) {
			// ロック・記録の DB に届かないときなど。次の日の実行は続ける
			log.error("Scheduled {} failed: {}", TaskName.DAILY_COLLECT.value(), BatchRunRecorder.describe(e), e);
		}
	}

}
