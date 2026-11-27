package com.catticket.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import com.catticket.review.row.RatingStatRow;

/**
 * ReviewMapper（実際の MySQL のテスト用 DB）。各テストはロールバックされる。
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReviewMapperTest {

	@Autowired
	ReviewMapper reviewMapper;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void sumAndCountGroupsByPerformance() {
		jdbcTemplate.update("INSERT INTO venue (kopis_venue_id, name) VALUES ('FCRV01', '극장')");
		long venueId = id("SELECT id FROM venue WHERE kopis_venue_id = 'FCRV01'");
		long p1 = insertPerformance("PFRV01", venueId);
		long p2 = insertPerformance("PFRV02", venueId);
		long p3 = insertPerformance("PFRV03", venueId); // レビューなし
		long p4 = insertPerformance("PFRV04", venueId); // 引数に入れない
		long u1 = insertUser("a@example.com", "A");
		long u2 = insertUser("b@example.com", "B");
		long u3 = insertUser("c@example.com", "C");
		insertReview(u1, p1, 5);
		insertReview(u2, p1, 4);
		insertReview(u3, p1, 4);
		insertReview(u1, p2, 1);
		insertReview(u1, p4, 3);

		List<RatingStatRow> rows = reviewMapper.sumAndCount(List.of(p1, p2, p3));

		assertThat(rows).containsExactlyInAnyOrder(new RatingStatRow(p1, 13, 3), new RatingStatRow(p2, 1, 1));
	}

	private long id(String sql) {
		return jdbcTemplate.queryForObject(sql, Long.class);
	}

	private long insertPerformance(String kopisId, long venueId) {
		jdbcTemplate.update(
				"INSERT INTO performance (kopis_performance_id, venue_id, title, start_date, end_date, genre_code,"
						+ " is_seoul, collected_at)"
						+ " VALUES (?, ?, ?, '2026-11-01', '2026-12-31', 'GGGA', TRUE, '2026-11-10 06:00:00')",
				kopisId, venueId, "공연 " + kopisId);
		return id("SELECT id FROM performance WHERE kopis_performance_id = '" + kopisId + "'");
	}

	private long insertUser(String email, String nickname) {
		jdbcTemplate.update("INSERT INTO app_user (email, password_hash, nickname, role, status, created_at, updated_at)"
				+ " VALUES (?, '{bcrypt}x', ?, 'USER', 'ACTIVE', '2026-11-10 10:00:00', '2026-11-10 10:00:00')", email,
				nickname);
		return id("SELECT id FROM app_user WHERE email = '" + email + "'");
	}

	private void insertReview(long userId, long performanceId, int rating) {
		jdbcTemplate.update("INSERT INTO review (user_id, performance_id, rating, created_at, updated_at)"
				+ " VALUES (?, ?, ?, '2026-11-10 10:00:00', '2026-11-10 10:00:00')", userId, performanceId, rating);
	}

}
