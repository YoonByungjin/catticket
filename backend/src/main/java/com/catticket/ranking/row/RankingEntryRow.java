package com.catticket.ranking.row;

import java.time.LocalDate;

import com.catticket.common.display.PerformanceSummaryRow;

/**
 * 表示できる公演に結びついた順位の行（findTopVisible の結果）。
 * providerRank の後は SharedSql の summaryColumns と同じ列（公演のカードを組み立てるため）。
 */
public record RankingEntryRow(int providerRank, long id, String performanceId, String title, LocalDate startDate,
		LocalDate endDate, String genreCode, String venueId, String venueName, String posterUrl, boolean hasAward) {

	public PerformanceSummaryRow toSummaryRow() {
		return new PerformanceSummaryRow(id, performanceId, title, startDate, endDate, genreCode, venueId, venueName,
				posterUrl, hasAward);
	}

}
