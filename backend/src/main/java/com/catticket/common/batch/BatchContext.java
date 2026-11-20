package com.catticket.common.batch;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ジョブの実行中に渡す値（06 の 5.2）。now はジョブの開始時刻で、全ステップで同じ値を使う。
 */
public record BatchContext(long jobRunId, LocalDateTime now, LocalDate today) {
}
