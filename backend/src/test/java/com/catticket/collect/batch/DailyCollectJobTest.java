package com.catticket.collect.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.catticket.collect.AwardCollectStep;
import com.catticket.collect.GenreTrendStep;
import com.catticket.collect.MissingPerformanceStep;
import com.catticket.collect.PerformanceCollectStep;
import com.catticket.collect.RankingCollectStep;
import com.catticket.collect.client.KopisCallPolicy;
import com.catticket.common.batch.BatchContext;
import com.catticket.common.batch.BatchJobRunner;
import com.catticket.common.batch.BatchRunRecorder;
import com.catticket.common.batch.JobResult;
import com.catticket.common.batch.StepResult;
import com.catticket.common.batch.StepRun;
import com.catticket.common.batch.StepSummary;
import com.catticket.common.batch.TaskName;
import com.catticket.translation.TranslationStep;

/**
 * DailyCollectJob（B-01。05 の 3.2・3.5、06 の 5.3）。ステップ・記録・ジョブの共通の流れ・KopisCallPolicy はモック。
 */
class DailyCollectJobTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 6, 0, 0);

	private static final BatchContext CTX = new BatchContext(9L, NOW, LocalDate.of(2026, 11, 24));

	private static final String STOP = "S2: KOPIS blocked: KOPIS boxoffice blocked: HTTP 400 (Request Blocked)";

	private final BatchJobRunner runner = mock(BatchJobRunner.class);

	private final BatchRunRecorder recorder = mock(BatchRunRecorder.class);

	private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));

	private final PerformanceCollectStep s1 = mock(PerformanceCollectStep.class);

	private final RankingCollectStep s2 = mock(RankingCollectStep.class);

	private final AwardCollectStep s3 = mock(AwardCollectStep.class);

	private final MissingPerformanceStep s4 = mock(MissingPerformanceStep.class);

	private final GenreTrendStep s6 = mock(GenreTrendStep.class);

	private final TranslationStep s7 = mock(TranslationStep.class);

	private final KopisCallPolicy policy = mock(KopisCallPolicy.class);

	// 止めた印（ステップの中で立てる）
	private final AtomicReference<String> stopped = new AtomicReference<>();

	private final DailyCollectJob job = new DailyCollectJob(runner, recorder, clock, s1, s2, s3, s4, s6, s7, policy);

	private static StepResult ok() {
		return StepResult.success(1, new StepSummary());
	}

	@BeforeEach
	void setUp() {
		when(recorder.startStep(anyLong(), anyString(), any()))
			.thenAnswer(inv -> new StepRun(100L, inv.getArgument(0), inv.getArgument(1)));
		when(s1.run(CTX)).thenReturn(ok());
		when(s2.run(eq(CTX), anySet())).thenReturn(ok());
		when(s3.run(eq(CTX), anySet())).thenReturn(ok());
		when(s4.run(eq(CTX), anySet(), anySet())).thenReturn(ok());
		when(s6.run(CTX)).thenReturn(ok());
		when(s7.run(CTX)).thenReturn(ok());
		when(policy.allowance()).thenReturn(5);
		when(policy.stepCalls()).thenReturn(3);
		when(policy.runCalls()).thenReturn(12);
		when(policy.perRun()).thenReturn(20);
		when(policy.stopped()).thenAnswer(inv -> stopped.get() != null);
		when(policy.stopMessage()).thenAnswer(inv -> stopped.get());
	}

	/** finishStep に渡したステップの名前と結果（呼んだ順） */
	private List<String> finishedSteps(List<StepResult> results) {
		ArgumentCaptor<StepRun> runs = ArgumentCaptor.forClass(StepRun.class);
		ArgumentCaptor<StepResult> captured = ArgumentCaptor.forClass(StepResult.class);
		verify(recorder, times(6)).finishStep(runs.capture(), captured.capture(), any());
		results.addAll(captured.getAllValues());
		return runs.getAllValues().stream().map(StepRun::stepName).toList();
	}

	private StepResult resultOf(String step) {
		List<StepResult> results = new ArrayList<>();
		List<String> names = finishedSteps(results);
		return results.get(names.indexOf(step));
	}

	// S2 の実行中に止まった（S2 の後から stopped() が真）
	private void stopDuringS2() {
		when(s2.run(eq(CTX), anySet())).thenAnswer(inv -> {
			stopped.set(STOP);
			return StepResult.failure(10, new StepSummary(), "KopisStoppedException: KOPIS blocked: x");
		});
	}

	@Test
	void runsThroughTheRunnerAsDailyCollect() {
		assertThat(job.name()).isEqualTo(TaskName.DAILY_COLLECT);
		when(runner.execute(eq(TaskName.DAILY_COLLECT), any()))
			.thenAnswer(inv -> inv.<Function<BatchContext, JobResult>>getArgument(1).apply(CTX));

		assertThat(job.run()).isEqualTo(JobResult.SUCCESS);

		verify(runner).execute(eq(TaskName.DAILY_COLLECT), any());
		verify(s7).run(CTX);
	}

	@Test
	void startsTheJobOnceThenEachKopisStepBeforeItRuns() {
		job.runSteps(CTX);

		InOrder order = inOrder(policy, recorder, s1, s2, s3, s4, s6, s7);
		order.verify(policy).startJob();
		order.verify(recorder).startStep(9L, "S1", NOW);
		order.verify(policy).startStep("S1");
		order.verify(s1).run(CTX);
		order.verify(recorder).startStep(9L, "S2", NOW);
		order.verify(policy).startStep("S2");
		order.verify(s2).run(eq(CTX), anySet());
		order.verify(policy).startStep("S3");
		order.verify(s3).run(eq(CTX), anySet());
		order.verify(policy).startStep("S4");
		order.verify(s4).run(eq(CTX), anySet(), anySet());
		order.verify(policy).startStep("S6");
		order.verify(s6).run(CTX);
		order.verify(s7).run(CTX);
		verify(policy, times(1)).startJob();
		// S7 は KOPIS のステップではない
		verify(policy, never()).startStep("S7");
	}

	@Test
	void allStepsSucceededIsSuccessInOrderWithoutS5() {
		JobResult result = job.runSteps(CTX);

		assertThat(result).isEqualTo(JobResult.SUCCESS);
		InOrder order = inOrder(recorder, s1, s2, s3, s4, s6, s7);
		order.verify(recorder).startStep(9L, "S1", NOW);
		order.verify(s1).run(CTX);
		order.verify(recorder).finishStep(any(), any(), eq(NOW));
		order.verify(recorder).startStep(9L, "S2", NOW);
		order.verify(s2).run(eq(CTX), anySet());
		order.verify(recorder).startStep(9L, "S3", NOW);
		order.verify(s3).run(eq(CTX), anySet());
		order.verify(recorder).startStep(9L, "S4", NOW);
		order.verify(s4).run(eq(CTX), anySet(), anySet());
		order.verify(recorder).startStep(9L, "S6", NOW);
		order.verify(s6).run(CTX);
		order.verify(recorder).startStep(9L, "S7", NOW);
		order.verify(s7).run(CTX);
		order.verify(recorder).finishStep(any(), any(), eq(NOW));
		// S5 は欠番（05 の 3.6）
		verify(recorder, never()).startStep(anyLong(), eq("S5"), any());
		List<StepResult> results = new ArrayList<>();
		assertThat(finishedSteps(results)).containsExactly("S1", "S2", "S3", "S4", "S6", "S7");
		assertThat(results).allMatch(StepResult::success);
	}

	@Test
	void kopisStepsRecordTheirSendCount() {
		job.runSteps(CTX);

		List<StepResult> results = new ArrayList<>();
		List<String> names = finishedSteps(results);
		for (String step : List.of("S1", "S2", "S3", "S4", "S6")) {
			assertThat(results.get(names.indexOf(step)).summary().get("kopisCalls")).isEqualTo(3);
		}
		// S7 は KOPIS を呼ばない
		assertThat(results.get(names.indexOf("S7")).summary().counts()).doesNotContainKey("kopisCalls");
	}

	@Test
	void stepEndingWithAnExceptionStillRecordsTheSendCount() {
		when(s3.run(eq(CTX), anySet())).thenThrow(new IllegalStateException("db down"));

		job.runSteps(CTX);

		StepResult s3Result = resultOf("S3");
		assertThat(s3Result.success()).isFalse();
		assertThat(s3Result.errorMessage()).isEqualTo("IllegalStateException: db down");
		assertThat(s3Result.processedCount()).isNull();
		assertThat(s3Result.summary().counts()).containsExactly(Map.entry("kopisCalls", 3L));
	}

	@Test
	void stoppedDuringS2SkipsTheLaterKopisStepsButRunsS7() {
		stopDuringS2();

		JobResult result = job.runSteps(CTX);

		assertThat(result).isEqualTo(JobResult.PARTIAL_FAILURE);
		verify(s3, never()).run(any(), anySet());
		verify(s4, never()).run(any(), anySet(), anySet());
		verify(s6, never()).run(any());
		verify(policy, never()).startStep("S3");
		verify(s7).run(CTX);
		List<StepResult> results = new ArrayList<>();
		List<String> names = finishedSteps(results);
		assertThat(names).containsExactly("S1", "S2", "S3", "S4", "S6", "S7");
		for (String step : List.of("S3", "S4", "S6")) {
			StepResult skipped = results.get(names.indexOf(step));
			assertThat(skipped.success()).isFalse();
			assertThat(skipped.errorMessage()).isEqualTo("skipped: KOPIS calls stopped in this run at " + STOP);
			assertThat(skipped.processedCount()).isNull();
			assertThat(skipped.summary().counts()).containsExactly(Map.entry("kopisCalls", 0L));
		}
		assertThat(results.get(names.indexOf("S7")).success()).isTrue();
	}

	@Test
	void stoppedDuringS1SkipsS2ToS6ButRunsS7() {
		when(s1.run(CTX)).thenAnswer(inv -> {
			stopped.set("S1: KOPIS calls stopped after 5 consecutive failures; last: x");
			return StepResult.s1Failure(0, new StepSummary(), "KopisStoppedException: x", CTX.today());
		});

		JobResult result = job.runSteps(CTX);

		assertThat(result).isEqualTo(JobResult.PARTIAL_FAILURE);
		verify(s2, never()).run(any(), anySet());
		verify(s3, never()).run(any(), anySet());
		verify(s4, never()).run(any(), anySet(), anySet());
		verify(s6, never()).run(any());
		verify(s7).run(CTX);
		assertThat(resultOf("S2").errorMessage()).isEqualTo(
				"skipped: KOPIS calls stopped in this run at S1: KOPIS calls stopped after 5 consecutive failures; last: x");
	}

	@Test
	void stepWithoutAllowanceIsSkippedAndTheLaterStepsRun() {
		when(policy.allowance()).thenReturn(5, 5, 0, 5, 5);

		JobResult result = job.runSteps(CTX);

		assertThat(result).isEqualTo(JobResult.PARTIAL_FAILURE);
		verify(s3, never()).run(any(), anySet());
		verify(s4).run(eq(CTX), anySet(), anySet());
		verify(s6).run(CTX);
		StepResult s3Result = resultOf("S3");
		assertThat(s3Result.success()).isFalse();
		assertThat(s3Result.errorMessage()).isEqualTo("skipped: no KOPIS call allowance left (12/20)");
		assertThat(s3Result.processedCount()).isNull();
		assertThat(s3Result.summary().counts()).containsExactly(Map.entry("kopisCalls", 0L));
	}

	@Test
	void oneFailedStepIsPartialFailureAndTheRestStillRun() {
		StepResult failed = StepResult.failure(new StepSummary(), "1 failed; first: x");
		when(s2.run(eq(CTX), anySet())).thenReturn(failed);

		JobResult result = job.runSteps(CTX);

		assertThat(result).isEqualTo(JobResult.PARTIAL_FAILURE);
		verify(s3).run(eq(CTX), anySet());
		verify(s4).run(eq(CTX), anySet(), anySet());
		verify(s6).run(CTX);
		verify(s7).run(CTX);
		StepResult s2Result = resultOf("S2");
		assertThat(s2Result).isSameAs(failed);
		assertThat(s2Result.summary().get("kopisCalls")).isEqualTo(3);
	}

	@Test
	void otherExceptionsAreStepFailuresToo() {
		when(s7.run(CTX)).thenThrow(new IllegalStateException("boom"));
		when(s6.run(CTX)).thenReturn(null);

		JobResult result = job.runSteps(CTX);

		assertThat(result).isEqualTo(JobResult.PARTIAL_FAILURE);
		List<StepResult> results = new ArrayList<>();
		List<String> names = finishedSteps(results);
		assertThat(results.get(names.indexOf("S7")).errorMessage()).isEqualTo("IllegalStateException: boom");
		assertThat(results.get(names.indexOf("S6")).success()).isFalse();
	}

	@Test
	void s4ReceivesTheRankingAndAwardIdsSeparately() {
		when(s2.run(eq(CTX), anySet())).thenAnswer(inv -> {
			inv.<Set<String>>getArgument(1).addAll(List.of("PFR1", "PFBOTH"));
			return ok();
		});
		// S3 が上限で止まっても、それまでに足した ID は S4 に渡る（05 の 3.2）
		doAnswer(inv -> {
			inv.<Set<String>>getArgument(1).addAll(List.of("PFBOTH", "PFW1"));
			return StepResult.failure(1, new StepSummary(), "KopisCallLimitException: limit");
		}).when(s3).run(eq(CTX), anySet());
		List<Set<String>> received = new ArrayList<>();
		when(s4.run(eq(CTX), anySet(), anySet())).thenAnswer(inv -> {
			received.add(Set.copyOf(inv.<Set<String>>getArgument(1)));
			received.add(Set.copyOf(inv.<Set<String>>getArgument(2)));
			return ok();
		});

		assertThat(job.runSteps(CTX)).isEqualTo(JobResult.PARTIAL_FAILURE);

		assertThat(received).containsExactly(Set.of("PFR1", "PFBOTH"), Set.of("PFBOTH", "PFW1"));
	}

}
