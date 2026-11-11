package com.catticket.common.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import com.catticket.common.display.PerformanceSummaryRow;

/**
 * mapper/common/SharedSql.xml が読み込まれ、ビュー performance_effective に対して動くこと。
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SharedSqlMybatisTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

	@Autowired
	SqlSessionFactory sqlSessionFactory;

	@Autowired
	SharedSqlTestMapper mapper;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void fragmentsAreLoaded() {
		var fragments = sqlSessionFactory.getConfiguration().getSqlFragments();
		assertThat(fragments).containsKeys("com.catticket.common.db.SharedSql.publicCondition",
				"com.catticket.common.db.SharedSql.summaryColumns");
	}

	@Test
	void publicConditionAndSummaryColumns() {
		jdbcTemplate.update("INSERT INTO venue (kopis_venue_id, name) VALUES ('FCTEST01', '샤롯데씨어터')");
		long venueId = jdbcTemplate.queryForObject("SELECT id FROM venue WHERE kopis_venue_id = 'FCTEST01'",
				Long.class);
		insert("PFT01", venueId, TODAY.minusDays(10), TODAY.plusDays(10), true, false, true);
		insert("PFT02", venueId, TODAY.minusDays(10), TODAY.plusDays(10), true, true, false); // 非表示
		insert("PFT03", venueId, TODAY.minusDays(10), TODAY.minusDays(1), true, false, false); // 終了
		insert("PFT04", venueId, TODAY.minusDays(10), TODAY.plusDays(10), false, false, false); // ソウルでない
		insert("PFT05", venueId, TODAY.plusDays(180), TODAY.plusDays(200), true, false, false); // 180 日後は含む
		insert("PFT06", venueId, TODAY.plusDays(181), TODAY.plusDays(200), true, false, false); // 181 日後は含まない
		insert("PFT07", venueId, TODAY, TODAY, true, false, false); // 今日だけの公演
		long pft01 = jdbcTemplate.queryForObject(
				"SELECT id FROM performance WHERE kopis_performance_id = 'PFT01'", Long.class);
		// 管理者の修正を反映した値が出る（04 の 11.3）
		jdbcTemplate.update("INSERT INTO performance_override (performance_id, field_name, override_value)"
				+ " VALUES (?, 'title', '修正した名前')", pft01);

		List<PerformanceSummaryRow> rows = mapper.findPublicSummaries(TODAY).stream()
			.filter(r -> r.performanceId().startsWith("PFT"))
			.toList();

		assertThat(rows).extracting(PerformanceSummaryRow::performanceId).containsExactly("PFT01", "PFT07", "PFT05");
		PerformanceSummaryRow first = rows.get(0);
		assertThat(first.id()).isEqualTo(pft01);
		assertThat(first.title()).isEqualTo("修正した名前");
		assertThat(first.startDate()).isEqualTo(TODAY.minusDays(10));
		assertThat(first.endDate()).isEqualTo(TODAY.plusDays(10));
		assertThat(first.genreCode()).isEqualTo("GGGA");
		assertThat(first.venueId()).isEqualTo("FCTEST01");
		assertThat(first.venueName()).isEqualTo("샤롯데씨어터");
		assertThat(first.posterUrl()).isEqualTo("https://example.com/PFT01.jpg");
		assertThat(first.hasAward()).isTrue();
		assertThat(rows.get(1).hasAward()).isFalse();
	}

	private void insert(String kopisId, long venueId, LocalDate start, LocalDate end, boolean seoul, boolean hidden,
			boolean award) {
		jdbcTemplate.update(
				"INSERT INTO performance (kopis_performance_id, venue_id, title, start_date, end_date, genre_code,"
						+ " poster_url, has_award, is_seoul, is_hidden, collected_at)"
						+ " VALUES (?, ?, ?, ?, ?, 'GGGA', ?, ?, ?, ?, '2026-11-10 06:00:00')",
				kopisId, venueId, "공연 " + kopisId, start, end, "https://example.com/" + kopisId + ".jpg", award, seoul,
				hidden);
	}

}
