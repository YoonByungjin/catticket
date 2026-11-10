package com.catticket.common.code;

import java.util.Arrays;
import java.util.Optional;

/**
 * KOPIS のジャンル。定数名を API・DB のコード（{@code genre_code}）と同じにする。
 * 値と KOPIS の名前（{@code genrenm}）は 05 の 3.4・docs/kopis-api.md で分かっている分だけ。
 * 共通コードの一覧での確定は 05 の 7（06 の 11 の U-02）。
 */
public enum Genre {

	AAAA("연극"),
	GGGA("뮤지컬"),
	CCCA("서양음악(클래식)"),
	CCCC("한국음악(국악)"),
	CCCD("대중음악"),
	BBBC("무용"),
	BBBE("대중무용"),
	EEEA("복합"),
	EEEB("서커스/마술");

	private final String kopisName;

	Genre(String kopisName) {
		this.kopisName = kopisName;
	}

	public String code() {
		return name();
	}

	public String kopisName() {
		return kopisName;
	}

	public static Optional<Genre> fromKopisName(String name) {
		if (name == null) {
			return Optional.empty();
		}
		String key = name.strip();
		return Arrays.stream(values()).filter(g -> g.kopisName.equals(key)).findFirst();
	}

}
