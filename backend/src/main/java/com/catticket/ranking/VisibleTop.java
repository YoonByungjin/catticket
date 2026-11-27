package com.catticket.ranking;

import java.util.List;

import com.catticket.common.display.Period;
import com.catticket.ranking.row.RankingEntryRow;

/**
 * 表示する期間（03 の 4.9）と、表示できない公演を除いて 1 から振り直した上位（03 の 9.1）。
 * API-20・API-03 が同じ順位を使うためにまとめる。
 */
record VisibleTop(Period period, List<Ranked> entries) {

	record Ranked(int rank, RankingEntryRow row) {
	}

}
