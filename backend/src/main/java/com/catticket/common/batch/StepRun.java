package com.catticket.common.batch;

/**
 * 記録を始めたステップ（BatchRunRecorder.startStep の戻り値）。
 */
public record StepRun(long id, long jobRunId, String stepName) {
}
