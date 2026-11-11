package com.catticket.common.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NicknamesAndHashesTest {

	@Test
	void nicknameIsNfkcThenStripped() {
		// 03 の 7.1 の例
		assertThat(Nicknames.normalize("Ｃａｔ１")).isEqualTo("Cat1");
		assertThat(Nicknames.normalize("ﾈｺ")).isEqualTo("ネコ");
		assertThat(Nicknames.normalize("①")).isEqualTo("1");
		assertThat(Nicknames.normalize("㍿")).isEqualTo("株式会社");
		assertThat(Nicknames.normalize("Cat")).isEqualTo("Cat");
		// 分解されたハングル → 合成済み
		assertThat(Nicknames.normalize("가")).isEqualTo("가");
		// U+00A0 は NFKC で U+0020 になり、前後から除かれる
		assertThat(Nicknames.normalize(" Cat ")).isEqualTo("Cat");
		assertThat(Nicknames.normalize("  ")).isNull();
		assertThat(Nicknames.normalize(null)).isNull();
	}

	@Test
	void sha256Hex() {
		assertThat(Hashes.sha256Hex("abc"))
			.isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
		assertThat(Hashes.sha256Hex("")).hasSize(64);
	}

}
