package com.catticket.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.catticket.collect.client.xml.KopisGenreStat;
import com.catticket.collect.row.GenreTrendRow;
import com.catticket.common.code.Genre;

/**
 * GenreTrendCalculator（05 の 3.2 の「S6 の詳細」、A-06）。
 */
class GenreTrendCalculatorTest {

	private static KopisGenreStat stat(String name, String bookings) {
		return new KopisGenreStat(name, bookings);
	}

	@Test
	void sharesAreRoundedHalfUpToSixDecimalsInInputOrder() {
		List<GenreTrendRow> rows = GenreTrendCalculator.calculate(
				List.of(stat("연극", "1"), stat("합계", "999"), stat("뮤지컬", "2"), stat("대중무용", "0")));

		// 1/3 = 33.3333333… → 33.333333、2/3 = 66.6666666… → 66.666667（HALF_UP）。합계 の行は除く
		assertThat(rows).containsExactly(new GenreTrendRow(Genre.AAAA, 1, new BigDecimal("33.333333")),
				new GenreTrendRow(Genre.GGGA, 2, new BigDecimal("66.666667")),
				new GenreTrendRow(Genre.BBBE, 0, new BigDecimal("0.000000")));
	}

	@Test
	void halfUpAtTheSeventhDecimal() {
		// 1 / 1600000 × 100 = 0.0000625 → 0.000063（HALF_UP。HALF_EVEN なら 0.000062）
		List<GenreTrendRow> rows = GenreTrendCalculator
			.calculate(List.of(stat("연극", "1"), stat("뮤지컬", "1599999")));

		assertThat(rows.get(0).share()).isEqualByComparingTo("0.000063");
		assertThat(rows.get(1).share()).isEqualByComparingTo("99.999938");
	}

	@Test
	void usesNtssnmrssmOfTheRealResponse() {
		// 実際の応答（external/kopis/genre-stats.xml）の 9 ジャンルの ntssnmrssm と 합계（3,428,245）
		List<GenreTrendRow> rows = GenreTrendCalculator.calculate(List.of(stat("연극", "471333"),
				stat("무용(서양/한국무용)", "117300"), stat("대중무용", "2758"), stat("서양음악(클래식)", "465682"),
				stat("한국음악(국악)", "78978"), stat("대중음악", "1041510"), stat("복합", "27326"),
				stat("서커스/마술", "52152"), stat("뮤지컬", "1171206"), stat(" 합계 ", "3428245")));

		assertThat(rows).hasSize(9);
		assertThat(rows.stream().mapToLong(GenreTrendRow::bookingCount).sum()).isEqualTo(3428245L);
		assertThat(rows.get(0)).isEqualTo(new GenreTrendRow(Genre.AAAA, 471333, new BigDecimal("13.748521")));
		assertThat(rows.get(2)).isEqualTo(new GenreTrendRow(Genre.BBBE, 2758, new BigDecimal("0.080449")));
		assertThat(rows.get(8)).isEqualTo(new GenreTrendRow(Genre.GGGA, 1171206, new BigDecimal("34.163428")));
	}

	@Test
	void zeroTotalIsEmpty() {
		assertThat(GenreTrendCalculator.calculate(List.of(stat("연극", "0"), stat("합계", "0")))).isEmpty();
		assertThat(GenreTrendCalculator.calculate(List.of())).isEmpty();
	}

	@Test
	void unknownGenreFails() {
		assertThatThrownBy(() -> GenreTrendCalculator.calculate(List.of(stat("연극", "1"), stat("오페라", "2"))))
			.isInstanceOf(InvalidGenreStatException.class)
			.hasMessage("unknown genre: 오페라");
	}

	@Test
	void duplicateGenreFails() {
		assertThatThrownBy(() -> GenreTrendCalculator.calculate(List.of(stat("연극", "1"), stat(" 연극", "2"))))
			.isInstanceOf(InvalidGenreStatException.class)
			.hasMessage("duplicate genre: 연극");
	}

	@ParameterizedTest
	@ValueSource(strings = { "-1", "abc", "1.5", "" })
	void invalidBookingsFail(String value) {
		assertThatThrownBy(() -> GenreTrendCalculator.calculate(List.of(stat("연극", value))))
			.isInstanceOf(InvalidGenreStatException.class)
			.hasMessageStartingWith("invalid ntssnmrssm: 연극=");
	}

	@Test
	void missingBookingsFail() {
		assertThatThrownBy(() -> GenreTrendCalculator.calculate(List.of(stat("연극", null))))
			.isInstanceOf(InvalidGenreStatException.class)
			.hasMessage("invalid ntssnmrssm: 연극=null");
	}

}
