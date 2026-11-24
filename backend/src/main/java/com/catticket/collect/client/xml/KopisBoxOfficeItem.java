package com.catticket.collect.client.xml;

import com.catticket.common.text.Texts;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 順位の1行。使うのは順位と公演 ID だけ（ジャンルは照会の catecode で決まる。06 の 7.1）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KopisBoxOfficeItem(String rnum, String mt20id) {

	public KopisBoxOfficeItem {
		rnum = Texts.stripToNull(rnum);
		mt20id = Texts.stripToNull(mt20id);
	}

}
