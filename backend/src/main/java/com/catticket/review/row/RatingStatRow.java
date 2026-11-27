package com.catticket.review.row;

/**
 * 公演ごとの星の合計と件数（sumAndCount の結果）。performanceId は内部の ID（performance.id）。
 */
public record RatingStatRow(long performanceId, long sum, long count) {
}
