package com.catticket.common.display;

import java.time.LocalDate;

/**
 * 公演のカード（03 の 7.2）。値は管理者の修正を反映したもの。rating は API-20・21 では null。
 */
public record PerformanceSummary(String performanceId, LocalizedText title, VenueRef venue, LocalDate startDate,
		LocalDate endDate, String genre, PerformanceStatus status, String posterUrl, boolean hasAward,
		Rating rating) {
}
