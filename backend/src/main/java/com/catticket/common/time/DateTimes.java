package com.catticket.common.time;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * 時刻の部品。業務の時刻はすべて Asia/Seoul の壁時計の時刻（03 の 1.1・7.1）。
 */
public final class DateTimes {

	public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private DateTimes() {
	}

	/** API に出す日時（+09:00、秒まで） */
	public static OffsetDateTime toApi(LocalDateTime value) {
		if (value == null) {
			return null;
		}
		return value.truncatedTo(ChronoUnit.SECONDS).atZone(SEOUL).toOffsetDateTime();
	}

	public static LocalDate today(Clock clock) {
		return LocalDate.ofInstant(clock.instant(), SEOUL);
	}

	/** DB の DATETIME は秒までなので、秒で切り捨てる */
	public static LocalDateTime now(Clock clock) {
		return LocalDateTime.ofInstant(clock.instant(), SEOUL).truncatedTo(ChronoUnit.SECONDS);
	}

}
