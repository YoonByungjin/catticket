package com.catticket.common.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.catticket.common.time.DateTimes;

/**
 * BatchJobRunner（JobLock・BatchRunRecorder はモック）。
 */
class BatchJobRunnerTest {

	// 2026-11-19 06:00:00 Asia/Seoul
	private final Clock clock = Clock.fixed(Instant.parse("2026-11-18T21:00:00Z"), DateTimes.SEOUL);

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 19, 6, 0, 0);

	private JobLock jobLock;

	private JobLock.Held held;

	private BatchRunRecorder recorder;

	private BatchJobRunner runner;

	@BeforeEach
	void setUp() {
		jobLock = mock(JobLock.class);
		held = mock(JobLock.Held.class);
		recorder = mock(BatchRunRecorder.class);
		runner = new BatchJobRunner(jobLock, recorder, clock);
	}

	@Test
	void lockedDoesNotRunOrRecord() {
		when(jobLock.tryAcquire(TaskName.IMPORT_SUBWAY)).thenReturn(Optional.empty());
		AtomicReference<BatchContext> called = new AtomicReference<>();

		JobResult result = runner.execute(TaskName.IMPORT_SUBWAY, ctx -> {
			called.set(ctx);
			return JobResult.SUCCESS;
		});

		assertThat(result).isEqualTo(JobResult.LOCKED);
		assertThat(called.get()).isNull();
		verifyNoInteractions(recorder);
	}

	@Test
	void successRecordsStartAndFinishAndReleasesLock() {
		when(jobLock.tryAcquire(TaskName.IMPORT_SUBWAY)).thenReturn(Optional.of(held));
		when(recorder.start(TaskName.IMPORT_SUBWAY, NOW)).thenReturn(42L);
		AtomicReference<BatchContext> called = new AtomicReference<>();

		JobResult result = runner.execute(TaskName.IMPORT_SUBWAY, ctx -> {
			called.set(ctx);
			return JobResult.SUCCESS;
		});

		assertThat(result).isEqualTo(JobResult.SUCCESS);
		assertThat(called.get()).isEqualTo(new BatchContext(42L, NOW, LocalDate.of(2026, 11, 19)));
		InOrder order = inOrder(jobLock, recorder, held);
		order.verify(jobLock).tryAcquire(TaskName.IMPORT_SUBWAY);
		order.verify(recorder).start(TaskName.IMPORT_SUBWAY, NOW);
		order.verify(recorder).finish(42L, JobResult.SUCCESS, null, NOW);
		order.verify(held).close();
	}

	@Test
	void partialFailureIsPassedThrough() {
		when(jobLock.tryAcquire(TaskName.DAILY_COLLECT)).thenReturn(Optional.of(held));
		when(recorder.start(any(), any())).thenReturn(1L);

		assertThat(runner.execute(TaskName.DAILY_COLLECT, ctx -> JobResult.PARTIAL_FAILURE))
			.isEqualTo(JobResult.PARTIAL_FAILURE);
		verify(recorder).finish(1L, JobResult.PARTIAL_FAILURE, null, NOW);
		verify(held).close();
	}

	@Test
	void exceptionBecomesFailureWithOneLineMessageAndReleasesLock() {
		when(jobLock.tryAcquire(TaskName.IMPORT_SUBWAY)).thenReturn(Optional.of(held));
		when(recorder.start(any(), any())).thenReturn(7L);

		JobResult result = runner.execute(TaskName.IMPORT_SUBWAY, ctx -> {
			throw new IllegalStateException("broken file");
		});

		assertThat(result).isEqualTo(JobResult.FAILURE);
		ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
		verify(recorder).finish(eq(7L), eq(JobResult.FAILURE), message.capture(), eq(NOW));
		// クラス名とメッセージだけ（スタックトレースを入れない）
		assertThat(message.getValue()).isEqualTo("IllegalStateException: broken file");
		verify(held).close();
	}

	@Test
	void lockIsReleasedEvenWhenRecordingFails() {
		when(jobLock.tryAcquire(TaskName.IMPORT_SUBWAY)).thenReturn(Optional.of(held));
		when(recorder.start(any(), any())).thenThrow(new IllegalStateException("db down"));

		try {
			runner.execute(TaskName.IMPORT_SUBWAY, ctx -> JobResult.SUCCESS);
		}
		catch (IllegalStateException expected) {
			// TaskRunner が終了コード 1 にする
		}
		verify(held).close();
		verify(recorder, never()).finish(anyLong(), any(), any(), any());
	}

}
