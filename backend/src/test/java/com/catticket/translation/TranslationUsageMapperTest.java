package com.catticket.translation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * TranslationUsageMapper（実際の MySQL のテスト用 DB）。各テストはロールバックされる。
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TranslationUsageMapperTest {

	// ほかのテストの月とぶつからない月
	private static final LocalDate MONTH = LocalDate.of(2099, 1, 1);

	private static final LocalDateTime T1 = LocalDateTime.of(2026, 11, 24, 6, 0, 0);

	@Autowired
	TranslationUsageMapper mapper;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void monthWithoutRowIsZero() {
		assertThat(mapper.findChars(MONTH)).isZero();
	}

	@Test
	void usageIsAddedToTheSameMonthRow() {
		mapper.addUsage(MONTH, 1200, T1);
		mapper.addUsage(MONTH, 300, T1.plusMinutes(1));

		assertThat(mapper.findChars(MONTH)).isEqualTo(1500);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM translation_usage WHERE usage_month = ?", Long.class,
				MONTH)).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT updated_at FROM translation_usage WHERE usage_month = ?",
				LocalDateTime.class, MONTH)).isEqualTo(T1.plusMinutes(1));
	}

	@Test
	void monthsAreSeparate() {
		mapper.addUsage(MONTH, 100, T1);
		mapper.addUsage(MONTH.plusMonths(1), 7, T1);

		assertThat(mapper.findChars(MONTH)).isEqualTo(100);
		assertThat(mapper.findChars(MONTH.plusMonths(1))).isEqualTo(7);
	}

}
