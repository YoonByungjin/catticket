package com.catticket.collect.client.xml;

import com.catticket.common.text.Texts;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 公演詳細の予約サイト（{@code relates/relate}）。値は strip し、空なら null */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KopisRelate(String relatenm, String relateurl) {

	public KopisRelate {
		relatenm = Texts.stripToNull(relatenm);
		relateurl = Texts.stripToNull(relateurl);
	}

}
