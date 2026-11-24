package com.catticket.collect.row;

import java.math.BigDecimal;

import com.catticket.common.code.Genre;

/**
 * ジャンル別トレンドの1行（GenreTrendCalculator の結果。04 の genre_trend）。share は % で小数第6位まで。
 */
public record GenreTrendRow(Genre genre, long bookingCount, BigDecimal share) {
}
