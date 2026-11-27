package com.catticket.collect;

import java.util.Optional;

import com.catticket.common.code.SeoulDistrict;
import com.catticket.common.text.Texts;

/**
 * 会場の住所から区を決める（03 の 4.2）。Spring に依存しない。
 * 住所の先頭「서울특별시」に続く語が 25 区の名前と一致するときだけ決める。
 */
public final class DistrictParser {

	static final String SEOUL_PREFIX = "서울특별시";

	private DistrictParser() {
	}

	/** 決められなければ空（住所がない・ソウルでない・形が違う） */
	public static Optional<SeoulDistrict> parse(String address) {
		String value = Texts.stripToNull(address);
		if (value == null || !value.startsWith(SEOUL_PREFIX)) {
			return Optional.empty();
		}
		String rest = value.substring(SEOUL_PREFIX.length()).strip();
		if (rest.isEmpty()) {
			return Optional.empty();
		}
		// 区の名前は空白で区切られた最初の語。前方一致にしない（「중구」と「중랑구」などを取り違えないため）
		String first = rest.split("\\s+", 2)[0];
		return SeoulDistrict.fromKoreanName(first);
	}

}
