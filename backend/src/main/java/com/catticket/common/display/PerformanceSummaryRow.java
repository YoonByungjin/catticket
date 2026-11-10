package com.catticket.common.display;

import java.time.LocalDate;

/**
 * 公演のカードの SQL の結果（mapper/common/SharedSql.xml の summaryColumns と同じ順）。
 * id は内部の ID（performance.id）で API に出さない。performanceId・venueId は KOPIS の ID。
 */
public record PerformanceSummaryRow(long id, String performanceId, String title, LocalDate startDate,
		LocalDate endDate, String genreCode, String venueId, String venueName, String posterUrl, boolean hasAward) {
}
