package com.catticket.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.catticket.common.code.PeriodType;
import com.catticket.common.display.Period;

/**
 * RankingPeriods.collectTargets（06 の 4.9 の「RankingCollectStep の期間」）。月・年の変わり目と月曜日を確かめる。
 */
class RankingPeriodsTest {

	private static Period p(String start, String end) {
		return new Period(LocalDate.parse(start), LocalDate.parse(end));
	}

	@Test
	void sixTargetsInOrder() {
		// 2026-11-24 は火曜日
		List<RankingTarget> targets = RankingPeriods.collectTargets(LocalDate.of(2026, 11, 24));

		assertThat(targets).containsExactly(
				new RankingTarget(PeriodType.DAILY, p("2026-11-23", "2026-11-23")),
				new RankingTarget(PeriodType.DAILY, p("2026-11-22", "2026-11-22")),
				new RankingTarget(PeriodType.WEEKLY, p("2026-11-16", "2026-11-22")),
				new RankingTarget(PeriodType.WEEKLY, p("2026-11-09", "2026-11-15")),
				new RankingTarget(PeriodType.MONTHLY, p("2026-10-01", "2026-10-31")),
				new RankingTarget(PeriodType.MONTHLY, p("2026-09-01", "2026-09-30")));
	}

	@Test
	void onMondayLastWeekEndsYesterday() {
		// 2026-11-23 は月曜日。今週はまだ終わっていないので、先週は 11-16〜11-22
		List<RankingTarget> targets = RankingPeriods.collectTargets(LocalDate.of(2026, 11, 23));

		assertThat(targets.get(2).period()).isEqualTo(p("2026-11-16", "2026-11-22"));
		assertThat(targets.get(3).period()).isEqualTo(p("2026-11-09", "2026-11-15"));
	}

	@Test
	void onSundayLastWeekIsTheWeekBefore() {
		// 2026-11-22 は日曜日。今週（11-16〜）は今日まで続くので、先週は 11-09〜11-15
		List<RankingTarget> targets = RankingPeriods.collectTargets(LocalDate.of(2026, 11, 22));

		assertThat(targets.get(2).period()).isEqualTo(p("2026-11-09", "2026-11-15"));
	}

	@Test
	void firstDayOfMonth() {
		// 2026-03-01: 前日は 2 月の末日（平年）、先月は 2 月、先々月は 1 月
		List<RankingTarget> targets = RankingPeriods.collectTargets(LocalDate.of(2026, 3, 1));

		assertThat(targets.get(0).period()).isEqualTo(p("2026-02-28", "2026-02-28"));
		assertThat(targets.get(1).period()).isEqualTo(p("2026-02-27", "2026-02-27"));
		assertThat(targets.get(4).period()).isEqualTo(p("2026-02-01", "2026-02-28"));
		assertThat(targets.get(5).period()).isEqualTo(p("2026-01-01", "2026-01-31"));
	}

	@Test
	void yearBoundary() {
		// 2027-01-01 は金曜日。週は年をまたぐ
		List<RankingTarget> targets = RankingPeriods.collectTargets(LocalDate.of(2027, 1, 1));

		assertThat(targets.get(0).period()).isEqualTo(p("2026-12-31", "2026-12-31"));
		assertThat(targets.get(2).period()).isEqualTo(p("2026-12-21", "2026-12-27"));
		assertThat(targets.get(3).period()).isEqualTo(p("2026-12-14", "2026-12-20"));
		assertThat(targets.get(4).period()).isEqualTo(p("2026-12-01", "2026-12-31"));
		assertThat(targets.get(5).period()).isEqualTo(p("2026-11-01", "2026-11-30"));
	}

	@Test
	void leapYearFebruary() {
		assertThat(RankingPeriods.collectTargets(LocalDate.of(2028, 3, 15)).get(4).period())
			.isEqualTo(p("2028-02-01", "2028-02-29"));
	}

}
