package com.catticket.collect.client.xml;

import com.catticket.common.text.Texts;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 目録の1行。使うのは公演 ID だけ（06 の 7.1 の「使う項目」）。値は strip し、空なら null。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KopisPerformanceListItem(String mt20id) {

	public KopisPerformanceListItem {
		mt20id = Texts.stripToNull(mt20id);
	}

}
