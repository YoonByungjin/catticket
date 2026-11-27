package com.catticket.collect.row;

/**
 * 人気順位の1行（04 の ranking_entry）。
 */
public record RankingEntryInsert(int providerRank, String kopisPerformanceId) {
}
