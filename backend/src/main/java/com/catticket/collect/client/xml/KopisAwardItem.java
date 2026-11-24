package com.catticket.collect.client.xml;

import com.catticket.common.text.Texts;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 受賞作の1行。受賞の有無だけを使うので公演 ID だけ（06 の 7.1） */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KopisAwardItem(String mt20id) {

	public KopisAwardItem {
		mt20id = Texts.stripToNull(mt20id);
	}

}
