package com.catticket.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.catticket.collect.client.KopisCallLimitException;
import com.catticket.collect.client.KopisClient;
import com.catticket.collect.client.KopisFatalException;
import com.catticket.collect.client.KopisRetryableException;
import com.catticket.collect.client.KopisStopReason;
import com.catticket.collect.client.KopisStoppedException;
import com.catticket.collect.client.xml.KopisBoxOfficeItem;
import com.catticket.collect.row.RankingCollectionKey;
import com.catticket.collect.row.RankingEntryInsert;
import com.catticket.common.batch.BatchContext;
import com.catticket.common.batch.StepResult;
import com.catticket.common.batch.StepSummary;
import com.catticket.common.code.Genre;
import com.catticket.common.code.PeriodType;
import com.catticket.common.display.Period;

/**
 * RankingCollectStep（S2。05 の 3.4）。クライアント・保存はモック。
 */
class RankingCollectStepTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 6, 0, 0);

	private static final BatchContext CTX = new BatchContext(1L, NOW, TODAY);

	/** 日次の現在の期間（前日） */
	private static final Period YESTERDAY = new Period(LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 23));

	private static final Period LAST_WEEK = new Period(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 22));

	private final KopisClient client = mock(KopisClient.class);

	private final RankingSaveService saveService = mock(RankingSaveService.class);

	private final RankingCollectStep step = new RankingCollectStep(client, saveService);

	private final Set<String> seen = new LinkedHashSet<>();

	@BeforeEach
	void setUp() {
		when(client.boxOffice(any(), any())).thenReturn(List.of(new KopisBoxOfficeItem("1", "PF1")));
	}

	@Test
	void sixPeriodsTimesAllAndNineGenres() {
		StepResult result = step.run(CTX, seen);

		// 6 期間 × (全体 + 9 ジャンル) = 60 回（05 の 3.4）
		verify(client, times(60)).boxOffice(any(), any());
		verify(saveService, times(60)).replace(any(), any(), eq(NOW));
		assertThat(result.success()).isTrue();
		assertThat(result.processedCount()).isEqualTo(60);
		assertThat(result.summary().get("collected")).isEqualTo(60);
		assertThat(seen).containsExactly("PF1");
	}

	@Test
	void queriesAllFirstThenGenresInOrder() {
		step.run(CTX, seen);

		InOrder order = inOrder(client, saveService);
		order.verify(client).boxOffice(YESTERDAY, Optional.empty());
		order.verify(saveService)
			.replace(new RankingCollectionKey(PeriodType.DAILY, "ALL", YESTERDAY),
					List.of(new RankingEntryInsert(1, "PF1")), NOW);
		for (Genre g : Genre.values()) {
			order.verify(client).boxOffice(YESTERDAY, Optional.of(g));
			order.verify(saveService).replace(new RankingCollectionKey(PeriodType.DAILY, g.code(), YESTERDAY),
					List.of(new RankingEntryInsert(1, "PF1")), NOW);
		}
	}

	@Test
	void emptyAllMeansNotYetAvailableAndSkipsGenres() {
		// 前日の分がまだ集計されていない
		when(client.boxOffice(YESTERDAY, Optional.empty())).thenReturn(List.of());

		StepResult result = step.run(CTX, seen);

		verify(client, never()).boxOffice(eq(YESTERDAY), eq(Optional.of(Genre.AAAA)));
		verify(saveService, never()).replace(eq(new RankingCollectionKey(PeriodType.DAILY, "ALL", YESTERDAY)), any(),
				any());
		assertThat(result.success()).isTrue();
		assertThat(result.summary().get("notYetAvailable")).isEqualTo(1);
		assertThat(result.summary().details("notYetAvailable")).containsExactly("DAILY|2026-11-23");
		assertThat(result.summary().get("collected")).isEqualTo(50);
	}

	@Test
	void emptyGenreIsRecordedAsZeroRows() {
		when(client.boxOffice(YESTERDAY, Optional.of(Genre.BBBC))).thenReturn(List.of());

		step.run(CTX, seen);

		verify(saveService).replace(new RankingCollectionKey(PeriodType.DAILY, "BBBC", YESTERDAY), List.of(), NOW);
	}

	@Test
	void failedAllSkipsGenresAndFailsTheStep() {
		when(client.boxOffice(LAST_WEEK, Optional.empty()))
			.thenThrow(new KopisRetryableException("KOPIS boxoffice HTTP 503"));

		StepResult result = step.run(CTX, seen);

		verify(client, never()).boxOffice(eq(LAST_WEEK), eq(Optional.of(Genre.AAAA)));
		assertThat(result.success()).isFalse();
		assertThat(result.processedCount()).isEqualTo(50);
		assertThat(result.summary().get("failed")).isEqualTo(1);
		assertThat(result.errorMessage())
			.isEqualTo("1 failed; first: ranking WEEKLY|2026-11-16|ALL: KopisRetryableException: KOPIS boxoffice HTTP 503");
	}

	@Test
	void failedGenreDoesNotStopOtherGenres() {
		when(client.boxOffice(YESTERDAY, Optional.of(Genre.GGGA)))
			.thenThrow(new KopisFatalException("KOPIS boxoffice returncode=99 errmsg=x"));

		StepResult result = step.run(CTX, seen);

		verify(saveService, never()).replace(eq(new RankingCollectionKey(PeriodType.DAILY, "GGGA", YESTERDAY)), any(),
				any());
		verify(saveService).replace(eq(new RankingCollectionKey(PeriodType.DAILY, "CCCA", YESTERDAY)), any(), any());
		assertThat(result.success()).isFalse();
		assertThat(result.summary().get("collected")).isEqualTo(59);
	}

	@Test
	void callLimitFailsTheStepKeepingSavedGroupsAndSeenIds() {
		// 週次の現在の期間は 3 つ目（日次の 2 期間 × 10 組を保存した後）
		when(client.boxOffice(LAST_WEEK, Optional.empty()))
			.thenThrow(new KopisCallLimitException("KOPIS call limit reached in S2: 70/70 (run 255/300)"));

		StepResult result = step.run(CTX, seen);

		assertThat(result.success()).isFalse();
		assertThat(result.processedCount()).isEqualTo(20);
		assertThat(result.summary().get("collected")).isEqualTo(20);
		assertThat(result.summary().get("callLimitReached")).isEqualTo(1);
		assertThat(result.errorMessage()).isEqualTo(
				"KopisCallLimitException: KOPIS call limit reached in S2: 70/70 (run 255/300)");
		assertThat(seen).containsExactly("PF1");
		verify(client, times(21)).boxOffice(any(), any());
	}

	@Test
	void stoppedKopisFailsTheStepKeepingSavedGroups() {
		when(client.boxOffice(LAST_WEEK, Optional.empty()))
			.thenThrow(new KopisStoppedException(KopisStopReason.BLOCKED, "KOPIS blocked: x", null));

		StepResult result = step.run(CTX, seen);

		assertThat(result.success()).isFalse();
		assertThat(result.processedCount()).isEqualTo(20);
		assertThat(result.summary().counts()).doesNotContainKey("callLimitReached");
		assertThat(result.errorMessage()).isEqualTo("KopisStoppedException: KOPIS blocked: x");
		assertThat(seen).containsExactly("PF1");
	}

	@Test
	void allRowsAreSavedAndSeen() {
		when(client.boxOffice(YESTERDAY, Optional.empty())).thenReturn(
				List.of(new KopisBoxOfficeItem("1", "PFX"), new KopisBoxOfficeItem("2", "PFY"),
						new KopisBoxOfficeItem("50", "PFZ")));

		step.run(CTX, seen);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<RankingEntryInsert>> entries = ArgumentCaptor.forClass(List.class);
		verify(saveService).replace(eq(new RankingCollectionKey(PeriodType.DAILY, "ALL", YESTERDAY)),
				entries.capture(), eq(NOW));
		assertThat(entries.getValue()).containsExactly(new RankingEntryInsert(1, "PFX"),
				new RankingEntryInsert(2, "PFY"), new RankingEntryInsert(50, "PFZ"));
		assertThat(seen).contains("PFX", "PFY", "PFZ", "PF1");
	}

	@Test
	void invalidAndDuplicateRowsAreSkipped() {
		RankingCollectionKey key = new RankingCollectionKey(PeriodType.DAILY, "ALL", YESTERDAY);
		StepSummary summary = new StepSummary();

		List<RankingEntryInsert> entries = RankingCollectStep.toEntries(key,
				List.of(new KopisBoxOfficeItem("1", "PFA"), new KopisBoxOfficeItem("x", "PFB"),
						new KopisBoxOfficeItem("0", "PFC"), new KopisBoxOfficeItem("3", null),
						new KopisBoxOfficeItem("4", "PFA"), new KopisBoxOfficeItem(null, "PFD"),
						new KopisBoxOfficeItem("5", "PFE")),
				summary);

		assertThat(entries).containsExactly(new RankingEntryInsert(1, "PFA"), new RankingEntryInsert(5, "PFE"));
		assertThat(summary.get("skippedRow")).isEqualTo(5);
		assertThat(summary.details("skippedRow")).contains("DAILY|2026-11-23|ALL|4|PFA");
	}

}
