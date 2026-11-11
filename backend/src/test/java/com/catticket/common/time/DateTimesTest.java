package com.catticket.common.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class DateTimesTest {

	// UTC では 11/9 の 15:30 だが、ソウルでは 11/10 の 00:30
	private final Clock clock = Clock.fixed(Instant.parse("2026-11-09T15:30:45.987Z"), ZoneOffset.UTC);

	@Test
	void todayAndNowAreSeoulWallClock() {
		assertThat(DateTimes.today(clock)).isEqualTo(LocalDate.of(2026, 11, 10));
		assertThat(DateTimes.now(clock)).isEqualTo(LocalDateTime.of(2026, 11, 10, 0, 30, 45));
	}

	@Test
	void toApiAddsSeoulOffsetAndTruncatesToSeconds() {
		OffsetDateTime api = DateTimes.toApi(LocalDateTime.of(2026, 11, 5, 14, 3, 7, 500_000_000));
		assertThat(api).isEqualTo(OffsetDateTime.of(2026, 11, 5, 14, 3, 7, 0, ZoneOffset.ofHours(9)));
		assertThat(DateTimes.toApi(null)).isNull();
	}

}
