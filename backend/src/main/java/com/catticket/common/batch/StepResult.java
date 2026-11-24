package com.catticket.common.batch;

import java.time.LocalDate;

/**
 * ステップの結果（06 の 5.2）。errorMessage は失敗のときだけ（1行。スタックトレースを入れない）。
 * windowEndDate は成功した S1 の「今回の窓の末日」だけ（04 の DB-10、06 の 11 の U-11）。ほかは null。
 */
public record StepResult(boolean success, long processedCount, StepSummary summary, String errorMessage,
		LocalDate windowEndDate) {

	public StepResult {
		if (windowEndDate != null && !success) {
			// 窓の末日は成功した S1 だけが持つ（04 の chk_batch_step_run_window）
			throw new IllegalArgumentException("windowEndDate is only for a successful step");
		}
	}

	public static StepResult success(long processedCount, StepSummary summary) {
		return new StepResult(true, processedCount, summary, null, null);
	}

	/** 成功した S1（窓の末日を記録する） */
	public static StepResult success(long processedCount, StepSummary summary, LocalDate windowEndDate) {
		return new StepResult(true, processedCount, summary, null, windowEndDate);
	}

	public static StepResult failure(StepSummary summary, String errorMessage) {
		return new StepResult(false, 0, summary, errorMessage, null);
	}

	/** 失敗したが、途中まで処理した件数を残す */
	public static StepResult failure(long processedCount, StepSummary summary, String errorMessage) {
		return new StepResult(false, processedCount, summary, errorMessage, null);
	}

}
