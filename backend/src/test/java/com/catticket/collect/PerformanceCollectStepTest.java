package com.catticket.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.catticket.collect.client.KopisCallLimitException;
import com.catticket.collect.client.KopisClient;
import com.catticket.collect.client.KopisFatalException;
import com.catticket.collect.client.KopisPage;
import com.catticket.collect.client.KopisRetryableException;
import com.catticket.collect.client.KopisStopReason;
import com.catticket.collect.client.KopisStoppedException;
import com.catticket.collect.client.xml.KopisPerformanceListItem;
import com.catticket.common.batch.BatchContext;
import com.catticket.common.batch.BatchRunMapper;
import com.catticket.common.batch.LastS1;
import com.catticket.common.batch.PreviousS1;
import com.catticket.common.batch.StepResult;
import com.catticket.common.batch.StepSummary;

/**
 * PerformanceCollectStep（S1。05 の 3.3）。クライアント・Mapper・公演の取り込みはモック。
 */
class PerformanceCollectStepTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 6, 0, 0);

	private static final BatchContext CTX = new BatchContext(1L, NOW, TODAY);

	private static final LocalDate SECOND_RANGE = LocalDate.of(2026, 12, 25);

	private final KopisClient client = mock(KopisClient.class);

	private final BatchRunMapper batchRunMapper = mock(BatchRunMapper.class);

	private final CollectMapper collectMapper = mock(CollectMapper.class);

	private final KopisPerformanceImporter importer = mock(KopisPerformanceImporter.class);

	private final PerformanceCollectStep step = new PerformanceCollectStep(client, batchRunMapper, collectMapper,
			importer);

	@BeforeEach
	void setUp() {
		when(batchRunMapper.findLastSuccessfulS1()).thenReturn(Optional.empty());
		when(batchRunMapper.findLatestS1Before(1L)).thenReturn(Optional.empty());
		when(client.listPerformances(any(), any(), any(), anyInt())).thenReturn(page());
		when(collectMapper.findKopisIdsCollectedSince(anyCollection(), any())).thenReturn(Set.of());
		// 取り込みは保存できたとして数える
		doAnswer(inv -> {
			inv.<StepSummary>getArgument(3).increment("saved");
			return null;
		}).when(importer).importOne(anyString(), anyBoolean(), any(), any(), any());
	}

	private static KopisPage<KopisPerformanceListItem> page(String... ids) {
		return new KopisPage<>(Arrays.stream(ids).map(KopisPerformanceListItem::new).toList());
	}

	private static KopisPage<KopisPerformanceListItem> fullPage(int from) {
		return new KopisPage<>(IntStream.range(from, from + 100).mapToObj(i -> new KopisPerformanceListItem("PF" + i)).toList());
	}

	@Test
	void firstRunListsSixRangesWithoutAfterDateAndRecordsWindowEnd() {
		LocalDate firstEnd = LocalDate.of(2026, 12, 24);
		// 1 区間目: 100 件ちょうど → 次のページ → 最後のページ。2 区間目にも同じ公演がある
		when(client.listPerformances(TODAY, firstEnd, null, 1)).thenReturn(fullPage(0));
		when(client.listPerformances(TODAY, firstEnd, null, 2)).thenReturn(page("PFA", "PFB"));
		when(client.listPerformances(eq(SECOND_RANGE), any(), isNull(), eq(1))).thenReturn(page("PFB", "PFC"));

		StepResult result = step.run(CTX);

		verify(client, times(7)).listPerformances(any(), any(), isNull(), anyInt());
		verify(client, never()).listPerformances(TODAY, firstEnd, null, 3);
		// 区間をまたいだ同じ公演は 1 回だけ取り込む
		verify(importer, times(103)).importOne(anyString(), eq(false), eq(NOW), any(), any());
		verify(importer, times(1)).importOne(eq("PFB"), anyBoolean(), any(), any(), any());
		assertThat(result.success()).isTrue();
		assertThat(result.windowEndDate()).isEqualTo(LocalDate.of(2027, 5, 23));
		// 前の S1 がないので基準日は今日
		assertThat(result.collectBaseDate()).isEqualTo(TODAY);
		assertThat(result.processedCount()).isEqualTo(103);
		assertThat(result.summary().get("ranges")).isEqualTo(6);
		assertThat(result.summary().get("pages")).isEqualTo(7);
		assertThat(result.summary().get("listed")).isEqualTo(103);
	}

	@Test
	void savesEachRangeBeforeListingTheNext() {
		when(client.listPerformances(eq(TODAY), any(), isNull(), eq(1))).thenReturn(page("PFA"));
		when(client.listPerformances(eq(SECOND_RANGE), any(), isNull(), eq(1))).thenReturn(page("PFB"));

		step.run(CTX);

		// 上限で途中で止まっても、読んだ区間の保存が進むように（05 の 3.3 の 1.）
		InOrder order = inOrder(client, importer);
		order.verify(client).listPerformances(eq(TODAY), any(), isNull(), eq(1));
		order.verify(importer).importOne(eq("PFA"), anyBoolean(), any(), any(), any());
		order.verify(client).listPerformances(eq(SECOND_RANGE), any(), isNull(), eq(1));
		order.verify(importer).importOne(eq("PFB"), anyBoolean(), any(), any(), any());
	}

	@Test
	void skipsPerformancesCollectedSinceTheBaseDate() {
		when(batchRunMapper.findLatestS1Before(1L))
			.thenReturn(Optional.of(new PreviousS1("FAILURE", LocalDate.of(2026, 11, 22))));
		when(client.listPerformances(eq(TODAY), any(), isNull(), eq(1))).thenReturn(page("PFA", "PFB", "PFC"));
		when(client.listPerformances(eq(SECOND_RANGE), any(), isNull(), eq(1))).thenReturn(page("PFC", "PFD"));
		when(collectMapper.findKopisIdsCollectedSince(anyCollection(), any())).thenReturn(Set.of("PFB"));

		StepResult result = step.run(CTX);

		// 渡す since は基準日の 0 時
		verify(collectMapper).findKopisIdsCollectedSince(argThat(ids -> ids.containsAll(Set.of("PFA", "PFB", "PFC"))),
				eq(LocalDateTime.of(2026, 11, 22, 0, 0)));
		// 2 区間目は、すでに扱った PFC を除いて照会する
		verify(collectMapper).findKopisIdsCollectedSince(argThat((Collection<String> ids) -> ids.size() == 1
				&& ids.contains("PFD")), eq(LocalDateTime.of(2026, 11, 22, 0, 0)));
		verify(importer, never()).importOne(eq("PFB"), anyBoolean(), any(), any(), any());
		verify(importer, times(1)).importOne(eq("PFC"), anyBoolean(), any(), any(), any());
		verify(importer, times(3)).importOne(anyString(), anyBoolean(), any(), any(), any());
		assertThat(result.summary().get("alreadyCollected")).isEqualTo(1);
		assertThat(result.summary().get("listed")).isEqualTo(4);
		assertThat(result.collectBaseDate()).isEqualTo(LocalDate.of(2026, 11, 22));
	}

	@Test
	void lookupIsSplitIntoChunks() {
		when(client.listPerformances(eq(TODAY), any(), isNull(), anyInt()))
			.thenReturn(fullPage(0), fullPage(100), fullPage(200), fullPage(300), fullPage(400), page("PFX"));

		step.run(CTX);

		verify(collectMapper).findKopisIdsCollectedSince(argThat((Collection<String> ids) -> ids.size() == 500),
				any());
		verify(collectMapper).findKopisIdsCollectedSince(argThat((Collection<String> ids) -> ids.size() == 1),
				any());
	}

	@Test
	void callLimitWhileListingFailsWithCountsAndBaseDateButNoWindowEnd() {
		when(batchRunMapper.findLatestS1Before(1L))
			.thenReturn(Optional.of(new PreviousS1("FAILURE", LocalDate.of(2026, 11, 22))));
		when(client.listPerformances(eq(TODAY), any(), isNull(), eq(1))).thenReturn(page("PFA", "PFB"));
		when(client.listPerformances(eq(SECOND_RANGE), any(), isNull(), eq(1)))
			.thenThrow(new KopisCallLimitException("KOPIS call limit reached in S1: 11/11 (run 11/20)"));

		StepResult result = step.run(CTX);

		assertThat(result.success()).isFalse();
		assertThat(result.windowEndDate()).isNull();
		assertThat(result.collectBaseDate()).isEqualTo(LocalDate.of(2026, 11, 22));
		assertThat(result.errorMessage())
			.isEqualTo("KopisCallLimitException: KOPIS call limit reached in S1: 11/11 (run 11/20)");
		assertThat(result.summary().get("callLimitReached")).isEqualTo(1);
		assertThat(result.summary().get("saved")).isEqualTo(2);
		assertThat(result.processedCount()).isEqualTo(2);
		// 3 区間目からは呼ばない
		verify(client, times(2)).listPerformances(any(), any(), any(), anyInt());
	}

	@Test
	void callLimitWhileImportingStopsTheStep() {
		when(client.listPerformances(eq(TODAY), any(), isNull(), eq(1))).thenReturn(page("PFA", "PFB", "PFC"));
		doAnswer(inv -> {
			throw new KopisCallLimitException("limit");
		}).when(importer).importOne(eq("PFB"), anyBoolean(), any(), any(), any());

		StepResult result = step.run(CTX);

		verify(importer, never()).importOne(eq("PFC"), anyBoolean(), any(), any(), any());
		assertThat(result.success()).isFalse();
		assertThat(result.windowEndDate()).isNull();
		assertThat(result.collectBaseDate()).isEqualTo(TODAY);
		assertThat(result.summary().get("saved")).isEqualTo(1);
		assertThat(result.summary().get("callLimitReached")).isEqualTo(1);
	}

	@Test
	void stoppedKopisFailsWithCountsButWithoutCallLimitReached() {
		when(client.listPerformances(eq(TODAY), any(), isNull(), eq(1))).thenReturn(page("PFA", "PFB"));
		doAnswer(inv -> {
			throw new KopisStoppedException(KopisStopReason.BLOCKED, "KOPIS blocked: x", null);
		}).when(importer).importOne(eq("PFB"), anyBoolean(), any(), any(), any());

		StepResult result = step.run(CTX);

		assertThat(result.success()).isFalse();
		assertThat(result.windowEndDate()).isNull();
		assertThat(result.collectBaseDate()).isEqualTo(TODAY);
		assertThat(result.errorMessage()).isEqualTo("KopisStoppedException: KOPIS blocked: x");
		assertThat(result.summary().get("saved")).isEqualTo(1);
		assertThat(result.summary().counts()).doesNotContainKey("callLimitReached");
		verify(client, times(1)).listPerformances(any(), any(), any(), anyInt());
	}

	@Test
	void resumedRunThatFinishesRecordsWindowEndAndTheInheritedBaseDate() {
		LocalDate today = LocalDate.of(2026, 11, 27);
		BatchContext ctx = new BatchContext(5L, LocalDateTime.of(2026, 11, 27, 6, 0), today);
		when(batchRunMapper.findLatestS1Before(5L))
			.thenReturn(Optional.of(new PreviousS1("FAILURE", LocalDate.of(2026, 11, 25))));
		when(client.listPerformances(eq(today), any(), isNull(), eq(1))).thenReturn(page("PFA"));

		StepResult result = step.run(ctx);

		assertThat(result.success()).isTrue();
		assertThat(result.windowEndDate()).isEqualTo(today.plusDays(180));
		assertThat(result.collectBaseDate()).isEqualTo(LocalDate.of(2026, 11, 25));
	}

	@Test
	void previousSuccessfulS1MakesTodayTheBaseDate() {
		when(batchRunMapper.findLatestS1Before(1L))
			.thenReturn(Optional.of(new PreviousS1("SUCCESS", LocalDate.of(2026, 11, 23))));

		StepResult result = step.run(CTX);

		assertThat(result.collectBaseDate()).isEqualTo(TODAY);
		verify(collectMapper, never()).findKopisIdsCollectedSince(anyCollection(), any());
	}

	@Test
	void laterRunUsesAfterDateAndNewDays() {
		when(batchRunMapper.findLastSuccessfulS1())
			.thenReturn(Optional.of(new LastS1(TODAY.minusDays(1), TODAY.minusDays(1).plusDays(180))));

		StepResult result = step.run(CTX);

		// (a) 6 区間は afterdate = 前回の成功の日の前日、(b) 新しく窓に入った 1 日は afterdate なし
		verify(client, times(6)).listPerformances(any(), any(), eq(TODAY.minusDays(2)), eq(1));
		verify(client).listPerformances(LocalDate.of(2027, 5, 23), LocalDate.of(2027, 5, 23), null, 1);
		assertThat(result.success()).isTrue();
		assertThat(result.windowEndDate()).isEqualTo(LocalDate.of(2027, 5, 23));
	}

	@Test
	void failedRangeFailsTheStepWithoutWindowEndButContinues() {
		when(client.listPerformances(eq(TODAY), any(), isNull(), eq(1))).thenReturn(page("PFA"));
		when(client.listPerformances(eq(SECOND_RANGE), any(), isNull(), eq(1)))
			.thenThrow(new KopisRetryableException("KOPIS pblprfr unexpected response: no row element"));

		StepResult result = step.run(CTX);

		// ほかの区間も読み、読めた公演は保存する
		verify(client, times(6)).listPerformances(any(), any(), isNull(), eq(1));
		verify(importer).importOne(eq("PFA"), anyBoolean(), any(), any(), any());
		assertThat(result.success()).isFalse();
		assertThat(result.windowEndDate()).isNull();
		assertThat(result.collectBaseDate()).isEqualTo(TODAY);
		assertThat(result.processedCount()).isEqualTo(1);
		assertThat(result.errorMessage()).isEqualTo("1 failed; first: range 2026-12-25~2027-01-24 page 1: "
				+ "KopisRetryableException: KOPIS pblprfr unexpected response: no row element");
		assertThat(result.summary().get("failed")).isEqualTo(1);
	}

	@Test
	void failedPerformanceFailsTheStep() {
		when(client.listPerformances(eq(TODAY), any(), isNull(), eq(1))).thenReturn(page("PFA", "PFB"));
		doAnswer(inv -> {
			inv.<UnitFailures>getArgument(4).record("performance PFA", new KopisFatalException("KOPIS HTTP 404"));
			return null;
		}).when(importer).importOne(eq("PFA"), anyBoolean(), any(), any(), any());

		StepResult result = step.run(CTX);

		verify(importer).importOne(eq("PFB"), anyBoolean(), any(), any(), any());
		assertThat(result.success()).isFalse();
		assertThat(result.windowEndDate()).isNull();
		assertThat(result.errorMessage()).startsWith("1 failed; first: performance PFA: KopisFatalException");
	}

	@Test
	void invalidIdsInTheListAreSkipped() {
		when(client.listPerformances(eq(TODAY), any(), isNull(), eq(1)))
			.thenReturn(page("PFA", null, "PF012345678901234567890"));

		StepResult result = step.run(CTX);

		verify(importer, times(1)).importOne(anyString(), anyBoolean(), any(), any(), any());
		assertThat(result.summary().get("skippedId")).isEqualTo(2);
		assertThat(result.success()).isTrue();
	}

	@Test
	void emptyWindowIsSuccess() {
		StepResult result = step.run(CTX);

		assertThat(result.success()).isTrue();
		assertThat(result.processedCount()).isZero();
		assertThat(result.windowEndDate()).isEqualTo(LocalDate.of(2027, 5, 23));
		assertThat(result.summary().get("listed")).isZero();
		// 公演がなければ収集済みの確認もしない
		verify(collectMapper, never()).findKopisIdsCollectedSince(anyCollection(), any());
	}

}
