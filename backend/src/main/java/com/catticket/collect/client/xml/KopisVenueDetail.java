package com.catticket.collect.client.xml;

import com.catticket.common.text.Texts;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 施設詳細（{@code prfplc/{mt10id}}）の使う項目（06 の 7.1）。座標も文字列で受ける。値は strip し、空なら null。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KopisVenueDetail(String mt10id, String fcltynm, String adres, String la, String lo) {

	public KopisVenueDetail {
		mt10id = Texts.stripToNull(mt10id);
		fcltynm = Texts.stripToNull(fcltynm);
		adres = Texts.stripToNull(adres);
		la = Texts.stripToNull(la);
		lo = Texts.stripToNull(lo);
	}

}
