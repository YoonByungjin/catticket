package com.catticket.collect;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.catticket.collect.client.xml.KopisPerformanceDetail;
import com.catticket.collect.client.xml.KopisRelate;
import com.catticket.collect.client.xml.KopisVenueDetail;
import com.catticket.collect.row.BookingSiteValue;
import com.catticket.collect.row.PerformanceValues;
import com.catticket.collect.row.VenueValues;

/**
 * ProviderValues（05 の 3.3 の 10.・A-02 の長さの規則、06 の 7.1 の「値の形」）。
 */
class ProviderValuesTest {

	static KopisPerformanceDetail detail(String mt20id, String mt10id, String genrenm, String prfnm, String poster,
			List<KopisRelate> relates) {
		return new KopisPerformanceDetail(mt20id, prfnm, "2026.12.04", "2027.05.02", "조승우, 홍광호", "2시간 50분",
				"만 13세 이상", "VIP석 180,000원", "화요일(19:30)", poster, "서울특별시", genrenm, "N",
				"2026-11-09 14:52:19", mt10id, relates);
	}

	static KopisPerformanceDetail detail() {
		return detail("PF303043", "FC000011", "뮤지컬", "스위니토드", "http://www.kopis.or.kr/p.gif",
				List.of(new KopisRelate("NHN티켓링크", "https://www.ticketlink.co.kr/product/66145")));
	}

	@Test
	void convertsAllColumns() {
		Conversion<PerformanceValues> c = ProviderValues.performance(detail());

		assertThat(c.skipped()).isFalse();
		assertThat(c.notes()).isEmpty();
		assertThat(c.value()).isEqualTo(new PerformanceValues("PF303043", "FC000011", "스위니토드",
				LocalDate.of(2026, 12, 4), LocalDate.of(2027, 5, 2), "GGGA", "조승우, 홍광호", "VIP석 180,000원",
				"화요일(19:30)", "2시간 50분", "만 13세 이상", "http://www.kopis.or.kr/p.gif", false, true,
				LocalDateTime.of(2026, 11, 9, 14, 52, 19),
				List.of(new BookingSiteValue("NHN티켓링크", "https://www.ticketlink.co.kr/product/66145", 1))));
	}

