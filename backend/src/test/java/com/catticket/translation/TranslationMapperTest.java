package com.catticket.translation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import com.catticket.common.text.Hashes;
import com.catticket.translation.row.TranslationInsert;
import com.catticket.translation.row.TranslationRow;

/**
 * TranslationMapper（実際の MySQL のテスト用 DB）。各テストはロールバックされる。
 * 05 の 3.7 の対象・対象外、同じ原文のまとめ、翻訳済みを読まないこと、保存の冪等を確かめる。
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TranslationMapperTest {

	private static final LocalDateTime T1 = LocalDateTime.of(2026, 11, 24, 6, 0, 0);

	// ほかのテストのデータが残っていても全部読めるように大きくする
	private static final int ALL = 1_000_000;

	// ほかのテストのデータとぶつからない原文の印
	private static final String P = "번역시험-";

	@Autowired
	TranslationMapper mapper;

	@Autowired
	JdbcTemplate jdbc;

	private long venue(String kopisId, String name, String address) {
		jdbc.update("INSERT INTO venue (kopis_venue_id, name, address, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
				kopisId, name, address, T1, T1);
		return jdbc.queryForObject("SELECT id FROM venue WHERE kopis_venue_id = ?", Long.class, kopisId);
	}

	private long performance(String kopisId, long venueId, String title, String cast, String price, String schedule,
			String runtime, String age) {
		jdbc.update("""
				INSERT INTO performance (kopis_performance_id, venue_id, title, start_date, end_date, genre_code,
				  cast_text, ticket_price, schedule_text, runtime, age_limit, is_seoul, collected_at, created_at, updated_at)
				VALUES (?, ?, ?, '2026-12-01', '2026-12-31', 'AAAA', ?, ?, ?, ?, ?, TRUE, ?, ?, ?)
				""", kopisId, venueId, title, cast, price, schedule, runtime, age, T1, T1, T1);
		return jdbc.queryForObject("SELECT id FROM performance WHERE kopis_performance_id = ?", Long.class, kopisId);
	}

	private void override(long performanceId, String field, String value) {
		jdbc.update("""
				INSERT INTO performance_override (performance_id, field_name, override_value, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?)
				""", performanceId, field, value, T1, T1);
	}

	private void bookingSite(long performanceId, String source, String name, int order) {
		jdbc.update("""
				INSERT INTO booking_site (performance_id, source, name, url, sort_order, created_at, updated_at)
				VALUES (?, ?, ?, 'https://ticket.test', ?, ?, ?)
				""", performanceId, source, name, order, T1, T1);
	}

	private long station(String code, int line, String nameKo, String nameJa) {
		jdbc.update("""
				INSERT INTO station (external_station_code, line, name_ko, name_ja, latitude, longitude, created_at, updated_at)
				VALUES (?, ?, ?, ?, 37.5, 127.0, ?, ?)
				""", code, line, nameKo, nameJa, T1, T1);
		return jdbc.queryForObject("SELECT id FROM station WHERE external_station_code = ?", Long.class, code);
	}

	private void locker(long stationId, String name, String location) {
		jdbc.update("INSERT INTO coin_locker (station_id, name, location_text, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
				stationId, name, location, T1, T1);
	}

	private void translated(String source, String text) {
		mapper.insertTranslations(List.of(new TranslationInsert(Hashes.sha256Hex(source), source, text, T1)));
	}

	/** この印の付いた原文だけ（ほかのテストの残りを除く） */
	private static List<String> mine(List<String> texts) {
		return texts.stream().filter(t -> t.startsWith(P)).toList();
	}

	@Test
	void titlesIncludeOverridesOnceEachAndCaseSensitive() {
		long v = venue("FCTRTEST01", P + "극장", null);
		long p1 = performance("PFTRTEST01", v, P + "레미제라블", null, null, null, null, null);
		performance("PFTRTEST02", v, P + "레미제라블", null, null, null, null, null);
		performance("PFTRTEST03", v, P + "Show", null, null, null, null, null);
		performance("PFTRTEST04", v, P + "show", null, null, null, null, null);
		override(p1, "title", P + "레미제라블 (수정)");
		// 公演名でない修正は公演名に入らない
		override(p1, "cast_text", P + "출연 수정");

		List<String> titles = mine(mapper.findUntranslatedTitles(ALL));

		assertThat(titles).containsExactlyInAnyOrder(P + "레미제라블", P + "레미제라블 (수정)", P + "Show", P + "show");
	}

	@Test
	void originalsDifferingOnlyInTrailingSpacesAreSeparate() {
		// utf8mb4_0900_bin（NO PAD）: 04 のハッシュと同じく末尾の空白も区別する（06 の 4.10 の 1.。U-15 ①）
		long v = venue("FCTRTEST01", P + "극장", null);
		performance("PFTRTEST01", v, P + "abc", null, null, null, null, null);
		performance("PFTRTEST02", v, P + "abc ", null, null, null, null, null);

		assertThat(mine(mapper.findUntranslatedTitles(ALL))).containsExactlyInAnyOrder(P + "abc", P + "abc ");
	}

	@Test
	void translatedOriginalsAreNotRead() {
		long v = venue("FCTRTEST01", P + "극장", null);
		performance("PFTRTEST01", v, P + "공연가", null, null, null, null, null);
		performance("PFTRTEST02", v, P + "공연나", null, null, null, null, null);
		translated(P + "공연가", "公演A");
		translated(P + "극장", "劇場");

		assertThat(mine(mapper.findUntranslatedTitles(ALL))).containsExactly(P + "공연나");
		assertThat(mine(mapper.findUntranslatedVenueNames(ALL))).isEmpty();
	}

	@Test
	void limitIsApplied() {
		long v = venue("FCTRTEST01", P + "극장", null);
		performance("PFTRTEST01", v, P + "공연가", null, null, null, null, null);
		performance("PFTRTEST02", v, P + "공연나", null, null, null, null, null);

		assertThat(mapper.findUntranslatedTitles(1)).hasSize(1);
	}

	@Test
	void venueNames() {
		venue("FCTRTEST01", P + "예술의전당", null);
		venue("FCTRTEST02", P + "예술의전당", null);
		venue("FCTRTEST03", P + "세종문화회관", null);

		assertThat(mine(mapper.findUntranslatedVenueNames(ALL))).containsExactlyInAnyOrder(P + "예술의전당",
				P + "세종문화회관");
	}

	@Test
	void stationNamesWithoutOfficialJapaneseOnly() {
		station("TRTEST01", 1, P + "서울역", "ソウル駅");
		station("TRTEST02", 2, P + "복정", null);
		station("TRTEST03", 3, P + "가락시장", " ");

		assertThat(mine(mapper.findUntranslatedStationNames(ALL))).containsExactlyInAnyOrder(P + "복정", P + "가락시장");
	}

	@Test
	void othersCoverEveryTargetAndSkipNullNamesAndEmptyTexts() {
		long v = venue("FCTRTEST01", P + "극장", P + "서울 종로구 대학로 1");
		long p = performance("PFTRTEST01", v, P + "공연", P + "출연", P + "가격", P + "시간", P + "2시간", P + "만 7세");
		performance("PFTRTEST02", v, P + "공연2", "", null, null, null, null);
		override(p, "ticket_price", P + "가격 수정");
		override(p, "age_limit", P + "만 12세");
		override(p, "genre_code", P + "GGGA");
		bookingSite(p, "COLLECTED", P + "인터파크", 1);
		bookingSite(p, "COLLECTED", null, 2);
		bookingSite(p, "ADMIN", P + "예스24", 1);
		long st = station("TRTEST01", 1, P + "역", null);
		locker(st, P + "보관함", P + "1번 출입구 주변");
		// 公演名と同じ文が出演者にあっても、それ以外の読みでは1つ
		performance("PFTRTEST03", v, P + "공연3", P + "출연", null, null, null, null);

		List<String> others = mapper.findUntranslatedOthers(ALL);

		assertThat(others).doesNotContainNull().doesNotContain("");
		assertThat(mine(others)).containsExactlyInAnyOrder(P + "출연", P + "가격", P + "시간", P + "2시간", P + "만 7세",
				P + "가격 수정", P + "만 12세", P + "인터파크", P + "예스24", P + "서울 종로구 대학로 1", P + "1번 출입구 주변");
	}

	@Test
	void insertIsIdempotentForTheSameOriginal() {
		translated(P + "공연", "公演");
		translated(P + "공연", "別の訳");

		Map<String, Object> row = jdbc.queryForMap(
				"SELECT COUNT(*) AS n, MAX(translated_text) AS t FROM translation WHERE source_hash = ?",
				Hashes.sha256Hex(P + "공연"));
		assertThat(((Number) row.get("n")).longValue()).isEqualTo(1);
		assertThat(row.get("t")).isEqualTo("公演");
	}

	@Test
	void javaHashMatchesSqlSha2() {
		// 04 の 11.2.7: アプリの SHA-256 と MySQL の SHA2(文, 256) が同じ（日本語の検索で SQL から引くため）
		String source = P + "𠮷 뮤지컬 Ａ";
		String sql = jdbc.queryForObject("SELECT SHA2(?, 256)", String.class, source);

		assertThat(Hashes.sha256Hex(source)).isEqualTo(sql);
	}

	@Test
	void findByHashesReturnsOnlyExistingRows() {
		translated(P + "가", "A");
		translated(P + "나", "B");

		List<TranslationRow> rows = mapper.findByHashes(
				List.of(Hashes.sha256Hex(P + "가"), Hashes.sha256Hex(P + "나"), Hashes.sha256Hex(P + "없음")));

		assertThat(rows).containsExactlyInAnyOrder(new TranslationRow(Hashes.sha256Hex(P + "가"), "A"),
				new TranslationRow(Hashes.sha256Hex(P + "나"), "B"));
		assertThat(mapper.findByHashes(Collections.singletonList(Hashes.sha256Hex(P + "없음")))).isEmpty();
	}

}
