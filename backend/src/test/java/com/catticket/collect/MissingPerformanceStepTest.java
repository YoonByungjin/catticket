package com.catticket.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.catticket.collect.client.KopisCallLimitException;
import com.catticket.collect.client.KopisRetryableException;
import com.catticket.collect.client.KopisStopReason;
import com.catticket.collect.client.KopisStoppedException;
import com.catticket.common.batch.BatchContext;
import com.catticket.common.batch.StepResult;
import com.catticket.common.batch.StepSummary;

/**
 * MissingPerformanceStep（S4。05 の 3.2 の S4）。Mapper・公演の取り込みはモック。
 */
class MissingPerformanceStepTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 6, 0, 0);

	private static final BatchContext CTX = new BatchContext(1L, NOW, LocalDate.of(2026, 11, 24));

	private final CollectMapper mapper = mock(CollectMapper.class);

	private final KopisPerformanceImporter importer = mock(KopisPerformanceImporter.class);

	private final MissingPerformanceStep step = new MissingPerformanceStep(mapper, importer);

	@BeforeEach
	void setUp() {
		doAnswer(inv -> {
			inv.<StepSummary>getArgument(3).increment("saved");
			return null;
		}).when(importer).importOne(anyString(), anyBoolean(), any(), any(), any());
	}

	@Test
	void importsOnlyIdsNotInTheDb() {
		Set<String> seen = new LinkedHashSet<>(List.of("PFA", "PFB", "PFC", "PF0123456789012345678"));
		when(mapper.findExistingKopisIds(anyCollection())).thenReturn(Set.of("PFB"));

		StepResult result = step.run(CTX, seen, Set.of());

		// 公演 ID として使えない ID は DB に問い合わせない
		verify(mapper).findExistingKopisIds(List.of("PFA", "PFB", "PFC"));
		InOrder order = inOrder(importer);
		order.verify(importer).importOne(eq("PFA"), eq(false), eq(NOW), any(), any());
		order.verify(importer).importOne(eq("PFC"), eq(false), eq(NOW), any(), any());
		verify(importer, never()).importOne(eq("PFB"), anyBoolean(), any(), any(), any());
		assertThat(result.success()).isTrue();
		assertThat(result.processedCount()).isEqualTo(2);
		assertThat(result.summary().get("candidates")).isEqualTo(3);
		assertThat(result.summary().get("missing")).isEqualTo(2);
	}

	@Test
	void awardedIsPassedOnlyForAwardIds() {
		// 順位だけ・受賞作だけ・両方（06 の 11 の U-13）。候補は rankingIds の順、続けて awardIds
		Set<String> rankingIds = new LinkedHashSet<>(List.of("PFR", "PFBOTH"));
		Set<String> awardIds = new LinkedHashSet<>(List.of("PFBOTH", "PFW"));
		when(mapper.findExistingKopisIds(anyCollection())).thenReturn(Set.of());

		StepResult result = step.run(CTX, rankingIds, awardIds);

		verify(mapper).findExistingKopisIds(List.of("PFR", "PFBOTH", "PFW"));
		InOrder order = inOrder(importer);
		order.verify(importer).importOne(eq("PFR"), eq(false), eq(NOW), any(), any());
		order.verify(importer).importOne(eq("PFBOTH"), eq(true), eq(NOW), any(), any());
		order.verify(importer).importOne(eq("PFW"), eq(true), eq(NOW), any(), any());
		assertThat(result.summary().get("candidates")).isEqualTo(3);
		assertThat(result.summary().get("missing")).isEqualTo(3);
	}

	@Test
	void awardIdsAloneAreCandidates() {
		when(mapper.findExistingKopisIds(anyCollection())).thenReturn(Set.of("PFW1"));

		StepResult result = step.run(CTX, Set.of(), new LinkedHashSet<>(List.of("PFW1", "PFW2")));

		verify(importer).importOne(eq("PFW2"), eq(true), eq(NOW), any(), any());
		verify(importer, never()).importOne(eq("PFW1"), anyBoolean(), any(), any(), any());
		assertThat(result.processedCount()).isEqualTo(1);
	}

	@Test
	void nothingToDoWithoutCandidates() {
		StepResult result = step.run(CTX, Set.of(), Set.of());

		verify(mapper, never()).findExistingKopisIds(anyCollection());
		assertThat(result.success()).isTrue();
		assertThat(result.processedCount()).isZero();
	}

	@Test
	void largeInputIsLookedUpInChunks() {
		Set<String> seen = new LinkedHashSet<>(IntStream.range(0, 1200).mapToObj(i -> "PF" + i).toList());
		Set<Collection<String>> chunks = new HashSet<>();
		when(mapper.findExistingKopisIds(anyCollection())).thenAnswer(inv -> {
			Collection<String> ids = inv.getArgument(0);
			chunks.add(List.copyOf(ids));
			return new HashSet<>(ids);
		});

		StepResult result = step.run(CTX, seen, Set.of());

		verify(mapper, times(3)).findExistingKopisIds(anyCollection());
		assertThat(chunks).allSatisfy(c -> assertThat(c.size()).isLessThanOrEqualTo(500));
		verify(importer, never()).importOne(anyString(), anyBoolean(), any(), any(), any());
		assertThat(result.summary().get("missing")).isZero();
	}

	@Test
	void failedImportFailsTheStep() {
		when(mapper.findExistingKopisIds(anyCollection())).thenReturn(Set.of());
		doAnswer(inv -> {
			inv.<UnitFailures>getArgument(4).record("performance PFA", new KopisRetryableException("timeout"));
			return null;
		}).when(importer).importOne(eq("PFA"), anyBoolean(), any(), any(), any());

		StepResult result = step.run(CTX, new LinkedHashSet<>(List.of("PFA", "PFB")), Set.of());

		verify(importer).importOne(eq("PFB"), anyBoolean(), any(), any(), any());
		assertThat(result.success()).isFalse();
		assertThat(result.errorMessage()).isEqualTo("1 failed; first: performance PFA: KopisRetryableException: timeout");
	}

	@Test
	void callLimitFailsTheStepKeepingSavedCount() {
		when(mapper.findExistingKopisIds(anyCollection())).thenReturn(Set.of());
		doAnswer(inv -> {
			inv.<StepSummary>getArgument(3).increment("saved");
			return null;
		}).when(importer).importOne(eq("PFA"), anyBoolean(), any(), any(), any());
		doAnswer(inv -> {
			throw new KopisCallLimitException("KOPIS call limit reached in S4: 2/2 (run 18/20)");
		}).when(importer).importOne(eq("PFB"), anyBoolean(), any(), any(), any());

		StepResult result = step.run(CTX, new LinkedHashSet<>(List.of("PFA", "PFB", "PFC")), Set.of());

		verify(importer, never()).importOne(eq("PFC"), anyBoolean(), any(), any(), any());
		assertThat(result.success()).isFalse();
		assertThat(result.processedCount()).isEqualTo(1);
		assertThat(result.summary().get("saved")).isEqualTo(1);
		assertThat(result.summary().get("callLimitReached")).isEqualTo(1);
		assertThat(result.errorMessage()).isEqualTo(
				"KopisCallLimitException: KOPIS call limit reached in S4: 2/2 (run 18/20)");
	}

	@Test
	void stoppedKopisFailsTheStep() {
		when(mapper.findExistingKopisIds(anyCollection())).thenReturn(Set.of());
		doAnswer(inv -> {
			throw new KopisStoppedException(KopisStopReason.BLOCKED, "KOPIS blocked: x", null);
		}).when(importer).importOne(eq("PFA"), anyBoolean(), any(), any(), any());

		StepResult result = step.run(CTX, new LinkedHashSet<>(List.of("PFA", "PFB")), Set.of());

		verify(importer, never()).importOne(eq("PFB"), anyBoolean(), any(), any(), any());
		assertThat(result.success()).isFalse();
		assertThat(result.processedCount()).isZero();
		assertThat(result.summary().counts()).doesNotContainKey("callLimitReached");
		assertThat(result.errorMessage()).isEqualTo("KopisStoppedException: KOPIS blocked: x");
	}

}
