package com.catticket.common.display;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 評価（03 の 4.4・7.2）。平均は SQL の AVG を使わず、合計と件数から BigDecimal で求める（境目の値で P-03 とずれないように）。
 */
public record Rating(BigDecimal average, long count) {

	public static Rating of(long sum, long count) {
		if (count == 0) {
			return new Rating(null, 0);
		}
		BigDecimal average = BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
		return new Rating(average, count);
	}

}
