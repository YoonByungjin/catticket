package com.catticket.collect;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import com.catticket.collect.row.BookingSiteValue;
import com.catticket.collect.row.CollectVenueRow;
import com.catticket.collect.row.GenreTrendRow;
import com.catticket.collect.row.PerformanceValues;
import com.catticket.collect.row.RankingCollectionKey;
import com.catticket.collect.row.RankingEntryInsert;
import com.catticket.collect.row.VenueValues;
import com.catticket.common.code.Genre;
import com.catticket.common.code.PeriodType;
import com.catticket.common.display.Period;

/**
 * CollectMapper（実際の MySQL のテスト用 DB）。各テストはロールバックされる。
 * 自動収集が管理者の修正・非表示・受賞歴を上書きしないこと、入れ替えが冪等なことを確かめる。
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CollectMapperTest {

	private static final LocalDateTime T1 = LocalDateTime.of(2026, 11, 24, 6, 0, 0);

	private static final LocalDateTime T2 = T1.plusDays(1);

	// ほかのテストのデータとぶつからない ID
	private static final String VENUE = "FCTEST0001";

	private static final String PERF = "PFTEST0001";

	@Autowired
	CollectMapper mapper;

	@Autowired
	JdbcTemplate jdbc;

	private static VenueValues venue(String name) {
		return new VenueValues(VENUE, name, "서울특별시 마포구 독막로7길 23", "마포구", new BigDecimal("37.548989"),
				new BigDecimal("126.918945"));
	}

	private static PerformanceValues performance(String title, List<BookingSiteValue> sites) {
		return new PerformanceValues(PERF, VENUE, title, LocalDate.of(2026, 12, 4), LocalDate.of(2027, 5, 2), "GGGA",
				"조승우", "VIP석 180,000원", "화요일(19:30)", "2시간 50분", "만 13세 이상", "http://x/p.gif", false, true,
				LocalDateTime.of(2026, 11, 9, 14, 52, 19), sites);
	}

	private long saveVenue() {
		mapper.upsertVenue(venue("로드 유미플레이 1"), T1);
		return mapper.findVenueByKopisId(VENUE).orElseThrow().id();
	}

	private long savePerformance(long venueId, String title, LocalDateTime now) {
		mapper.upsertPerformance(performance(title, List.of()), venueId, true, now);
		return mapper.findPerformanceIdByKopisId(PERF).orElseThrow();
	}

	@Test
	void venueIsInsertedAndUpdated() {
		long id = saveVenue();

		assertThat(mapper.findVenueByKopisId(VENUE)).contains(new CollectVenueRow(id, "서울특별시 마포구 독막로7길 23"));
		Map<String, Object> row = jdbc.queryForMap("SELECT * FROM venue WHERE id = ?", id);
		assertThat(row.get("name")).isEqualTo("로드 유미플레이 1");
		assertThat(row.get("district")).isEqualTo("마포구");
		assertThat(row.get("latitude")).isEqualTo(new BigDecimal("37.548989"));
		assertThat(row.get("created_at")).isEqualTo(T1);

		// 座標のない会場も保存する（両方 NULL）
		mapper.upsertVenue(new VenueValues(VENUE, "새 이름", null, null, null, null), T2);

		Map<String, Object> updated = jdbc.queryForMap("SELECT * FROM venue WHERE id = ?", id);
		assertThat(updated.get("name")).isEqualTo("새 이름");
		assertThat(updated.get("latitude")).isNull();
		assertThat(updated.get("created_at")).isEqualTo(T1);
		assertThat(updated.get("updated_at")).isEqualTo(T2);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM venue WHERE kopis_venue_id = ?", Integer.class, VENUE))
			.isEqualTo(1);
	}

	@Test
	void unknownIdsAreEmpty() {
		assertThat(mapper.findVenueByKopisId("FCNONE")).isEmpty();
		assertThat(mapper.findPerformanceIdByKopisId("PFNONE")).isEmpty();
	}

	@Test
	void performanceIsInsertedWithAllColumns() {
		long venueId = saveVenue();
		long id = savePerformance(venueId, "스위니토드", T1);

		Map<String, Object> row = jdbc.queryForMap("SELECT * FROM performance WHERE id = ?", id);
		assertThat(row.get("kopis_performance_id")).isEqualTo(PERF);
		assertThat(((Number) row.get("venue_id")).longValue()).isEqualTo(venueId);
		assertThat(row.get("title")).isEqualTo("스위니토드");
		assertThat(((java.sql.Date) row.get("start_date")).toLocalDate()).isEqualTo(LocalDate.of(2026, 12, 4));
		assertThat(row.get("genre_code")).isEqualTo("GGGA");
		assertThat(row.get("cast_text")).isEqualTo("조승우");
		assertThat(row.get("poster_url")).isEqualTo("http://x/p.gif");
		assertThat(row.get("is_open_run")).isEqualTo(false);
		assertThat(row.get("is_seoul")).isEqualTo(true);
		assertThat(row.get("is_hidden")).isEqualTo(false);
		assertThat(row.get("has_award")).isEqualTo(false);
		assertThat(row.get("kopis_updated_at")).isEqualTo(LocalDateTime.of(2026, 11, 9, 14, 52, 19));
		assertThat(row.get("collected_at")).isEqualTo(T1);
		assertThat(row.get("created_at")).isEqualTo(T1);
	}

	@Test
	void upsertDoesNotOverwriteHiddenAwardOrAdminOverrides() {
		long venueId = saveVenue();
		long id = savePerformance(venueId, "스위니토드", T1);
		// 管理者が非表示にし、公演名を修正した。S3 が受賞歴を立てた
		jdbc.update("UPDATE performance SET is_hidden = TRUE WHERE id = ?", id);
		assertThat(mapper.markAwarded(List.of(PERF), T1)).isEqualTo(1);
		jdbc.update("INSERT INTO performance_override (performance_id, field_name, override_value, created_at, updated_at)"
				+ " VALUES (?, 'title', '관리자 제목', ?, ?)", id, T1, T1);

		// 次の日の収集
		mapper.upsertPerformance(performance("스위니토드 2027", List.of()), venueId, true, T2);

		Map<String, Object> row = jdbc.queryForMap("SELECT * FROM performance WHERE id = ?", id);
		assertThat(row.get("title")).isEqualTo("스위니토드 2027");
		assertThat(row.get("is_hidden")).isEqualTo(true);
		assertThat(row.get("has_award")).isEqualTo(true);
		assertThat(row.get("collected_at")).isEqualTo(T2);
		assertThat(row.get("created_at")).isEqualTo(T1);
		// 修正の値は別の表にあり、そのまま。表示の値（ビュー）は修正の値
		assertThat(jdbc.queryForObject("SELECT override_value FROM performance_override WHERE performance_id = ?",
				String.class, id))
			.isEqualTo("관리자 제목");
		assertThat(jdbc.queryForObject("SELECT title FROM performance_effective WHERE id = ?", String.class, id))
			.isEqualTo("관리자 제목");
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM performance WHERE kopis_performance_id = ?",
				Integer.class, PERF))
			.isEqualTo(1);
	}

	@Test
	void collectedBookingSitesAreReplacedIdempotentlyWithoutTouchingAdminRows() {
		long id = savePerformance(saveVenue(), "스위니토드", T1);
		jdbc.update("INSERT INTO booking_site (performance_id, source, name, url, sort_order, created_at, updated_at)"
				+ " VALUES (?, 'ADMIN', '관리자 예매처', 'https://admin/', 1, ?, ?)", id, T1, T1);
		List<BookingSiteValue> sites = List.of(new BookingSiteValue("NHN티켓링크", "https://a/", 1),
				new BookingSiteValue(null, "https://b/", 2));

		for (int i = 0; i < 2; i++) {
			mapper.deleteBookingSites(id, "COLLECTED");
			mapper.insertBookingSites(id, "COLLECTED", sites, T1);
		}

		List<Map<String, Object>> rows = jdbc.queryForList(
				"SELECT source, name, url, sort_order FROM booking_site WHERE performance_id = ? ORDER BY source, sort_order",
				id);
		assertThat(rows).hasSize(3);
		assertThat(rows.get(0)).containsEntry("source", "COLLECTED").containsEntry("name", "NHN티켓링크");
		assertThat(rows.get(1)).containsEntry("name", null).containsEntry("url", "https://b/");
		assertThat(rows.get(2)).containsEntry("source", "ADMIN").containsEntry("name", "관리자 예매처");

		// 予約サイトがなくなったら COLLECTED の行だけ消える
		assertThat(mapper.deleteBookingSites(id, "COLLECTED")).isEqualTo(2);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM booking_site WHERE performance_id = ?", Integer.class, id))
			.isEqualTo(1);
	}

	@Test
	void markAwardedOnlySetsTheFlag() {
		savePerformance(saveVenue(), "스위니토드", T1);

		assertThat(mapper.markAwarded(List.of(PERF, "PFNOTINDB"), T2)).isEqualTo(1);
		// 立っている行は変えない（2回目は 0）
		assertThat(mapper.markAwarded(List.of(PERF), T2.plusDays(1))).isZero();
		assertThat(jdbc.queryForObject("SELECT updated_at FROM performance WHERE kopis_performance_id = ?",
				LocalDateTime.class, PERF))
			.isEqualTo(T2);
	}

	@Test
	void findExistingKopisIds() {
		savePerformance(saveVenue(), "스위니토드", T1);

		assertThat(mapper.findExistingKopisIds(List.of(PERF, "PFNOTINDB"))).containsExactly(PERF);
	}

	@Test
	void rankingCollectionReplaceIsIdempotent() {
		RankingCollectionKey key = new RankingCollectionKey(PeriodType.WEEKLY, RankingCollectionKey.ALL,
				new Period(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 22)));
		List<RankingEntryInsert> entries = List.of(new RankingEntryInsert(1, "PF306800"),
				new RankingEntryInsert(2, "PF000002"));

		replace(key, entries, T1);
		long id = mapper.findCollectionId(key).orElseThrow();
		replace(key, entries, T2);

		assertThat(mapper.findCollectionId(key)).contains(id);
		Map<String, Object> collection = jdbc.queryForMap("SELECT * FROM ranking_collection WHERE id = ?", id);
		assertThat(collection.get("period_type")).isEqualTo("WEEKLY");
		assertThat(collection.get("genre_code")).isEqualTo("ALL");
		assertThat(((java.sql.Date) collection.get("period_end")).toLocalDate()).isEqualTo(LocalDate.of(2026, 11, 22));
		assertThat(((Number) collection.get("row_count")).intValue()).isEqualTo(2);
		assertThat(collection.get("completed_at")).isEqualTo(T2);
		assertThat(collection.get("created_at")).isEqualTo(T1);
		assertThat(jdbc.queryForList("SELECT provider_rank, kopis_performance_id FROM ranking_entry"
				+ " WHERE ranking_collection_id = ? ORDER BY provider_rank", id))
			.extracting(r -> r.get("kopis_performance_id"))
			.containsExactly("PF306800", "PF000002");

		// 0 件の組も完了として記録できる
		replace(key, List.of(), T2.plusHours(1));
		assertThat(jdbc.queryForObject("SELECT row_count FROM ranking_collection WHERE id = ?", Integer.class, id))
			.isZero();
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ranking_entry WHERE ranking_collection_id = ?",
				Integer.class, id))
			.isZero();
	}

	@Test
	void rankingCollectionsAreSeparatedByGenreAndPeriod() {
		Period day = new Period(LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 23));
		replace(new RankingCollectionKey(PeriodType.DAILY, "ALL", day), List.of(new RankingEntryInsert(1, "PF1")), T1);
		replace(new RankingCollectionKey(PeriodType.DAILY, "GGGA", day), List.of(new RankingEntryInsert(1, "PF1")), T1);

		assertThat(mapper.findCollectionId(new RankingCollectionKey(PeriodType.DAILY, "ALL", day)))
			.isNotEqualTo(mapper.findCollectionId(new RankingCollectionKey(PeriodType.DAILY, "GGGA", day)));
		assertThat(mapper.findCollectionId(new RankingCollectionKey(PeriodType.DAILY, "ALL",
				new Period(LocalDate.of(2026, 11, 22), LocalDate.of(2026, 11, 22)))))
			.isEmpty();
	}

	// RankingSaveService.replace と同じ順番
	private void replace(RankingCollectionKey key, List<RankingEntryInsert> entries, LocalDateTime now) {
		mapper.upsertRankingCollection(key, entries.size(), now);
		long id = mapper.findCollectionId(key).orElseThrow();
		mapper.deleteRankingEntries(id);
		if (!entries.isEmpty()) {
			mapper.insertRankingEntries(id, entries, now);
		}
	}

	@Test
	void genreTrendReplaceIsIdempotent() {
		Period period = new Period(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 11, 23));
		Period older = new Period(LocalDate.of(2026, 10, 24), LocalDate.of(2026, 11, 22));
		List<GenreTrendRow> rows = List.of(new GenreTrendRow(Genre.AAAA, 471333, new BigDecimal("13.748521")),
				new GenreTrendRow(Genre.BBBE, 0, new BigDecimal("0.000000")));
		mapper.insertGenreTrends(older, rows, GenreTrendBasis.NATIONAL, T1);

		for (int i = 0; i < 2; i++) {
			mapper.deleteGenreTrends(period);
			mapper.insertGenreTrends(period, rows, GenreTrendBasis.NATIONAL, T2);
		}

		List<Map<String, Object>> saved = jdbc.queryForList(
				"SELECT genre_code, booking_count, share, basis FROM genre_trend WHERE period_end = ? ORDER BY genre_code",
				period.end());
		assertThat(saved).hasSize(2);
		assertThat(saved.get(0)).containsEntry("genre_code", "AAAA")
			.containsEntry("share", new BigDecimal("13.748521"))
			.containsEntry("basis", "NATIONAL");
		assertThat(((Number) saved.get(0).get("booking_count")).longValue()).isEqualTo(471333L);
		// ほかの期間の行は消さない
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM genre_trend WHERE period_end = ?", Integer.class,
				older.end()))
			.isEqualTo(2);
		assertThat(mapper.deleteGenreTrends(period)).isEqualTo(2);
	}

}
