package com.catticket.common.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;

/**
 * BatchRunRecorder（BatchRunMapper はモック）。
 */
class BatchRunRecorderTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 19, 6, 0, 0);

	private static final String KOPIS_KEY = "kopis-secret-0123456789";

	private BatchRunMapper mapper;

	private BatchRunRecorder recorder;

	@BeforeEach
	void setUp() {
		mapper = mock(BatchRunMapper.class);
		MockEnvironment env = new MockEnvironment().withProperty("catticket.kopis.api-key", KOPIS_KEY)
			.withProperty("catticket.azure.translator-key", "")
			.withProperty("spring.datasource.password", "db-password-xyz");
		recorder = new BatchRunRecorder(mapper, JsonMapper.builder().build(), env);
	}

	@Test
	void startReturnsGeneratedId() {
		doAnswer(inv -> {
			inv.<JobRunInsert>getArgument(0).setId(11L);
			return 1;
		}).when(mapper).insertJobRun(any());

		assertThat(recorder.start(TaskName.IMPORT_SUBWAY, NOW)).isEqualTo(11L);
	}

	@Test
	void startStepReturnsStepRun() {
		doAnswer(inv -> {
			inv.<StepRunInsert>getArgument(0).setId(21L);
			return 1;
		}).when(mapper).insertStepRun(any());

		assertThat(recorder.startStep(11L, "import-subway", NOW)).isEqualTo(new StepRun(21L, 11L, "import-subway"));
	}

	@Test
	void finishStepWritesSummaryAsJson() {
		StepSummary summary = new StepSummary();
		summary.add("stationsSaved", 261);
		summary.add("lockerUnmatched", 1);
		summary.addDetail("lockerUnmatched", "7|이수|7호선 대합실");

		recorder.finishStep(new StepRun(21L, 11L, "import-subway"), StepResult.success(574, summary), NOW);

		verify(mapper).updateStepRun(21L, NOW, "SUCCESS", 574L,
				"{\"stationsSaved\":261,\"lockerUnmatched\":1,\"details\":{\"lockerUnmatched\":[\"7|이수|7호선 대합실\"]}}",
				null);
	}

	@Test
	void finishStepFailureHasNoSecretsAndIsOneLine() {
		recorder.finishStep(new StepRun(21L, 11L, "S1"),
				StepResult.failure(null, "HttpClientErrorException: GET http://x?service=" + KOPIS_KEY + "\nline2"),
				NOW);

		verify(mapper).updateStepRun(21L, NOW, "FAILURE", 0L, null,
				"HttpClientErrorException: GET http://x?service=*** line2");
	}

	@Test
	void finishWritesResultAndSanitizedMessage() {
		recorder.finish(11L, JobResult.FAILURE, "SQLException: Access denied (db-password-xyz)", NOW);
		verify(mapper).updateJobRun(11L, NOW, "FAILURE", "SQLException: Access denied (***)");
	}

	@Test
	void partialFailureWithoutMessageWritesFirstFailedStep() {
		when(mapper.findFirstFailedStep(11L)).thenReturn(Optional.of(new FailedStep("S3", "X: y")));

		recorder.finish(11L, JobResult.PARTIAL_FAILURE, null, NOW);

		verify(mapper).updateJobRun(11L, NOW, "PARTIAL_FAILURE", "S3: X: y");
	}

	@Test
	void failureWithoutMessageWritesFirstFailedStep() {
		when(mapper.findFirstFailedStep(11L)).thenReturn(Optional.of(new FailedStep("import-subway",
				"SubwayFileException: locker 12行目 보관함명: 空")));

		recorder.finish(11L, JobResult.FAILURE, null, NOW);

		verify(mapper).updateJobRun(11L, NOW, "FAILURE", "import-subway: SubwayFileException: locker 12行目 보관함명: 空");
	}

	@Test
	void bodyExceptionMessageIsPreferredOverFailedStep() {
		recorder.finish(11L, JobResult.FAILURE, "IllegalStateException: broken", NOW);

		verify(mapper).updateJobRun(11L, NOW, "FAILURE", "IllegalStateException: broken");
		verify(mapper, never()).findFirstFailedStep(anyLong());
	}

	@Test
	void successKeepsNullAndDoesNotLookUpSteps() {
		recorder.finish(11L, JobResult.SUCCESS, null, NOW);

		verify(mapper).updateJobRun(11L, NOW, "SUCCESS", null);
		verify(mapper, never()).findFirstFailedStep(anyLong());
	}

	@Test
	void noFailedStepWritesNull() {
		when(mapper.findFirstFailedStep(11L)).thenReturn(Optional.empty());

		recorder.finish(11L, JobResult.PARTIAL_FAILURE, null, NOW);

		verify(mapper).updateJobRun(11L, NOW, "PARTIAL_FAILURE", null);
	}

	@Test
	void failedStepWithoutMessageWritesStepNameOnly() {
		when(mapper.findFirstFailedStep(11L)).thenReturn(Optional.of(new FailedStep("S7", null)));

		recorder.finish(11L, JobResult.PARTIAL_FAILURE, null, NOW);

		verify(mapper).updateJobRun(11L, NOW, "PARTIAL_FAILURE", "S7");
	}

	@Test
	void failedStepMessageIsSanitizedAgainAfterAddingStepName() {
		// ステップ側で 2000 に切り詰め済みでも、名前を付けると超えるので再び切り詰める
		String stepMessage = "x".repeat(BatchRunRecorder.MAX_ERROR_MESSAGE);
		when(mapper.findFirstFailedStep(11L)).thenReturn(Optional.of(new FailedStep("S1", stepMessage)));

		recorder.finish(11L, JobResult.PARTIAL_FAILURE, null, NOW);

		String expected = ("S1: " + stepMessage).substring(0, BatchRunRecorder.MAX_ERROR_MESSAGE);
		verify(mapper).updateJobRun(11L, NOW, "PARTIAL_FAILURE", expected);
	}

	@Test
	void lockedIsNotRecorded() {
		assertThatThrownBy(() -> recorder.finish(11L, JobResult.LOCKED, null, NOW))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void longMessageIsTruncated() {
		String message = recorder.sanitize("x".repeat(5000));
		assertThat(message).hasSize(BatchRunRecorder.MAX_ERROR_MESSAGE);
	}

	@Test
	void describeIsClassNameAndMessage() {
		assertThat(BatchRunRecorder.describe(new IllegalStateException("boom"))).isEqualTo("IllegalStateException: boom");
		assertThat(BatchRunRecorder.describe(new NullPointerException())).isEqualTo("NullPointerException");
	}

	@Test
	void everyMethodRequiresNewTransaction() throws NoSuchMethodException {
		// ステップの失敗のロールバックで記録が消えないように（06 の 5.2）
		for (Method m : new Method[] {
				BatchRunRecorder.class.getMethod("start", TaskName.class, LocalDateTime.class),
				BatchRunRecorder.class.getMethod("startStep", long.class, String.class, LocalDateTime.class),
				BatchRunRecorder.class.getMethod("finishStep", StepRun.class, StepResult.class, LocalDateTime.class),
				BatchRunRecorder.class.getMethod("finish", long.class, JobResult.class, String.class,
						LocalDateTime.class) }) {
			Transactional tx = m.getAnnotation(Transactional.class);
			assertThat(tx).as(m.getName()).isNotNull();
			assertThat(tx.propagation()).as(m.getName()).isEqualTo(Propagation.REQUIRES_NEW);
		}
	}

}
