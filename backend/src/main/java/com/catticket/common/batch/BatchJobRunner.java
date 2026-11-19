package com.catticket.common.batch;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.catticket.common.time.DateTimes;

/**
 * ジョブの共通の流れ（06 の 5.2）: ロック → 記録の開始 → 本体 → 記録の終了 → ロックを外す。
 */
@Component
public class BatchJobRunner {

	private static final Logger log = LoggerFactory.getLogger(BatchJobRunner.class);

	private final JobLock jobLock;

	private final BatchRunRecorder recorder;

	private final Clock clock;

	public BatchJobRunner(JobLock jobLock, BatchRunRecorder recorder, Clock clock) {
		this.jobLock = jobLock;
		this.recorder = recorder;
		this.clock = clock;
	}

	public JobResult execute(TaskName task, Function<BatchContext, JobResult> body) {
		Optional<JobLock.Held> lock = jobLock.tryAcquire(task);
		if (lock.isEmpty()) {
			// 同じバッチが動いている。記録は作らない
			log.warn("Batch {} is already running (lock {} not acquired). Skipped.", task.value(),
					JobLock.lockName(task));
			return JobResult.LOCKED;
		}
		try (JobLock.Held held = lock.get()) {
			LocalDateTime now = DateTimes.now(clock);
			long jobRunId = recorder.start(task, now);
			log.info("Batch {} started (jobRunId={})", task.value(), jobRunId);
			JobResult result;
			String errorMessage = null;
			try {
				result = body.apply(new BatchContext(jobRunId, now, DateTimes.today(clock)));
				if (result == null || result == JobResult.LOCKED) {
					throw new IllegalStateException("job returned " + result);
				}
			}
			catch (RuntimeException e) {
				result = JobResult.FAILURE;
				errorMessage = BatchRunRecorder.describe(e);
				log.error("Batch {} failed: {}", task.value(), errorMessage, e);
			}
			recorder.finish(jobRunId, result, errorMessage, DateTimes.now(clock));
			log.info("Batch {} finished: {} (jobRunId={})", task.value(), result, jobRunId);
			return result;
		}
	}

}
