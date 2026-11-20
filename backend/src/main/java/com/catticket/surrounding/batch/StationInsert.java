package com.catticket.surrounding.batch;

import java.math.BigDecimal;

/**
 * 入れる駅（04 の station）。nameJa は公式の日本語表記がなければ null（B-01 の S7 で翻訳する）。
 */
public record StationInsert(String externalStationCode, int line, String nameKo, String nameJa,
		BigDecimal latitude, BigDecimal longitude) {
}
