package com.catticket.common.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * StepResult（06 の 5.2）。窓の末日と基準日の組み合わせを確かめる。
 */
class StepResultTest {

	private static final LocalDate END = LocalDate.of(2027, 5, 23);

	private static final LocalDate BASE = LocalDate.of(2026, 11, 22);

	@Test
	void windowEndDateNeedsCollectBaseDate() {
		assertThatThrownBy(() -> new StepResult(true, 1L, null, null, END, null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void failedStepCannotHaveWindowEndDate() {
		assertThatThrownBy(() -> new StepResult(false, 0L, null, "x", END, BASE))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void s1ResultsCarryTheBaseDate() {
		StepResult success = StepResult.s1Success(3, new StepSummary(), END, BASE);
		StepResult failure = StepResult.s1Failure(2, new StepSummary(), "x", BASE);

		assertThat(success.success()).isTrue();
		assertThat(success.windowEndDate()).isEqualTo(END);
		assertThat(success.collectBaseDate()).isEqualTo(BASE);
		assertThat(failure.success()).isFalse();
		assertThat(failure.processedCount()).isEqualTo(2L);
		assertThat(failure.windowEndDate()).isNull();
		assertThat(failure.collectBaseDate()).isEqualTo(BASE);
	}

	@Test
	void otherStepsHaveNoDates() {
		assertThat(StepResult.success(1, new StepSummary()).collectBaseDate()).isNull();
		assertThat(StepResult.failure(1, new StepSummary(), "x").windowEndDate()).isNull();
		// 件数のない失敗は null（記録は NULL）
		assertThat(StepResult.failure(new StepSummary(), "x").processedCount()).isNull();
	}

}
