package com.catticket.review;

/**
 * 公演の星の合計と件数（06 の 4.3）。平均は Rating.of で求める（SQL の AVG は使わない。03 の 7.2）。
 */
public record RatingStat(long sum, long count) {
}
