package com.catticket.collect;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.catticket.common.code.SeoulDistrict;

/**
 * DistrictParser（03 の 4.2）。
 */
class DistrictParserTest {

	@Test
	void readsDistrictAfterSeoul() {
		// 実際の施設詳細の住所（venue-detail.xml）
		assertThat(DistrictParser.parse("서울특별시 마포구 독막로7길 23 (서교동)")).contains(SeoulDistrict.MAPO);
		assertThat(DistrictParser.parse("  서울특별시   종로구 대학로 116  ")).contains(SeoulDistrict.JONGNO);
		// 「서울특별시」の直後に続く区も取る
		assertThat(DistrictParser.parse("서울특별시종로구 대학로 116")).contains(SeoulDistrict.JONGNO);
	}

	@ParameterizedTest
	@EnumSource(SeoulDistrict.class)
	void allTwentyFiveDistricts(SeoulDistrict district) {
		assertThat(DistrictParser.parse("서울특별시 " + district.koreanName() + " 어딘가로 1")).contains(district);
	}

	@Test
	void doesNotConfuseSimilarNames() {
		// 「중구」と「중랑구」を前方一致で取り違えない
		assertThat(DistrictParser.parse("서울특별시 중랑구 망우로 1")).contains(SeoulDistrict.JUNGNANG);
		assertThat(DistrictParser.parse("서울특별시 중구 세종대로 110")).contains(SeoulDistrict.JUNG);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "서울특별시", "서울특별시 ", "경기도 성남시 분당구 1", "서울 종로구 대학로 116",
			"서울특별시 종로 대학로 116", "서울특별시 해운대구 1", "부산광역시 중구 1" })
	void otherwiseUndetermined(String address) {
		assertThat(DistrictParser.parse(address)).isEmpty();
	}

}
