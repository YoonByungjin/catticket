package com.catticket.common.batch;

/**
 * ステップの結果（06 の 5.2）。errorMessage は失敗のときだけ（1行。スタックトレースを入れない）。
 */
public record StepResult(boolean success, long processedCount, StepSummary summary, String errorMessage) {

	public static StepResult success(long processedCount, StepSummary summary) {
		return new StepResult(true, processedCount, summary, null);
	}

	public static StepResult failure(StepSummary summary, String errorMessage) {
		return new StepResult(false, 0, summary, errorMessage);
	}

}
