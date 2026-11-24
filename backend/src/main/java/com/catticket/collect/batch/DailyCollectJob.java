package com.catticket.collect.batch;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.catticket.collect.AwardCollectStep;
import com.catticket.collect.GenreTrendStep;
import com.catticket.collect.MissingPerformanceStep;
import com.catticket.collect.PerformanceCollectStep;
import com.catticket.collect.RankingCollectStep;
import com.catticket.common.batch.BatchContext;
import com.catticket.common.batch.BatchJob;
import com.catticket.common.batch.BatchJobRunner;
import com.catticket.common.batch.BatchRunRecorder;
import com.catticket.common.batch.JobResult;
import com.catticket.common.batch.StepResult;
import com.catticket.common.batch.StepRun;
import com.catticket.common.batch.TaskName;
import com.catticket.common.time.DateTimes;
import com.catticket.translation.TranslationStep;

/**
 * B-01 {@code daily-collect}（05 の 3.2、06 の 5.3）。S1〜S4・S6・S7 を順に実行する（S5 は欠番）。
 * 前のステップが失敗しても次のステップを実行し、1つでも失敗したら PARTIAL_FAILURE にする。
 */
@Component
public class DailyCollectJob implements BatchJob {

	private static final Logger log = LoggerFactory.getLogger(DailyCollectJob.class);

	private final BatchJobRunner runner;

	private final BatchRunRecorder recorder;

	private final Clock clock;

	private final PerformanceCollectStep performanceStep;

	private final RankingCollectStep rankingStep;

	private final AwardCollectStep awardStep;

	private final MissingPerformanceStep missingStep;

	private final GenreTrendStep genreTrendStep;

	private final TranslationStep translationStep;

	public DailyCollectJob(BatchJobRunner runner, BatchRunRecorder recorder, Clock clock,
			PerformanceCollectStep performanceStep, RankingCollectStep rankingStep, AwardCollectStep awardStep,
			MissingPerformanceStep missingStep, GenreTrendStep genreTrendStep, TranslationStep translationStep) {
		this.runner = runner;
		this.recorder = recorder;
		this.clock = clock;
		this.performanceStep = performanceStep;
		this.rankingStep = rankingStep;
		this.awardStep = awardStep;
		this.missingStep = missingStep;
		this.genreTrendStep = genreTrendStep;
		this.translationStep = translationStep;
	}

	@Override
	public TaskName name() {
		return TaskName.DAILY_COLLECT;
	}

	@Override
	public JobResult run() {
		return runner.execute(TaskName.DAILY_COLLECT, this::runSteps);
	}

	JobResult runSteps(BatchContext ctx) {
		// S4 の入力（S2・S3 で見た公演 ID）。受賞作かどうかを S4 に渡すため2つに分ける（06 の 5.3）
		Set<String> rankingIds = new LinkedHashSet<>();
		Set<String> awardIds = new LinkedHashSet<>();
		boolean allSucceeded = true;
		allSucceeded &= runStep(ctx, PerformanceCollectStep.NAME, performanceStep::run);
		allSucceeded &= runStep(ctx, RankingCollectStep.NAME, c -> rankingStep.run(c, rankingIds));
		allSucceeded &= runStep(ctx, AwardCollectStep.NAME, c -> awardStep.run(c, awardIds));
		// S2・S3 が途中で止まっても、それまでに足した ID で行う（05 の 3.2）
		allSucceeded &= runStep(ctx, MissingPerformanceStep.NAME, c -> missingStep.run(c, rankingIds, awardIds));
		allSucceeded &= runStep(ctx, GenreTrendStep.NAME, genreTrendStep::run);
		allSucceeded &= runStep(ctx, TranslationStep.NAME, translationStep::run);
		return allSucceeded ? JobResult.SUCCESS : JobResult.PARTIAL_FAILURE;
	}

	/**
	 * 1つのステップを記録しながら実行する。例外（StepAbortedException を含む）はそのステップの失敗にし、次へ進めるよう投げない。
	 *
	 * @return ステップが成功したか
	 */
	private boolean runStep(BatchContext ctx, String stepName, Function<BatchContext, StepResult> step) {
		StepRun run = recorder.startStep(ctx.jobRunId(), stepName, DateTimes.now(clock));
		StepResult result;
		try {
			result = step.apply(ctx);
			if (result == null) {
				throw new IllegalStateException("step returned null");
			}
		}
		catch (RuntimeException e) {
			String message = BatchRunRecorder.describe(e);
			log.error("Step {} failed: {}", stepName, message, e);
			result = StepResult.failure(null, message);
		}
		recorder.finishStep(run, result, DateTimes.now(clock));
		if (!result.success()) {
			log.warn("Step {} finished with failure: {}", stepName, result.errorMessage());
		}
		return result.success();
	}

}
