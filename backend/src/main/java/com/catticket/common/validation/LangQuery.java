package com.catticket.common.validation;

import com.catticket.common.code.Lang;

/**
 * 表示言語のクエリ（03 の 7.1）。省略は ko。
 * record の部品 {@code lang} の accessor は String を返すので、{@link Lang} は {@link #toLang()} で返す。
 */
public record LangQuery(@OneOf({ "ko", "ja" }) String lang) {

	public Lang toLang() {
		return "ja".equals(lang) ? Lang.JA : Lang.KO;
	}

}
