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
import com.catticket.collect.client.KopisCallPolicy;
import com.catticket.common.batch.BatchContext;
import com.catticket.common.batch.BatchJob;
import com.catticket.common.batch.BatchJobRunner;
import com.catticket.common.batch.BatchRunRecorder;
import com.catticket.common.batch.JobResult;
import com.catticket.common.batch.StepResult;
import com.catticket.common.batch.StepRun;
import com.catticket.common.batch.StepSummary;
import com.catticket.common.batch.TaskName;
import com.catticket.common.time.DateTimes;
import com.catticket.translation.TranslationStep;

/**
 * B-01 {@code daily-collect}（05 の 3.2、06 の 5.3）。S1〜S4・S6・S7 を順に実行する（S5 は欠番）。
 * 前のステップが失敗しても次のステップを実行し、1つでも失敗したら PARTIAL_FAILURE にする。
 * ただし KOPIS の呼び出しを止めた後（遮断の印・続けて 5 回の失敗）は、残りの KOPIS のステップを呼ばずに失敗として記録する（05 の 3.2・3.5）。
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

	private final KopisCallPolicy policy;

	public DailyCollectJob(BatchJobRunner runner, BatchRunRecorder recorder, Clock clock,
			PerformanceCollectStep performanceStep, RankingCollectStep rankingStep, AwardCollectStep awardStep,
			MissingPerformanceStep missingStep, GenreTrendStep genreTrendStep, TranslationStep translationStep,
			KopisCallPolicy policy) {
		this.runner = runner;
		this.recorder = recorder;
		this.clock = clock;
		this.policy = policy;
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
		// 止めた印・続けての失敗の数・送信の数は実行ごと（06 の 5.3）
		policy.startJob();
		boolean allSucceeded = true;
		allSucceeded &= runKopisStep(ctx, PerformanceCollectStep.NAME, performanceStep::run);
		allSucceeded &= runKopisStep(ctx, RankingCollectStep.NAME, c -> rankingStep.run(c, rankingIds));
		allSucceeded &= runKopisStep(ctx, AwardCollectStep.NAME, c -> awardStep.run(c, awardIds));
		// S2・S3 が途中で止まっても、それまでに足した ID で行う（05 の 3.2）
		allSucceeded &= runKopisStep(ctx, MissingPerformanceStep.NAME,
				c -> missingStep.run(c, rankingIds, awardIds));
		allSucceeded &= runKopisStep(ctx, GenreTrendStep.NAME, genreTrendStep::run);
		// S7 は KOPIS を呼ばないので、止めた後も行う（05 の 3.5）
		StepRun s7 = recorder.startStep(ctx.jobRunId(), TranslationStep.NAME, DateTimes.now(clock));
		allSucceeded &= finish(s7, invoke(ctx, TranslationStep.NAME, translationStep::run));
		if (policy.stopped()) {
			log.info("KOPIS calls in this run: {}/{}, stopped: {}", policy.runCalls(), policy.perRun(),
					policy.stopMessage());
		}
		else {
			log.info("KOPIS calls in this run: {}/{}", policy.runCalls(), policy.perRun());
		}
		return allSucceeded ? JobResult.SUCCESS : JobResult.PARTIAL_FAILURE;
	}

	/**
	 * KOPIS のステップ（S1〜S4・S6）を記録しながら実行する（06 の 5.3 の 2.）。
	 * その実行の KOPIS を止めた後・割り当てが 0 のときは、呼ばずに失敗として記録する。
	 *
	 * @return ステップが成功したか
	 */
	private boolean runKopisStep(BatchContext ctx, String stepName, Function<BatchContext, StepResult> step) {
		StepRun run = recorder.startStep(ctx.jobRunId(), stepName, DateTimes.now(clock));
		if (policy.stopped()) {
			return finish(run, StepResult.failure(kopisCalls(null, 0),
					"skipped: KOPIS calls stopped in this run at " + policy.stopMessage()));
		}
		policy.startStep(stepName);
		if (policy.allowance() == 0) {
			return finish(run, StepResult.failure(kopisCalls(null, 0),
					"skipped: no KOPIS call allowance left (" + policy.runCalls() + "/" + policy.perRun() + ")"));
		}
		StepResult result = invoke(ctx, stepName, step);
		StepSummary summary = kopisCalls(result.summary(), policy.stepCalls());
		if (result.summary() == null) {
			// 例外で終わったステップにも送信の数を残す
			result = new StepResult(result.success(), result.processedCount(), summary, result.errorMessage(),
					result.windowEndDate(), result.collectBaseDate());
		}
		return finish(run, result);
	}

	/** ステップを実行する。例外はそのステップの失敗にし、次へ進めるよう投げない */
	private StepResult invoke(BatchContext ctx, String stepName, Function<BatchContext, StepResult> step) {
		try {
			StepResult result = step.apply(ctx);
			if (result == null) {
				throw new IllegalStateException("step returned null");
			}
			return result;
		}
		catch (RuntimeException e) {
			String message = BatchRunRecorder.describe(e);
			log.error("Step {} failed: {}", stepName, message, e);
			return StepResult.failure(null, message);
		}
	}

	private boolean finish(StepRun run, StepResult result) {
		recorder.finishStep(run, result, DateTimes.now(clock));
		if (!result.success()) {
			log.warn("Step {} finished with failure: {}", run.stepName(), result.errorMessage());
		}
		return result.success();
	}

	// summary に kopisCalls を足す（記録だけ。failed には入れない）。null なら kopisCalls だけの summary を作る
	private static StepSummary kopisCalls(StepSummary summary, int calls) {
		StepSummary target = summary == null ? new StepSummary() : summary;
		target.add("kopisCalls", calls);
		return target;
	}

}
