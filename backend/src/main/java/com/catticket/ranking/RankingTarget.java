package com.catticket.ranking;

import com.catticket.common.code.PeriodType;
import com.catticket.common.display.Period;

/**
 * S2 が照会する期間（06 の 4.9 の「RankingCollectStep の期間」）。
 */
public record RankingTarget(PeriodType type, Period period) {
}
