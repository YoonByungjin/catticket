package com.catticket.common.text;

import java.nio.charset.StandardCharsets;

/**
 * 文字列の部品。文字数はコードポイントで数える（03 の 7.1）。
 */
public final class Texts {

	private Texts() {
	}

	/** 前後の Unicode の空白を除き、空なら null */
	public static String stripToNull(String value) {
		if (value == null) {
			return null;
		}
		String stripped = value.strip();
		return stripped.isEmpty() ? null : stripped;
	}

	public static int codePointLength(String value) {
		return value == null ? 0 : value.codePointCount(0, value.length());
	}

	/** 一般カテゴリ Cc を含むか。allowNewlines なら LF・CR だけ許す */
	public static boolean hasControlChars(String value, boolean allowNewlines) {
		if (value == null) {
			return false;
		}
		return value.codePoints()
			.anyMatch(cp -> Character.getType(cp) == Character.CONTROL
					&& !(allowNewlines && (cp == '\n' || cp == '\r')));
	}

	/** 先頭から maxCodePoints 個のコードポイントにする（サロゲートペアを割らない） */
	public static String truncateCodePoints(String value, int maxCodePoints) {
		if (value == null || codePointLength(value) <= maxCodePoints) {
			return value;
		}
		return value.substring(0, value.offsetByCodePoints(0, maxCodePoints));
	}

	/** UTF-8 で maxBytes バイト以内にする（TEXT の列は 65,535 バイト）。文字の途中で切らない */
	public static String truncateUtf8Bytes(String value, int maxBytes) {
		if (value == null || value.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
			return value;
		}
		StringBuilder sb = new StringBuilder();
		int bytes = 0;
		for (int i = 0; i < value.length();) {
			int cp = value.codePointAt(i);
			int size = utf8Length(cp);
			if (bytes + size > maxBytes) {
				break;
			}
			sb.appendCodePoint(cp);
			bytes += size;
			i += Character.charCount(cp);
		}
		return sb.toString();
	}

	private static int utf8Length(int cp) {
		if (cp < 0x80) {
			return 1;
		}
		if (cp < 0x800) {
			return 2;
		}
		if (cp < 0x10000) {
			return 3;
		}
		return 4;
	}

}
