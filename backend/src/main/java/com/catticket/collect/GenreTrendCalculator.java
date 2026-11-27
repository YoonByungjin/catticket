package com.catticket.collect;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.catticket.collect.client.xml.KopisGenreStat;
import com.catticket.collect.row.GenreTrendRow;
import com.catticket.common.code.Genre;

/**
 * ジャンル別の予約数と割合（05 の 3.2 の「S6 の詳細」、06 の 4.9、A-06）。Spring に依存しない。
 * 予約数は販売枚数 {@code ntssnmrssm}。ほかの数の項目は読まない。
 */
public final class GenreTrendCalculator {

	/** 合計の行の名前（除く） */
	static final String TOTAL = "합계";

	/** 割合の小数の桁（genre_trend.share は DECIMAL(9,6)） */
	static final int SHARE_SCALE = 6;

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private GenreTrendCalculator() {
	}

	/**
	 * @return 入力の順の行。全ジャンルの予約数の合計が 0 なら空（割合を計算できないので保存しない）
	 * @throws InvalidGenreStatException 知らないジャンル・同じジャンルが2行・予約数が数でない・空・負
	 */
	public static List<GenreTrendRow> calculate(List<KopisGenreStat> stats) {
		record Count(Genre genre, long bookings) {
		}
		List<Count> counts = new ArrayList<>();
		Set<Genre> seen = EnumSet.noneOf(Genre.class);
		long total = 0;
		for (KopisGenreStat stat : stats) {
			String name = stat.catenm() == null ? null : stat.catenm().strip();
			if (TOTAL.equals(name)) {
				continue;
			}
			Optional<Genre> genre = Genre.fromKopisName(name);
			if (genre.isEmpty()) {
				// 分母が変わり、すべての割合が変わるので、その回は何も保存しない
				throw new InvalidGenreStatException("unknown genre: " + name);
			}
			if (!seen.add(genre.get())) {
				throw new InvalidGenreStatException("duplicate genre: " + name);
			}
			long bookings = bookings(name, stat.ntssnmrssm());
			counts.add(new Count(genre.get(), bookings));
			total += bookings;
		}
		if (total == 0) {
			return List.of();
		}
		BigDecimal denominator = BigDecimal.valueOf(total);
		return counts.stream()
			.map(c -> new GenreTrendRow(c.genre(), c.bookings(),
					BigDecimal.valueOf(c.bookings()).multiply(HUNDRED).divide(denominator, SHARE_SCALE,
							RoundingMode.HALF_UP)))
			.toList();
	}

	private static long bookings(String name, String value) {
		String text = value == null ? null : value.strip();
		long n;
		try {
			n = Long.parseLong(text);
		}
		catch (NumberFormatException e) {
			throw new InvalidGenreStatException("invalid ntssnmrssm: " + name + "=" + value);
		}
		if (n < 0) {
			// 確認した範囲では起きていない。起きたら提供元の値の意味が変わったとみなす
			throw new InvalidGenreStatException("invalid ntssnmrssm: " + name + "=" + value);
		}
		return n;
	}

}
