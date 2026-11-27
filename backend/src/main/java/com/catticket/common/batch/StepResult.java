package com.catticket.common.batch;

import java.time.LocalDate;

/**
 * ステップの結果（06 の 5.2）。errorMessage は失敗のときだけ（1行。スタックトレースを入れない）。
 * processedCount は処理した件数（呼ばずに飛ばしたステップなどは null で、記録は NULL。06 の 5.3）。
 * windowEndDate は成功した S1 の「今回の窓の末日」だけ（04 の DB-10、06 の 11 の U-11）。ほかは null。
 * collectBaseDate は S1 の収集の基準日（成功・失敗とも S1 だけ。05 の 3.3 の 12.）。ほかは null。
 */
public record StepResult(boolean success, Long processedCount, StepSummary summary, String errorMessage,
		LocalDate windowEndDate, LocalDate collectBaseDate) {

	public StepResult {
		if (windowEndDate != null && !success) {
			// 窓の末日は成功した S1 だけが持つ（04 の chk_batch_step_run_window）
			throw new IllegalArgumentException("windowEndDate is only for a successful step");
		}
		if (windowEndDate != null && collectBaseDate == null) {
			// 成功した S1 の基準日が次の実行の afterdate になるので、末日だけを記録しない
			throw new IllegalArgumentException("windowEndDate needs collectBaseDate");
		}
	}

	public static StepResult success(long processedCount, StepSummary summary) {
		return new StepResult(true, processedCount, summary, null, null, null);
	}

	/** 成功した S1（窓の末日と基準日を記録する） */
	public static StepResult s1Success(long processedCount, StepSummary summary, LocalDate windowEndDate,
			LocalDate collectBaseDate) {
		return new StepResult(true, processedCount, summary, null, windowEndDate, collectBaseDate);
	}

	/** 失敗した S1（基準日を記録し、次の実行が引き継ぐ。窓の末日は記録しない） */
	public static StepResult s1Failure(long processedCount, StepSummary summary, String errorMessage,
			LocalDate collectBaseDate) {
		return new StepResult(false, processedCount, summary, errorMessage, null, collectBaseDate);
	}

	/** 処理した件数のない失敗（processed_count は NULL） */
	public static StepResult failure(StepSummary summary, String errorMessage) {
		return new StepResult(false, null, summary, errorMessage, null, null);
	}

	/** 失敗したが、途中まで処理した件数を残す */
	public static StepResult failure(long processedCount, StepSummary summary, String errorMessage) {
		return new StepResult(false, processedCount, summary, errorMessage, null, null);
	}

}
