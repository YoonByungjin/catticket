package com.catticket.ranking.dto;

import java.util.List;

import com.catticket.common.code.PeriodType;
import com.catticket.common.display.Period;

/**
 * API-20 の応答（03 の API-20）。表示できる期間がないとき period は null、items は []（03 の 4.9）。
 */
public record RankingResponse(PeriodType periodType, String genre, Period period, List<RankingItem> items) {
}
