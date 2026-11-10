package com.catticket.common.text;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class TextsTest {

	private static final String CAT = "🐱"; // 絵文字 1 文字（UTF-16 で 2 単位、UTF-8 で 4 バイト）

	@Test
	void stripToNull() {
		assertThat(Texts.stripToNull(null)).isNull();
		assertThat(Texts.stripToNull(" 　 ")).isNull();
		assertThat(Texts.stripToNull("　 a b \t")).isEqualTo("a b");
	}

	@Test
	void codePointLength() {
		assertThat(Texts.codePointLength(null)).isZero();
		assertThat(Texts.codePointLength(CAT + "a")).isEqualTo(2);
		assertThat(Texts.codePointLength("고양이")).isEqualTo(3);
	}

	@Test
	void hasControlChars() {
		assertThat(Texts.hasControlChars(null, false)).isFalse();
		assertThat(Texts.hasControlChars("abc", false)).isFalse();
		assertThat(Texts.hasControlChars("a\tb", false)).isTrue();
		assertThat(Texts.hasControlChars("a\nb", false)).isTrue();
		assertThat(Texts.hasControlChars("a\r\nb", true)).isFalse();
		assertThat(Texts.hasControlChars("a\tb", true)).isTrue();
		assertThat(Texts.hasControlChars("a\u007Fb", true)).isTrue();
		// 書式文字（Cf）は Cc ではない
		assertThat(Texts.hasControlChars("a​b", false)).isFalse();
	}

	@Test
	void truncateCodePointsKeepsSurrogatePairs() {
		assertThat(Texts.truncateCodePoints(null, 3)).isNull();
		assertThat(Texts.truncateCodePoints("abc", 3)).isEqualTo("abc");
		assertThat(Texts.truncateCodePoints(CAT + CAT + CAT, 2)).isEqualTo(CAT + CAT);
	}

	@Test
	void truncateUtf8BytesStopsAtCharacterBoundary() {
		assertThat(Texts.truncateUtf8Bytes("abc", 3)).isEqualTo("abc");
		// 「가」は 3 バイト。4 バイトに収めると 1 文字だけ
		assertThat(Texts.truncateUtf8Bytes("가나", 4)).isEqualTo("가");
		String truncated = Texts.truncateUtf8Bytes("a" + CAT, 4);
		assertThat(truncated).isEqualTo("a");
		assertThat(truncated.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(4);
	}

}