	@Test
	void openRunAndArea() {
		KopisPerformanceDetail d = new KopisPerformanceDetail("PF1", "공연", "2014.09.11", "2099.12.31", null, null,
				null, null, null, null, "경기도", "연극", "Y", null, "FC1", List.of());

		PerformanceValues v = ProviderValues.performance(d).value();

		assertThat(v.openRun()).isTrue();
		assertThat(v.areaSeoul()).isFalse();
		assertThat(v.castText()).isNull();
		assertThat(v.kopisUpdatedAt()).isNull();
		assertThat(v.bookingSites()).isEmpty();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "PF0123456789012345678" })
	void invalidPerformanceIdIsNotSaved(String id) {
		// 21 文字・空は切り詰めると別の公演になるので保存しない
		Conversion<PerformanceValues> c = ProviderValues
			.performance(detail(id, "FC1", "연극", "공연", null, List.of()));

		assertThat(c.skipped()).isTrue();
		assertThat(c.notes()).extracting(CollectNote::name).containsExactly("skippedId");
	}

	@Test
	void twentyCharacterIdIsKept() {
		String id = "PF012345678901234567";
		assertThat(ProviderValues.performance(detail(id, "FC1", "연극", "공연", null, List.of())).value()
			.kopisPerformanceId()).isEqualTo(id);
		assertThat(ProviderValues.isValidId(id)).isTrue();
		assertThat(ProviderValues.isValidId(id + "8")).isFalse();
	}

	@Test
	void invalidVenueIdIsNotSaved() {
		Conversion<PerformanceValues> c = ProviderValues
			.performance(detail("PF1", "FC01234567890123456789", "연극", "공연", null, List.of()));

		assertThat(c.skipped()).isTrue();
		assertThat(c.notes()).containsExactly(CollectNote.of("skippedVenueId", "PF1|mt10id=FC01234567890123456789"));
	}

	@Test
	void unknownGenreIsNotSaved() {
		// 旧い名前「무용」は一覧にない（正しくは 무용(서양/한국무용)。06 の 11 の U-02）
		Conversion<PerformanceValues> c = ProviderValues.performance(detail("PF1", "FC1", "무용", "공연", null, List.of()));

		assertThat(c.skipped()).isTrue();
		assertThat(c.notes()).containsExactly(CollectNote.of("skippedGenre", "PF1|무용"));
	}

	@Test
	void danceGenreIsBbbc() {
		assertThat(ProviderValues.performance(detail("PF1", "FC1", "무용(서양/한국무용)", "공연", null, List.of()))
			.value()
			.genreCode()).isEqualTo("BBBC");
	}

	@Test
	void missingTitleOrBadDatesAreNotSaved() {
		assertThat(ProviderValues.performance(detail("PF1", "FC1", "연극", null, null, List.of())).notes())
			.containsExactly(CollectNote.of("skippedInvalid", "PF1|prfnm"));

		KopisPerformanceDetail badDate = new KopisPerformanceDetail("PF1", "공연", "2026-12-04", "2027.05.02", null,
				null, null, null, null, null, null, "연극", null, null, "FC1", List.of());
		assertThat(ProviderValues.performance(badDate).notes())
			.containsExactly(CollectNote.of("skippedInvalid", "PF1|prfpdfrom=2026-12-04"));
	}

	@Test
	void longTextsAreTruncatedByCodePoints() {
		// 4 バイトの文字（サロゲートペア）で数え、ペアを割らない
		String title = "🎭".repeat(501);
		Conversion<PerformanceValues> c = ProviderValues
			.performance(detail("PF1", "FC1", "연극", title, null, List.of()));

		assertThat(c.value().title()).isEqualTo("🎭".repeat(500));
		assertThat(c.notes()).containsExactly(CollectNote.of("truncated", "PF1|title"));
	}

	@Test
	void exactlyMaxLengthIsNotTruncated() {
		Conversion<PerformanceValues> c = ProviderValues
			.performance(detail("PF1", "FC1", "연극", "가".repeat(500), null, List.of()));

		assertThat(c.value().title()).hasSize(500);
		assertThat(c.notes()).isEmpty();
	}

	@Test
	void shortTextColumnsAre200CodePoints() {
		KopisPerformanceDetail d = new KopisPerformanceDetail("PF1", "공연", "2026.12.04", "2027.05.02", null,
				"분".repeat(201), "세".repeat(201), null, null, null, null, "연극", null, null, "FC1", List.of());

		Conversion<PerformanceValues> c = ProviderValues.performance(d);

		assertThat(c.value().runtime()).hasSize(200);
		assertThat(c.value().ageLimit()).hasSize(200);
		assertThat(c.notes()).containsExactly(CollectNote.of("truncated", "PF1|runtime"),
				CollectNote.of("truncated", "PF1|ageLimit"));
	}

	@Test
	void textColumnsAre65535Bytes() {
		// 한 は UTF-8 で 3 バイト。21,846 文字 = 65,538 バイト → 21,845 文字（65,535 バイト）
		String cast = "한".repeat(21846);
		KopisPerformanceDetail d = new KopisPerformanceDetail("PF1", "공연", "2026.12.04", "2027.05.02", cast, null,
				null, "a".repeat(65535), "b".repeat(65536), null, null, "연극", null, null, "FC1", List.of());

		Conversion<PerformanceValues> c = ProviderValues.performance(d);

		assertThat(c.value().castText()).hasSize(21845);
		assertThat(c.value().ticketPrice()).hasSize(65535);
		assertThat(c.value().scheduleText()).hasSize(65535);
		assertThat(c.notes()).containsExactly(CollectNote.of("truncated", "PF1|cast"),
				CollectNote.of("truncated", "PF1|schedule"));
	}

	@Test
	void longPosterUrlIsNull() {
		String url = "http://x/" + "a".repeat(1992);
		Conversion<PerformanceValues> c = ProviderValues.performance(detail("PF1", "FC1", "연극", "공연", url, List.of()));

		assertThat(c.value().posterUrl()).isNull();
		assertThat(c.notes()).containsExactly(CollectNote.of("posterUrlTooLong", "PF1"));
		// 2,000 文字ちょうどは保存する
		String ok = "http://x/" + "a".repeat(1991);
		assertThat(ProviderValues.performance(detail("PF1", "FC1", "연극", "공연", ok, List.of())).value().posterUrl())
			.isEqualTo(ok);
	}

	@Test
	void bookingSitesKeepOrderAndSkipItemsWithoutUsableUrl() {
		List<KopisRelate> relates = List.of(new KopisRelate("A", "https://a/"), new KopisRelate("B", null),
				new KopisRelate(null, "https://c/"), new KopisRelate("D", "https://d/" + "x".repeat(1991)),
				new KopisRelate("이".repeat(201), "https://e/"));

		Conversion<PerformanceValues> c = ProviderValues.performance(detail("PF1", "FC1", "연극", "공연", null, relates));

		// URL がない・長すぎる項目は保存せず、並び順は詰めて 1, 2, 3
		assertThat(c.value().bookingSites()).containsExactly(new BookingSiteValue("A", "https://a/", 1),
				new BookingSiteValue(null, "https://c/", 2), new BookingSiteValue("이".repeat(200), "https://e/", 3));
		assertThat(c.notes()).containsExactly(CollectNote.of("bookingSiteNoUrl", "PF1"),
				CollectNote.of("bookingSiteUrlTooLong", "PF1"), CollectNote.of("truncated", "PF1|bookingSiteName"));
	}

	@Test
	void updatedAtParsing() {
		assertThat(ProviderValues.updatedAt("2026-11-09 14:52:19")).isEqualTo(LocalDateTime.of(2026, 11, 9, 14, 52, 19));
		// 小数の秒は切り捨てる（繰り上げない。06 の 7.1）
		assertThat(ProviderValues.updatedAt("2026-11-06 18:00:03.02675"))
			.isEqualTo(LocalDateTime.of(2026, 11, 6, 18, 0, 3));
		assertThat(ProviderValues.updatedAt("2026-11-06 18:00:03.999999999"))
			.isEqualTo(LocalDateTime.of(2026, 11, 6, 18, 0, 3));
		assertThat(ProviderValues.updatedAt("2026-11-06 18:00:03.5")).isEqualTo(LocalDateTime.of(2026, 11, 6, 18, 0, 3));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "2026-11-06 18:00:03.", "2026-11-06T18:00:03", "2026-11-06", "2026-02-30 00:00:00",
			"2026-11-06 18:00:03.0123456789", "2026-11-06 24:00:00" })
	void invalidUpdatedAtIsNull(String value) {
		assertThat(ProviderValues.updatedAt(value)).isNull();
	}

	@Test
	void venueValues() {
		// 実際の施設詳細（venue-detail.xml）
		Conversion<VenueValues> c = ProviderValues.venue("FC004949", new KopisVenueDetail("FC004949", "로드 유미플레이 1",
				"서울특별시 마포구 독막로7길 23 (서교동)", "37.548989000000000", "126.91894500000000"));

		assertThat(c.notes()).isEmpty();
		assertThat(c.value()).isEqualTo(new VenueValues("FC004949", "로드 유미플레이 1", "서울특별시 마포구 독막로7길 23 (서교동)",
				"마포구", new BigDecimal("37.548989"), new BigDecimal("126.918945")));
	}

	@Test
	void coordinatesAreRoundedHalfUp() {
		assertThat(ProviderValues.coordinate("37.5489895", 90)).isEqualTo(new BigDecimal("37.548990"));
		assertThat(ProviderValues.coordinate("-90", 90)).isEqualTo(new BigDecimal("-90.000000"));
		assertThat(ProviderValues.coordinate("180.0000004", 180)).isEqualTo(new BigDecimal("180.000000"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "abc", "90.000001", "-91" })
	void invalidLatitudeMakesBothNull(String la) {
		Conversion<VenueValues> c = ProviderValues.venue("FC1",
				new KopisVenueDetail("FC1", "극장", "서울특별시 종로구 대학로 1", la, "126.9"));

		assertThat(c.value().latitude()).isNull();
		assertThat(c.value().longitude()).isNull();
		assertThat(c.notes()).containsExactly(CollectNote.of("venueNoLocation", "FC1"));
	}

	@Test
	void venueWithoutLocationOrDistrictIsSavedAndCounted() {
		Conversion<VenueValues> c = ProviderValues.venue("FC1", new KopisVenueDetail("FC1", "극장", null, null, "126.9"));

		assertThat(c.skipped()).isFalse();
		assertThat(c.value().district()).isNull();
		assertThat(c.value().latitude()).isNull();
		assertThat(c.notes()).containsExactly(CollectNote.of("venueNoDistrict", "FC1"),
				CollectNote.of("venueNoLocation", "FC1"));
	}

	@Test
	void venueTextsAreTruncated() {
		Conversion<VenueValues> c = ProviderValues.venue("FC1", new KopisVenueDetail("FC1", "극".repeat(201),
				"서울특별시 종로구 " + "길".repeat(500), "37.5", "126.9"));

		assertThat(c.value().name()).hasSize(200);
		assertThat(c.value().address()).hasSize(500);
		assertThat(c.value().district()).isEqualTo("종로구");
		assertThat(c.notes()).containsExactly(CollectNote.of("truncated", "FC1|venueName"),
				CollectNote.of("truncated", "FC1|address"));
	}

	@Test
	void venueWithoutNameIsNotSaved() {
		Conversion<VenueValues> c = ProviderValues.venue("FC1", new KopisVenueDetail("FC1", null, "서울특별시 종로구", null, null));

		assertThat(c.skipped()).isTrue();
		assertThat(c.notes()).containsExactly(CollectNote.of("skippedVenueInvalid", "FC1|fcltynm"));
	}

	@Test
	void seoulAddress() {
		assertThat(ProviderValues.isSeoulAddress(" 서울특별시 종로구")).isTrue();
		assertThat(ProviderValues.isSeoulAddress("경기도 성남시")).isFalse();
		assertThat(ProviderValues.isSeoulAddress(null)).isFalse();
	}

}
