package com.catticket.collect;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.catticket.common.batch.LastS1;
import com.catticket.common.batch.PreviousS1;
import com.catticket.common.display.Period;

/**
 * CollectWindows（05 の 3.3 の 1.〜3.）。
 */
class CollectWindowsTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

	private static Period p(String start, String end) {
		return new Period(LocalDate.parse(start), LocalDate.parse(end));
	}

	@Test
	void windowIs180DaysInSixSegmentsOf31Days() {
		List<Period> window = CollectWindows.window(TODAY);

		// 今日〜今日 + 180 日（両端を含む 181 日）
		assertThat(window).containsExactly(p("2026-11-24", "2026-12-24"), p("2026-12-25", "2027-01-24"),
				p("2027-01-25", "2027-02-24"), p("2027-02-25", "2027-03-27"), p("2027-03-28", "2027-04-27"),
				p("2027-04-28", "2027-05-23"));
		assertThat(window.get(5).end()).isEqualTo(TODAY.plusDays(180));
		// どの区間も 31 日以内（KOPIS の上限）
		assertThat(window).allSatisfy(s -> assertThat(s.start().plusDays(30)).isAfterOrEqualTo(s.end()));
	}

	@Test
	void firstRunHasNoAfterDate() {
		CollectPlan plan = CollectWindows.plan(TODAY, Optional.empty());

		assertThat(plan.ranges()).hasSize(6).allSatisfy(r -> assertThat(r.afterDate()).isNull());
		assertThat(plan.ranges()).extracting(CollectRange::period).isEqualTo(CollectWindows.window(TODAY));
		assertThat(plan.windowEnd()).isEqualTo(LocalDate.of(2027, 5, 23));
	}

	@Test
	void laterRunUsesDayBeforeLastSuccessAndQueriesNewDaysWithoutAfterDate() {
		// 前回: 昨日成功し、窓の末日は昨日 + 180 日
		LastS1 last = new LastS1(TODAY.minusDays(1), TODAY.minusDays(1).plusDays(180));

		CollectPlan plan = CollectWindows.plan(TODAY, Optional.of(last));

		// (a) 6 区間 × afterdate = 前回の成功の日の前日
		assertThat(plan.ranges().subList(0, 6)).allSatisfy(r -> assertThat(r.afterDate()).isEqualTo(TODAY.minusDays(2)));
		assertThat(plan.ranges().subList(0, 6)).extracting(CollectRange::period).isEqualTo(CollectWindows.window(TODAY));
		// (b) 新しく窓に入った 1 日だけを afterdate なしで
		assertThat(plan.ranges().subList(6, plan.ranges().size()))
			.containsExactly(new CollectRange(p("2027-05-23", "2027-05-23"), null));
		assertThat(plan.windowEnd()).isEqualTo(LocalDate.of(2027, 5, 23));
	}

	@Test
	void newDaysAfterAGapAreSplitInto31Days() {
		// 前回の成功は 40 日前（窓の末日はその 180 日後）
		LocalDate lastDay = TODAY.minusDays(40);
		CollectPlan plan = CollectWindows.plan(TODAY, Optional.of(new LastS1(lastDay, lastDay.plusDays(180))));

		List<CollectRange> newDays = plan.ranges().subList(6, plan.ranges().size());
		assertThat(newDays).containsExactly(new CollectRange(p("2027-04-14", "2027-05-14"), null),
				new CollectRange(p("2027-05-15", "2027-05-23"), null));
		assertThat(plan.ranges().get(0).afterDate()).isEqualTo(lastDay.minusDays(1));
	}

	@Test
	void sameDayRerunHasNoNewDays() {
		// 同じ日にもう一度実行（窓の末日は今回と同じ）
		CollectPlan plan = CollectWindows.plan(TODAY, Optional.of(new LastS1(TODAY, TODAY.plusDays(180))));

		assertThat(plan.ranges()).hasSize(6).allSatisfy(r -> assertThat(r.afterDate()).isEqualTo(TODAY.minusDays(1)));
	}

	@Test
	void newDaysStartNoEarlierThanToday() {
		// 窓の末日が今日より前（とても長く止まっていた）。窓の外（今日より前）は照会しない
		LocalDate lastDay = TODAY.minusDays(400);
		CollectPlan plan = CollectWindows.plan(TODAY, Optional.of(new LastS1(lastDay, lastDay.plusDays(180))));

		List<CollectRange> newDays = plan.ranges().subList(6, plan.ranges().size());
		assertThat(newDays).extracting(CollectRange::period).isEqualTo(CollectWindows.window(TODAY));
		assertThat(newDays).allSatisfy(r -> assertThat(r.afterDate()).isNull());
	}

	@Test
	void missingWindowEndIsTreatedAsFirstRun() {
		CollectPlan plan = CollectWindows.plan(TODAY, Optional.of(new LastS1(TODAY.minusDays(1), null)));

		assertThat(plan.ranges()).hasSize(6).allSatisfy(r -> assertThat(r.afterDate()).isNull());
	}

	@Test
	void segmentsOfEmptyRangeAreEmpty() {
		assertThat(CollectWindows.segments(TODAY, TODAY.minusDays(1))).isEmpty();
		assertThat(CollectWindows.segments(TODAY, TODAY)).containsExactly(new Period(TODAY, TODAY));
		assertThat(CollectWindows.segments(TODAY, TODAY.plusDays(30))).containsExactly(new Period(TODAY, TODAY.plusDays(30)));
		assertThat(CollectWindows.segments(TODAY, TODAY.plusDays(31))).hasSize(2);
	}

	@Test
	void baseDateIsTodayUnlessThePreviousS1DidNotSucceed() {
		LocalDate earlier = TODAY.minusDays(2);

		// 前がない・成功・基準日がない・基準日が今日より後 → 今日
		assertThat(CollectWindows.baseDate(TODAY, Optional.empty())).isEqualTo(TODAY);
		assertThat(CollectWindows.baseDate(TODAY, Optional.of(new PreviousS1("SUCCESS", earlier)))).isEqualTo(TODAY);
		assertThat(CollectWindows.baseDate(TODAY, Optional.of(new PreviousS1("FAILURE", null)))).isEqualTo(TODAY);
		assertThat(CollectWindows.baseDate(TODAY, Optional.of(new PreviousS1("FAILURE", TODAY.plusDays(1)))))
			.isEqualTo(TODAY);
	}

	@Test
	void baseDateIsInheritedFromAFailedOrUnfinishedS1() {
		LocalDate earlier = TODAY.minusDays(2);

		assertThat(CollectWindows.baseDate(TODAY, Optional.of(new PreviousS1("FAILURE", earlier)))).isEqualTo(earlier);
		// 終わりが記録されていない（result が null）
		assertThat(CollectWindows.baseDate(TODAY, Optional.of(new PreviousS1(null, earlier)))).isEqualTo(earlier);
		// 今日と同じ日も引き継ぐ（同じ日の2回目の実行）
		assertThat(CollectWindows.baseDate(TODAY, Optional.of(new PreviousS1("FAILURE", TODAY)))).isEqualTo(TODAY);
	}

}
