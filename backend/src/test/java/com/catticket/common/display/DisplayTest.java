package com.catticket.common.display;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.catticket.common.code.Lang;
import com.catticket.common.validation.PageQuery;

class DisplayTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

	@Test
	void ratingRoundsHalfUpToOneDecimal() {
		assertThat(Rating.of(0, 0)).isEqualTo(new Rating(null, 0));
		// 17 / 4 = 4.25 → 4.3（AVG の途中の丸めでずれる境目の値。03 の 7.2）
		assertThat(Rating.of(17, 4).average()).isEqualByComparingTo(new BigDecimal("4.3"));
		assertThat(Rating.of(10, 3).average()).isEqualTo(new BigDecimal("3.3"));
		assertThat(Rating.of(5, 1).average()).isEqualTo(new BigDecimal("5.0"));
	}

	@Test
	void pageResponseCountsPages() {
		PageQuery query = new PageQuery(1, 20);
		assertThat(PageResponse.of(List.of("a"), query, 41)).isEqualTo(new PageResponse<>(List.of("a"), 1, 20, 41, 3));
		assertThat(PageResponse.of(List.of(), new PageQuery(null, null), 0).totalPages()).isZero();
		assertThat(PageResponse.of(List.of(), new PageQuery(null, null), 20).totalPages()).isEqualTo(1);
		assertThat(PageResponse.of(null, query, 0).items()).isEmpty();
	}

	@Test
	void performanceStatusByToday() {
		assertThat(PerformanceStatus.of(TODAY.minusDays(1), TODAY.plusDays(1), TODAY)).isEqualTo(PerformanceStatus.ONGOING);
		assertThat(PerformanceStatus.of(TODAY, TODAY, TODAY)).isEqualTo(PerformanceStatus.ONGOING);
		assertThat(PerformanceStatus.of(TODAY.plusDays(1), TODAY.plusDays(2), TODAY))
			.isEqualTo(PerformanceStatus.UPCOMING);
		assertThat(PerformanceStatus.of(TODAY.minusDays(2), TODAY.minusDays(1), TODAY)).isEqualTo(PerformanceStatus.ENDED);
	}

	@Test
	void localizedText() {
		Map<String, String> translations = Map.of("오페라의 유령", "オペラ座の怪人");
		assertThat(LocalizedTextFactory.of("오페라의 유령", Lang.JA, translations))
			.isEqualTo(new LocalizedText("オペラ座の怪人", "오페라의 유령", true));
		// ko はいつも翻訳しない
		assertThat(LocalizedTextFactory.of("오페라의 유령", Lang.KO, translations))
			.isEqualTo(new LocalizedText("오페라의 유령", "오페라의 유령", false));
		// 翻訳がない
		assertThat(LocalizedTextFactory.of("캣츠", Lang.JA, translations))
			.isEqualTo(new LocalizedText("캣츠", "캣츠", false));
		assertThat(LocalizedTextFactory.of(null, Lang.JA, translations)).isNull();
		assertThat(LocalizedTextFactory.of("캣츠", Lang.JA, null).translated()).isFalse();
	}

	@Test
	void stationNamePrefersOfficialJapanese() {
		Map<String, String> translations = Map.of("시청", "市庁（翻訳）");
		assertThat(LocalizedTextFactory.ofStation("시청", "市庁", Lang.JA, translations))
			.isEqualTo(new LocalizedText("市庁", "시청", true));
		assertThat(LocalizedTextFactory.ofStation("시청", null, Lang.JA, translations))
			.isEqualTo(new LocalizedText("市庁（翻訳）", "시청", true));
		assertThat(LocalizedTextFactory.ofStation("종각", null, Lang.JA, translations))
			.isEqualTo(new LocalizedText("종각", "종각", false));
		assertThat(LocalizedTextFactory.ofStation("시청", "市庁", Lang.KO, translations))
			.isEqualTo(new LocalizedText("시청", "시청", false));
	}

}
