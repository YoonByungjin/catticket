package com.catticket.ranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.catticket.common.code.Lang;
import com.catticket.common.code.PeriodType;
import com.catticket.common.display.LocalizedText;
import com.catticket.common.display.Period;
import com.catticket.common.display.PerformanceStatus;
import com.catticket.common.display.PerformanceSummary;
import com.catticket.common.display.PerformanceSummaryRow;
import com.catticket.common.display.VenueRef;
import com.catticket.common.time.DateTimes;
import com.catticket.ranking.dto.RankingBadge;
import com.catticket.ranking.dto.RankingItem;
import com.catticket.ranking.dto.RankingResponse;
import com.catticket.ranking.row.CollectionRow;
import com.catticket.ranking.row.RankingEntryRow;
import com.catticket.summary.PerformanceSummaryAssembler;

/**
 * RankingService.getRanking・findRankingsOf（06 の 4.4、03 の API-20・4.3・4.9・9.1）。
 * 表示する期間の選び方は SQL（findLatestCompleted）なので RankingMapperTest で確かめる。
 */
class RankingServiceTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 27);

	private static final Period DAY = new Period(LocalDate.of(2026, 11, 26), LocalDate.of(2026, 11, 26));

	private static final Period WEEK = new Period(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 22));

	private static final Period MONTH = new Period(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

	private final RankingMapper rankingMapper = mock(RankingMapper.class);

	private final PerformanceSummaryAssembler assembler = mock(PerformanceSummaryAssembler.class);

	/** 2026-11-27 12:00（Asia/Seoul） */
	private final Clock clock = Clock.fixed(TODAY.atTime(12, 0).atZone(DateTimes.SEOUL).toInstant(),
			DateTimes.SEOUL);

	private final RankingService service = new RankingService(rankingMapper, assembler, clock);

	@Test
	void getRankingRenumbersVisibleRowsAndAssemblesCardsWithoutRating() {
		// SQL が除いた後の行（提供元の 2, 3, 3, 6 位。1・4・5 位は非表示か公演の行がない）
		when(rankingMapper.findLatestCompleted(PeriodType.WEEKLY, "GGGA", TODAY))
			.thenReturn(Optional.of(new CollectionRow(21, WEEK.start(), WEEK.end())));
		List<RankingEntryRow> rows = List.of(entry(2, "PF2"), entry(3, "PF3"), entry(3, "PF4"), entry(6, "PF6"));
		when(rankingMapper.findTopVisible(21, 10)).thenReturn(rows);
		List<PerformanceSummaryRow> summaryRows = rows.stream().map(RankingEntryRow::toSummaryRow).toList();
		List<PerformanceSummary> cards = summaryRows.stream().map(RankingServiceTest::card).toList();
		when(assembler.assemble(summaryRows, Lang.JA, TODAY, false)).thenReturn(cards);

		RankingResponse response = service.getRanking(PeriodType.WEEKLY, "GGGA", Lang.JA);

		assertThat(response.periodType()).isEqualTo(PeriodType.WEEKLY);
		assertThat(response.genre()).isEqualTo("GGGA");
		assertThat(response.period()).isEqualTo(WEEK);
		assertThat(response.items()).extracting(RankingItem::rank).containsExactly(1, 2, 3, 4);
		assertThat(response.items()).extracting(i -> i.performance().performanceId())
			.containsExactly("PF2", "PF3", "PF4", "PF6");
		// 「今日」は Clock から（06 の 3.1）、評価は計算しない（03 の 7.2）
		verify(rankingMapper).findLatestCompleted(PeriodType.WEEKLY, "GGGA", TODAY);
		verify(assembler).assemble(summaryRows, Lang.JA, TODAY, false);
	}

	@Test
	void getRankingWithoutDisplayablePeriodReturnsNullPeriodAndEmptyItems() {
		when(rankingMapper.findLatestCompleted(PeriodType.MONTHLY, "ALL", TODAY)).thenReturn(Optional.empty());

		RankingResponse response = service.getRanking(PeriodType.MONTHLY, "ALL", Lang.KO);

		assertThat(response).isEqualTo(new RankingResponse(PeriodType.MONTHLY, "ALL", null, List.of()));
		verify(rankingMapper, never()).findTopVisible(anyLong(), anyInt());
		verify(assembler, never()).assemble(any(), any(), any(), eq(false));
	}

	@Test
	void getRankingWithZeroRowsPeriodKeepsPeriod() {
		// 0 件でも収集が終わった期間（03 の 4.9）。period は出し、items は []
		when(rankingMapper.findLatestCompleted(PeriodType.DAILY, "ALL", TODAY))
			.thenReturn(Optional.of(new CollectionRow(11, DAY.start(), DAY.end())));
		when(rankingMapper.findTopVisible(11, 10)).thenReturn(List.of());
		when(assembler.assemble(List.of(), Lang.KO, TODAY, false)).thenReturn(List.of());

		RankingResponse response = service.getRanking(PeriodType.DAILY, "ALL", Lang.KO);

		assertThat(response).isEqualTo(new RankingResponse(PeriodType.DAILY, "ALL", DAY, List.of()));
	}

	@Test
	void returnsOnlyCombinationsContainingThePerformanceInOrder() {
		collection(PeriodType.DAILY, "ALL", 11, DAY, "PF1", "PF2", "PF3");
		collection(PeriodType.DAILY, "GGGA", 12, DAY, "PF9");
		collection(PeriodType.WEEKLY, "ALL", 21, WEEK, "PF3");
		// WEEKLY・GGGA は表示できる期間がない
		when(rankingMapper.findLatestCompleted(PeriodType.WEEKLY, "GGGA", TODAY)).thenReturn(Optional.empty());
		collection(PeriodType.MONTHLY, "ALL", 31, MONTH);
		collection(PeriodType.MONTHLY, "GGGA", 32, MONTH, "PF1", "PF3");

		List<RankingBadge> badges = service.findRankingsOf("PF3", "GGGA", TODAY);

		assertThat(badges).containsExactly(new RankingBadge(PeriodType.DAILY, "ALL", 3, DAY),
				new RankingBadge(PeriodType.WEEKLY, "ALL", 1, WEEK),
				new RankingBadge(PeriodType.MONTHLY, "GGGA", 2, MONTH));
	}

	@Test
	void ranksAreRenumberedFromOneAfterExcludedRows() {
		// SQL が除いた後の行（提供元の 1, 2, 4, 4, 7 位）。並んだ順に 1〜5 に振り直す（03 の 9.1）
		when(rankingMapper.findLatestCompleted(any(), anyString(), any())).thenReturn(Optional.empty());
		when(rankingMapper.findLatestCompleted(PeriodType.DAILY, "ALL", TODAY))
			.thenReturn(Optional.of(new CollectionRow(11, DAY.start(), DAY.end())));
		when(rankingMapper.findTopVisible(11, 10)).thenReturn(List.of(entry(1, "PF1"), entry(2, "PF2"),
				entry(4, "PF4"), entry(4, "PF5"), entry(7, "PF7")));

		assertThat(service.findRankingsOf("PF5", "AAAA", TODAY))
			.containsExactly(new RankingBadge(PeriodType.DAILY, "ALL", 4, DAY));
		assertThat(service.findRankingsOf("PF7", "AAAA", TODAY))
			.containsExactly(new RankingBadge(PeriodType.DAILY, "ALL", 5, DAY));
	}

	@Test
	void visibleTopAsksForTopTenOfLatestCompletedPeriod() {
		when(rankingMapper.findLatestCompleted(PeriodType.WEEKLY, "ALL", TODAY))
			.thenReturn(Optional.of(new CollectionRow(21, WEEK.start(), WEEK.end())));
		when(rankingMapper.findTopVisible(21, 10)).thenReturn(List.of(entry(3, "PF3"), entry(5, "PF5")));

		Optional<VisibleTop> top = service.findVisibleTop(PeriodType.WEEKLY, "ALL", TODAY);

		assertThat(top).isPresent();
		assertThat(top.get().period()).isEqualTo(WEEK);
		assertThat(top.get().entries()).extracting(VisibleTop.Ranked::rank).containsExactly(1, 2);
		assertThat(top.get().entries()).extracting(e -> e.row().performanceId()).containsExactly("PF3", "PF5");
		verify(rankingMapper).findTopVisible(21, 10);
	}

	@Test
	void noDisplayablePeriodGivesNoBadges() {
		when(rankingMapper.findLatestCompleted(any(), anyString(), any())).thenReturn(Optional.empty());

		assertThat(service.findRankingsOf("PF1", "GGGA", TODAY)).isEmpty();
		assertThat(service.findVisibleTop(PeriodType.MONTHLY, "ALL", TODAY)).isEmpty();
		verify(rankingMapper, never()).findTopVisible(anyLong(), anyInt());
	}

	private void collection(PeriodType type, String genre, long id, Period period, String... performanceIds) {
		when(rankingMapper.findLatestCompleted(type, genre, TODAY))
			.thenReturn(Optional.of(new CollectionRow(id, period.start(), period.end())));
		List<RankingEntryRow> rows = new ArrayList<>();
		for (int i = 0; i < performanceIds.length; i++) {
			rows.add(entry(i + 1, performanceIds[i]));
		}
		when(rankingMapper.findTopVisible(id, 10)).thenReturn(rows);
	}

	private static RankingEntryRow entry(int providerRank, String performanceId) {
		return new RankingEntryRow(providerRank, 100 + providerRank, performanceId, "공연", LocalDate.of(2026, 11, 1),
				LocalDate.of(2026, 12, 31), "GGGA", "FC1", "극장", null, false);
	}

	private static PerformanceSummary card(PerformanceSummaryRow row) {
		LocalizedText title = new LocalizedText(row.title(), row.title(), false);
		return new PerformanceSummary(row.performanceId(), title,
				new VenueRef(row.venueId(), new LocalizedText(row.venueName(), row.venueName(), false)),
				row.startDate(), row.endDate(), row.genreCode(), PerformanceStatus.ONGOING, row.posterUrl(),
				row.hasAward(), null);
	}

}
