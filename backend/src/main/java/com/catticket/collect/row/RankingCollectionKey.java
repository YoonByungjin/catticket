package com.catticket.collect.row;

import com.catticket.common.code.PeriodType;
import com.catticket.common.display.Period;

/**
 * 人気順位の組（期間の種類・ジャンル・期間。04 の ranking_collection）。genreCode は全体なら {@link #ALL}。
 */
public record RankingCollectionKey(PeriodType periodType, String genreCode, Period period) {

	/** 全体（catecode を指定しない照会）のジャンルのコード */
	public static final String ALL = "ALL";

	@Override
	public String toString() {
		return periodType + "|" + period.start() + "|" + genreCode;
	}

}
