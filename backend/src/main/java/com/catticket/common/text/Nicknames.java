package com.catticket.common.text;

import java.text.Normalizer;

/**
 * ニックネームの正規化（03 の 7.1）。NFKC を先にし、NFKC が普通の空白に変える文字（U+00A0 など）も前後から除けるようにする。
 */
public final class Nicknames {

	private Nicknames() {
	}

	public static String normalize(String value) {
		if (value == null) {
			return null;
		}
		return Texts.stripToNull(Normalizer.normalize(value, Normalizer.Form.NFKC));
	}

}
