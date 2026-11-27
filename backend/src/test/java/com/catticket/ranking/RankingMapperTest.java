package com.catticket.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import com.catticket.common.code.PeriodType;
import com.catticket.common.display.Period;
import com.catticket.ranking.row.CollectionRow;
import com.catticket.ranking.row.RankingEntryRow;

/**
 * RankingMapper（実際の MySQL のテスト用 DB）。各テストはロールバックされる。
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RankingMapperTest {

	/** 2026-11-27 は金曜日 */
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 27);

	@Autowired
	RankingMapper rankingMapper;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void latestCompletedIsMostRecentEndedPeriod() {
		insertCollection("DAILY", "ALL", "2026-11-24", "2026-11-24");
		long d25 = insertCollection("DAILY", "ALL", "2026-11-25", "2026-11-25");
		// 今日の期間はまだ終わっていない（末日 = 今日は対象外）
		insertCollection("DAILY", "ALL", "2026-11-27", "2026-11-27");
		// 種類・ジャンルの違う組は見ない
		insertCollection("DAILY", "GGGA", "2026-11-26", "2026-11-26");
		insertCollection("WEEKLY", "ALL", "2026-11-16", "2026-11-22");

		Optional<CollectionRow> row = rankingMapper.findLatestCompleted(PeriodType.DAILY, "ALL", TODAY);

		// 前日（11-26）の ALL は未収集なので 1つ前の 11-25
		assertThat(row).contains(new CollectionRow(d25, LocalDate.of(2026, 11, 25), LocalDate.of(2026, 11, 25)));
		assertThat(row.get().period()).isEqualTo(new Period(LocalDate.of(2026, 11, 25), LocalDate.of(2026, 11, 25)));
	}

	@Test
	void latestCompletedByTypeAndGenre() {
		long w = insertCollection("WEEKLY", "GGGA", "2026-11-16", "2026-11-22");
		insertCollection("WEEKLY", "GGGA", "2026-11-09", "2026-11-15");
		// 今週（11-23〜29）は終わっていない
		insertCollection("WEEKLY", "GGGA", "2026-11-23", "2026-11-29");
		long m = insertCollection("MONTHLY", "ALL", "2026-10-01", "2026-10-31");
		insertCollection("MONTHLY", "ALL", "2026-11-01", "2026-11-30");

		assertThat(rankingMapper.findLatestCompleted(PeriodType.WEEKLY, "GGGA", TODAY)).get()
			.extracting(CollectionRow::id)
			.isEqualTo(w);
		assertThat(rankingMapper.findLatestCompleted(PeriodType.MONTHLY, "ALL", TODAY)).get()
			.extracting(CollectionRow::id)
			.isEqualTo(m);
		assertThat(rankingMapper.findLatestCompleted(PeriodType.MONTHLY, "GGGA", TODAY)).isEmpty();
	}

	@Test
	void latestEndedPeriodIsUsedWhenItsCollectionIsComplete() {
		// 直近の終わった期間（前日・先週（月〜日）・先月）が収集済みなら、その期間（03 の 4.9）
		insertCollection("DAILY", "ALL", "2026-11-25", "2026-11-25");
		long day = insertCollection("DAILY", "ALL", "2026-11-26", "2026-11-26");
		insertCollection("WEEKLY", "ALL", "2026-11-09", "2026-11-15");
		long week = insertCollection("WEEKLY", "ALL", "2026-11-16", "2026-11-22");
		insertCollection("MONTHLY", "ALL", "2026-09-01", "2026-09-30");
		long month = insertCollection("MONTHLY", "ALL", "2026-10-01", "2026-10-31");

		assertThat(rankingMapper.findLatestCompleted(PeriodType.DAILY, "ALL", TODAY)).contains(
				new CollectionRow(day, LocalDate.of(2026, 11, 26), LocalDate.of(2026, 11, 26)));
		assertThat(rankingMapper.findLatestCompleted(PeriodType.WEEKLY, "ALL", TODAY)).contains(
				new CollectionRow(week, LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 22)));
		assertThat(rankingMapper.findLatestCompleted(PeriodType.MONTHLY, "ALL", TODAY)).contains(
				new CollectionRow(month, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)));
	}

	@Test
	void previousPeriodIsUsedWhileLatestEndedPeriodIsNotCollected() {
		// 先週（11-16〜22）・先月（10 月）が未収集なら、その1つ前（03 の 4.9、R-62）
		long week = insertCollection("WEEKLY", "ALL", "2026-11-09", "2026-11-15");
		long month = insertCollection("MONTHLY", "ALL", "2026-09-01", "2026-09-30");

		assertThat(rankingMapper.findLatestCompleted(PeriodType.WEEKLY, "ALL", TODAY)).get()
			.extracting(CollectionRow::id)
			.isEqualTo(week);
		assertThat(rankingMapper.findLatestCompleted(PeriodType.MONTHLY, "ALL", TODAY)).get()
			.extracting(CollectionRow::id)
			.isEqualTo(month);
	}

	@Test
	void periodBoundariesOnMondayAndFirstOfMonth() {
		// 2026-12-01（火）: 11 月は前日に終わったので対象。今週（11-30〜12-06）は終わっていない
		LocalDate dec1 = LocalDate.of(2026, 12, 1);
		long nov = insertCollection("MONTHLY", "ALL", "2026-11-01", "2026-11-30");
		long lastWeek = insertCollection("WEEKLY", "ALL", "2026-11-23", "2026-11-29");
		insertCollection("WEEKLY", "ALL", "2026-11-30", "2026-12-06");
		assertThat(rankingMapper.findLatestCompleted(PeriodType.MONTHLY, "ALL", dec1)).get()
			.extracting(CollectionRow::id)
			.isEqualTo(nov);
		assertThat(rankingMapper.findLatestCompleted(PeriodType.WEEKLY, "ALL", dec1)).get()
			.extracting(CollectionRow::id)
			.isEqualTo(lastWeek);

		// 2026-11-30（月）: 先週の日曜（11-29）が末日の週は対象。11 月はまだ終わっていない
		LocalDate nov30 = LocalDate.of(2026, 11, 30);
		assertThat(rankingMapper.findLatestCompleted(PeriodType.WEEKLY, "ALL", nov30)).get()
			.extracting(CollectionRow::id)
			.isEqualTo(lastWeek);
		assertThat(rankingMapper.findLatestCompleted(PeriodType.MONTHLY, "ALL", nov30)).isEmpty();
	}

	@Test
	void topVisibleExcludesHiddenAndUnlinkedRows() {
		jdbcTemplate.update("INSERT INTO venue (kopis_venue_id, name) VALUES ('FCRK01', '샤롯데씨어터')");
		long venueId = jdbcTemplate.queryForObject("SELECT id FROM venue WHERE kopis_venue_id = 'FCRK01'",
				Long.class);
		long p1 = insertPerformance("PFRK01", venueId, false);
		insertPerformance("PFRK02", venueId, true); // 非表示
		insertPerformance("PFRK04", venueId, false);
		insertPerformance("PFRK05", venueId, false);
		// 終わった公演も出る（03 の 1.6 の「表示できる公演」）
		jdbcTemplate.update("UPDATE performance SET end_date = '2026-11-01' WHERE kopis_performance_id = 'PFRK05'");
		jdbcTemplate.update("INSERT INTO performance_override (performance_id, field_name, override_value)"
				+ " VALUES (?, 'title', '修正した名前')", p1);
		long c = insertCollection("DAILY", "ALL", "2026-11-26", "2026-11-26");
		insertEntry(c, 1, "PFRK01");
		insertEntry(c, 2, "PFRK02");
		insertEntry(c, 3, "PFRK03"); // 公演の行がない
		// 同じ順位は公演 ID の順
		insertEntry(c, 4, "PFRK05");
		insertEntry(c, 4, "PFRK04");
		long other = insertCollection("DAILY", "ALL", "2026-11-25", "2026-11-25");
		insertEntry(other, 1, "PFRK04");

		List<RankingEntryRow> rows = rankingMapper.findTopVisible(c, 10);

		assertThat(rows).extracting(RankingEntryRow::performanceId).containsExactly("PFRK01", "PFRK04", "PFRK05");
		assertThat(rows).extracting(RankingEntryRow::providerRank).containsExactly(1, 4, 4);
		RankingEntryRow first = rows.get(0);
		assertThat(first.id()).isEqualTo(p1);
		assertThat(first.title()).isEqualTo("修正した名前");
		assertThat(first.startDate()).isEqualTo(LocalDate.of(2026, 11, 1));
		assertThat(first.endDate()).isEqualTo(LocalDate.of(2026, 12, 31));
		assertThat(first.genreCode()).isEqualTo("GGGA");
		assertThat(first.venueId()).isEqualTo("FCRK01");
		assertThat(first.venueName()).isEqualTo("샤롯데씨어터");
		assertThat(first.posterUrl()).isEqualTo("https://example.com/PFRK01.jpg");
		assertThat(first.hasAward()).isTrue();
		assertThat(first.toSummaryRow().performanceId()).isEqualTo("PFRK01");

		assertThat(rankingMapper.findTopVisible(c, 2)).extracting(RankingEntryRow::performanceId)
			.containsExactly("PFRK01", "PFRK04");
	}

	private long insertCollection(String type, String genre, String start, String end) {
		jdbcTemplate.update("INSERT INTO ranking_collection (period_type, genre_code, period_start, period_end,"
				+ " row_count, completed_at) VALUES (?, ?, ?, ?, 0, '2026-11-27 06:10:00')", type, genre, start, end);
		return jdbcTemplate.queryForObject("SELECT id FROM ranking_collection WHERE period_type = ? AND genre_code = ?"
				+ " AND period_start = ?", Long.class, type, genre, start);
	}

	private void insertEntry(long collectionId, int rank, String kopisId) {
		jdbcTemplate.update("INSERT INTO ranking_entry (ranking_collection_id, provider_rank, kopis_performance_id)"
				+ " VALUES (?, ?, ?)", collectionId, rank, kopisId);
	}

	private long insertPerformance(String kopisId, long venueId, boolean hidden) {
		jdbcTemplate.update(
				"INSERT INTO performance (kopis_performance_id, venue_id, title, start_date, end_date, genre_code,"
						+ " poster_url, has_award, is_seoul, is_hidden, collected_at)"
						+ " VALUES (?, ?, ?, '2026-11-01', '2026-12-31', 'GGGA', ?, TRUE, TRUE, ?, '2026-11-10 06:00:00')",
				kopisId, venueId, "공연 " + kopisId, "https://example.com/" + kopisId + ".jpg", hidden);
		return jdbcTemplate.queryForObject("SELECT id FROM performance WHERE kopis_performance_id = ?", Long.class,
				kopisId);
	}

}
