package com.catticket.ranking;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import com.catticket.common.code.PeriodType;
import com.catticket.common.display.Period;

/**
 * 人気順位の期間の暦の計算（03 の 4.9、ADR 0008 3.）。Spring に依存しない。
 * 日次は暦の日、週次は月〜日、月次は暦の月（Asia/Seoul の日付で渡す）。
 */
public final class RankingPeriods {

	private RankingPeriods() {
	}

	/**
	 * S2 が照会する 6 つの期間（06 の 4.9）。今日から見た現在の期間（直近の終わった期間）と直前の期間を、
	 * 日次（前日・前々日）→ 週次（先週・その前の週）→ 月次（先月・先々月）の順で返す。
	 */
	public static List<RankingTarget> collectTargets(LocalDate today) {
		LocalDate yesterday = today.minusDays(1);
		LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate lastWeek = thisMonday.minusWeeks(1);
		LocalDate thisMonth = today.withDayOfMonth(1);
		LocalDate lastMonth = thisMonth.minusMonths(1);
		return List.of(new RankingTarget(PeriodType.DAILY, day(yesterday)),
				new RankingTarget(PeriodType.DAILY, day(yesterday.minusDays(1))),
				new RankingTarget(PeriodType.WEEKLY, week(lastWeek)),
				new RankingTarget(PeriodType.WEEKLY, week(lastWeek.minusWeeks(1))),
				new RankingTarget(PeriodType.MONTHLY, month(lastMonth)),
				new RankingTarget(PeriodType.MONTHLY, month(lastMonth.minusMonths(1))));
	}

	private static Period day(LocalDate date) {
		return new Period(date, date);
	}

	private static Period week(LocalDate monday) {
		return new Period(monday, monday.plusDays(6));
	}

	private static Period month(LocalDate first) {
		return new Period(first, first.with(TemporalAdjusters.lastDayOfMonth()));
	}

}
