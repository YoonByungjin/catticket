package com.catticket.common.batch;

/**
 * ジョブの中で最初に失敗したステップ（06 の 5.2 の「ジョブの error_message」）。
 */
public record FailedStep(String stepName, String errorMessage) {
}
