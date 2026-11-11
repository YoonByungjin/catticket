package com.catticket.common.display;

import java.time.LocalDate;

/**
 * 公演の状態（03 の 1.6・7.2）。保存せず、照会のたびに今日（Asia/Seoul）と期間で決める。
 */
public enum PerformanceStatus {

	ONGOING, UPCOMING, ENDED;

	public static PerformanceStatus of(LocalDate start, LocalDate end, LocalDate today) {
		if (end.isBefore(today)) {
			return ENDED;
		}
		if (start.isAfter(today)) {
			return UPCOMING;
		}
		return ONGOING;
	}

}
