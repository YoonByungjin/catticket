package com.catticket.common.display;

import java.util.Map;

import com.catticket.common.code.Lang;

/**
 * {@link LocalizedText} を作る（03 の 7.2）。翻訳は原文をキーにした Map で受け取り、1行ずつ DB を引かない。
 */
public final class LocalizedTextFactory {

	private LocalizedTextFactory() {
	}

	/** 値そのものがない項目は LocalizedText ではなく null にする（03 の 7.2） */
	public static LocalizedText of(String original, Lang lang, Map<String, String> translations) {
		if (original == null) {
			return null;
		}
		if (lang == Lang.JA) {
			String translated = translations == null ? null : translations.get(original);
			if (translated != null) {
				return new LocalizedText(translated, original, true);
			}
		}
		return new LocalizedText(original, original, false);
	}

	/** 駅名は公式の日本語表記（name_ja）→ 翻訳 → 原文の順（03 の 7.2） */
	public static LocalizedText ofStation(String nameKo, String nameJa, Lang lang, Map<String, String> translations) {
		if (nameKo == null) {
			return null;
		}
		if (lang == Lang.JA && nameJa != null && !nameJa.isBlank()) {
			return new LocalizedText(nameJa, nameKo, true);
		}
		return of(nameKo, lang, translations);
	}

}
