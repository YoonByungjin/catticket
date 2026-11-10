package com.catticket.common.code;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class CodesTest {

	@Test
	void genreHasKnownCodes() {
		// 05 の 3.4 で分かっている 9 個（確定は U-02）
		assertThat(Arrays.stream(Genre.values()).map(Genre::code)).containsExactlyInAnyOrder("AAAA", "GGGA", "CCCA",
				"CCCC", "CCCD", "BBBC", "BBBE", "EEEA", "EEEB");
		assertThat(Genre.fromKopisName(" 뮤지컬 ")).contains(Genre.GGGA);
		assertThat(Genre.fromKopisName("없는 장르")).isEmpty();
		assertThat(Genre.fromKopisName(null)).isEmpty();
	}

	@Test
	void seoulHas25Districts() {
		assertThat(SeoulDistrict.values()).hasSize(25);
		assertThat(Arrays.stream(SeoulDistrict.values()).map(SeoulDistrict::koreanName).distinct()).hasSize(25)
			.allSatisfy(name -> assertThat(name).endsWith("구"));
		assertThat(SeoulDistrict.fromKoreanName("종로구")).contains(SeoulDistrict.JONGNO);
		assertThat(SeoulDistrict.fromKoreanName("수원시")).isEmpty();
	}

}
