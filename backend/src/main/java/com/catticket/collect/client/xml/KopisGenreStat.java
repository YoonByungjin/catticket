package com.catticket.collect.client.xml;

import com.catticket.common.text.Texts;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * ジャンル別統計の1行。予約数は販売枚数 {@code ntssnmrssm} を使う（05 の 3.2 の「S6 の詳細」）。
 * {@code 합계} の行も含めて返す（除くのは GenreTrendCalculator）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KopisGenreStat(String catenm, String ntssnmrssm) {

	public KopisGenreStat {
		catenm = Texts.stripToNull(catenm);
		ntssnmrssm = Texts.stripToNull(ntssnmrssm);
	}

}
