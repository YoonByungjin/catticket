package com.catticket.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.catticket.collect.client.KopisCallLimitException;
import com.catticket.collect.client.KopisClient;
import com.catticket.collect.client.KopisPage;
import com.catticket.collect.client.KopisRetryableException;
import com.catticket.collect.client.KopisStopReason;
import com.catticket.collect.client.KopisStoppedException;
import com.catticket.collect.client.xml.KopisAwardItem;
import com.catticket.common.batch.BatchContext;
import com.catticket.common.batch.StepResult;

/**
 * AwardCollectStep（S3。05 の 3.2 の S3）。クライアント・Mapper はモック。
 */
class AwardCollectStepTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 6, 0, 0);

	private static final BatchContext CTX = new BatchContext(1L, NOW, TODAY);

	private final KopisClient client = mock(KopisClient.class);

	private final CollectMapper mapper = mock(CollectMapper.class);

	private final AwardCollectStep step = new AwardCollectStep(client, mapper);

	private final Set<String> seen = new LinkedHashSet<>();

	@BeforeEach
	void setUp() {
		when(client.awards(any(), any(), anyInt())).thenReturn(new KopisPage<>(List.of()));
	}

	@Test
	void queriesTheS1WindowAndMarksEachPage() {
		List<KopisAwardItem> full = IntStream.range(0, 100).mapToObj(i -> new KopisAwardItem("PF" + i)).toList();
		when(client.awards(TODAY, LocalDate.of(2026, 12, 24), 1)).thenReturn(new KopisPage<>(full));
		when(client.awards(TODAY, LocalDate.of(2026, 12, 24), 2))
			.thenReturn(new KopisPage<>(List.of(new KopisAwardItem("PFA"))));
		when(mapper.markAwarded(anyCollection(), eq(NOW))).thenReturn(3);

		StepResult result = step.run(CTX, seen);

		// 6 区間 + 1 区間目の 2 ページ目
		verify(client, times(7)).awards(any(), any(), anyInt());
		verify(client).awards(LocalDate.of(2027, 4, 28), LocalDate.of(2027, 5, 23), 1);
		// 1回の照会の結果ごとに UPDATE（空のページでは呼ばない）
		verify(mapper, times(2)).markAwarded(anyCollection(), eq(NOW));
		assertThat(seen).hasSize(101).contains("PF0", "PFA");
		assertThat(result.success()).isTrue();
		assertThat(result.summary().get("listed")).isEqualTo(101);
		assertThat(result.summary().get("marked")).isEqualTo(6);
	}

	@Test
	void failedRangeFailsTheStepAndContinues() {
		when(client.awards(TODAY, LocalDate.of(2026, 12, 24), 1)).thenThrow(new KopisRetryableException("timeout"));
		when(client.awards(LocalDate.of(2026, 12, 25), LocalDate.of(2027, 1, 24), 1))
			.thenReturn(new KopisPage<>(List.of(new KopisAwardItem("PFA"))));

		StepResult result = step.run(CTX, seen);

		// 失敗した区間の後の区間も照会する
		verify(mapper).markAwarded(Set.of("PFA"), NOW);
		assertThat(result.success()).isFalse();
		assertThat(result.errorMessage())
			.isEqualTo("1 failed; first: awards 2026-11-24~2026-12-24 page 1: KopisRetryableException: timeout");
	}

	@Test
	void invalidIdsAreNotMarked() {
		when(client.awards(TODAY, LocalDate.of(2026, 12, 24), 1))
			.thenReturn(new KopisPage<>(List.of(new KopisAwardItem(null))));

		StepResult result = step.run(CTX, seen);

		verify(mapper, never()).markAwarded(anyCollection(), any());
		assertThat(result.summary().get("skippedId")).isEqualTo(1);
		assertThat(seen).isEmpty();
	}

	@Test
	void callLimitFailsTheStepKeepingMarkedCountAndSeenIds() {
		when(client.awards(TODAY, LocalDate.of(2026, 12, 24), 1))
			.thenReturn(new KopisPage<>(List.of(new KopisAwardItem("PFA"), new KopisAwardItem("PFB"))));
		when(mapper.markAwarded(anyCollection(), any())).thenReturn(2);
		when(client.awards(eq(LocalDate.of(2026, 12, 25)), any(), anyInt()))
			.thenThrow(new KopisCallLimitException("KOPIS call limit reached in S3: 1/1 (run 12/20)"));

		StepResult result = step.run(CTX, seen);

		assertThat(result.success()).isFalse();
		assertThat(result.processedCount()).isEqualTo(2);
		assertThat(result.summary().get("marked")).isEqualTo(2);
		assertThat(result.summary().get("callLimitReached")).isEqualTo(1);
		assertThat(result.errorMessage()).startsWith("KopisCallLimitException: KOPIS call limit reached in S3");
		// それまでに見た ID は S4 に渡す
		assertThat(seen).containsExactly("PFA", "PFB");
		verify(client, times(2)).awards(any(), any(), anyInt());
	}

	@Test
	void stoppedKopisFailsTheStep() {
		when(client.awards(any(), any(), anyInt()))
			.thenThrow(new KopisStoppedException(KopisStopReason.CONSECUTIVE_FAILURES, "KOPIS calls stopped", null));

		StepResult result = step.run(CTX, seen);

		assertThat(result.success()).isFalse();
		assertThat(result.processedCount()).isZero();
		assertThat(result.summary().counts()).doesNotContainKey("callLimitReached");
		assertThat(result.errorMessage()).isEqualTo("KopisStoppedException: KOPIS calls stopped");
		verify(client, times(1)).awards(any(), any(), anyInt());
	}

}
