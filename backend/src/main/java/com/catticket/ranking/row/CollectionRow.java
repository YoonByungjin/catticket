package com.catticket.ranking.row;

import java.time.LocalDate;

import com.catticket.common.display.Period;

/**
 * 順位の収集が終わった組の1行（04 の ranking_collection）。
 */
public record CollectionRow(long id, LocalDate periodStart, LocalDate periodEnd) {

	public Period period() {
		return new Period(periodStart, periodEnd);
	}

}
