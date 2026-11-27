package com.catticket.ranking.dto;

import com.catticket.common.display.PerformanceSummary;

/**
 * API-20 の items の1つ。rank は表示できない公演を除いて振り直した順位（1〜10。03 の 9.1）。
 */
public record RankingItem(int rank, PerformanceSummary performance) {
}
