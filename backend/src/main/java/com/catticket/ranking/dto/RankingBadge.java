package com.catticket.ranking.dto;

import com.catticket.common.code.PeriodType;
import com.catticket.common.display.Period;

/**
 * API-03 の rankings の1つ（03 の 4.3）。rank は振り直した順位（1〜10。03 の 9.1）、genre は ALL かジャンルのコード。
 */
public record RankingBadge(PeriodType periodType, String genre, int rank, Period period) {
}
