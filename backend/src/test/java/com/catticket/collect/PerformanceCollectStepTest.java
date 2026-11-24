package com.catticket.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.catticket.collect.client.KopisCallPolicy;
import com.catticket.collect.client.KopisClient;
import com.catticket.collect.client.KopisFatalException;
import com.catticket.collect.client.KopisPage;
import com.catticket.collect.client.KopisRetryableException;
import com.catticket.collect.client.StepAbortedException;
import com.catticket.collect.client.xml.KopisPerformanceListItem;
import com.catticket.common.batch.BatchContext;
import com.catticket.common.batch.BatchRunMapper;
import com.catticket.common.batch.LastS1;
import com.catticket.common.batch.StepResult;
import com.catticket.common.batch.StepSummary;

/**
 * PerformanceCollectStep（S1。05 の 3.3）。クライアント・Mapper・公演の取り込みはモック。
 */
class PerformanceCollectStepTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 6, 0, 0);

	private static final BatchContext CTX = new BatchContext(1L, NOW, TODAY);

	private final KopisCallPolicy policy = mock(KopisCallPolicy.class);

	private final KopisClient client = mock(KopisClient.class);

	private final BatchRunMapper batchRunMapper = mock(BatchRunMapper.class);

	private final KopisPerformanceImporter importer = mock(KopisPerformanceImporter.class);

	private final PerformanceCollectStep step = new PerformanceCollectStep(policy, client, batchRunMapper, importer);

	@BeforeEach
	void setUp() {
		when(batchRunMapper.findLastSuccessfulS1()).thenReturn(Optional.empty());
		when(client.listPerformances(any(), any(), any(), anyInt())).thenReturn(page());
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
	void startsTheStepBeforeCallingKopis() {
		step.run(CTX);

		InOrder order = inOrder(policy, client);
		order.verify(policy).startStep();
		order.verify(client, atLeastOnce()).listPerformances(any(), any(), any(), anyInt());
	}

	@Test
	void firstRunListsSixRangesWithoutAfterDateAndRecordsWindowEnd() {
		LocalDate firstEnd = LocalDate.of(2026, 12, 24);
		// 1 区間目: 100 件ちょうど → 次のページ → 最後のページ。2 区間目にも同じ公演がある
		when(client.listPerformances(TODAY, firstEnd, null, 1)).thenReturn(fullPage(0));
		when(client.listPerformances(TODAY, firstEnd, null, 2)).thenReturn(page("PFA", "PFB"));
		when(client.listPerformances(eq(LocalDate.of(2026, 12, 25)), any(), isNull(), eq(1)))
			.thenReturn(page("PFB", "PFC"));

		StepResult result = step.run(CTX);

		verify(client, times(7)).listPerformances(any(), any(), isNull(), anyInt());
		verify(client, never()).listPerformances(TODAY, firstEnd, null, 3);
		// 区間をまたいだ同じ公演は 1 回だけ取り込む
		verify(importer, times(103)).importOne(anyString(), eq(false), eq(NOW), any(), any());
		verify(importer, times(1)).importOne(eq("PFB"), anyBoolean(), any(), any(), any());
		assertThat(result.success()).isTrue();
		assertThat(result.windowEndDate()).isEqualTo(LocalDate.of(2027, 5, 23));
		assertThat(result.processedCount()).isEqualTo(103);
		assertThat(result.summary().get("ranges")).isEqualTo(6);
		assertThat(result.summary().get("pages")).isEqualTo(7);
		assertThat(result.summary().get("listed")).isEqualTo(103);
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
		when(client.listPerformances(eq(LocalDate.of(2026, 12, 25)), any(), isNull(), eq(1)))
			.thenThrow(new KopisRetryableException("KOPIS pblprfr unexpected response: no row element"));

		StepResult result = step.run(CTX);

		// ほかの区間も読み、読めた公演は保存する
		verify(client, times(6)).listPerformances(any(), any(), isNull(), eq(1));
		verify(importer).importOne(eq("PFA"), anyBoolean(), any(), any(), any());
		assertThat(result.success()).isFalse();
		assertThat(result.windowEndDate()).isNull();
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
	void stepAbortedIsNotCaught() {
		when(client.listPerformances(any(), any(), any(), anyInt())).thenThrow(new StepAbortedException("aborted"));

		assertThatThrownBy(() -> step.run(CTX)).isInstanceOf(StepAbortedException.class);
		verify(importer, never()).importOne(anyString(), anyBoolean(), any(), any(), any());
	}

	@Test
	void stepAbortedInImportIsNotCaught() {
		when(client.listPerformances(eq(TODAY), any(), isNull(), eq(1))).thenReturn(page("PFA", "PFB"));
		doAnswer(inv -> {
			throw new StepAbortedException("aborted");
		}).when(importer).importOne(eq("PFA"), anyBoolean(), any(), any(), any());

		assertThatThrownBy(() -> step.run(CTX)).isInstanceOf(StepAbortedException.class);
		verify(importer, never()).importOne(eq("PFB"), anyBoolean(), any(), any(), any());
	}

	@Test
	void emptyWindowIsSuccess() {
		StepResult result = step.run(CTX);

		assertThat(result.success()).isTrue();
		assertThat(result.processedCount()).isZero();
		assertThat(result.windowEndDate()).isEqualTo(LocalDate.of(2027, 5, 23));
		assertThat(result.summary().get("listed")).isZero();
	}

}
