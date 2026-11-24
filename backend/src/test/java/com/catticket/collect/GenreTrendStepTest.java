package com.catticket.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.catticket.collect.client.KopisCallPolicy;
import com.catticket.collect.client.KopisClient;
import com.catticket.collect.client.KopisFatalException;
import com.catticket.collect.client.StepAbortedException;
import com.catticket.collect.client.xml.KopisGenreStat;
import com.catticket.collect.row.GenreTrendRow;
import com.catticket.common.batch.BatchContext;
import com.catticket.common.batch.StepResult;
import com.catticket.common.code.Genre;
import com.catticket.common.display.Period;

/**
 * GenreTrendStep（S6。05 の 3.2 の「S6 の詳細」）。クライアント・保存はモック。
 */
class GenreTrendStepTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 6, 0, 0);

	private static final BatchContext CTX = new BatchContext(1L, NOW, LocalDate.of(2026, 11, 24));

	/** 前日までの 30 日 */
	private static final Period PERIOD = new Period(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 11, 23));

	private final KopisCallPolicy policy = mock(KopisCallPolicy.class);

	private final KopisClient client = mock(KopisClient.class);

	private final GenreTrendSaveService saveService = mock(GenreTrendSaveService.class);

	private final GenreTrendStep step = new GenreTrendStep(policy, client, saveService);

	@Test
	void savesNationalSharesOfTheLast30Days() {
		when(client.genreStats(PERIOD)).thenReturn(List.of(new KopisGenreStat("연극", "1"),
				new KopisGenreStat("뮤지컬", "3"), new KopisGenreStat("합계", "4")));

		StepResult result = step.run(CTX);

		InOrder order = inOrder(policy, client, saveService);
		order.verify(policy).startStep();
		order.verify(client).genreStats(PERIOD);
		order.verify(saveService)
			.replace(PERIOD,
					List.of(new GenreTrendRow(Genre.AAAA, 1, new BigDecimal("25.000000")),
							new GenreTrendRow(Genre.GGGA, 3, new BigDecimal("75.000000"))),
					GenreTrendBasis.NATIONAL, NOW);
		assertThat(result.success()).isTrue();
		assertThat(result.processedCount()).isEqualTo(2);
		assertThat(result.summary().get("saved")).isEqualTo(2);
	}

	@Test
	void zeroTotalSavesNothingButSucceeds() {
		when(client.genreStats(PERIOD)).thenReturn(List.of(new KopisGenreStat("연극", "0")));

		StepResult result = step.run(CTX);

		verifyNoInteractions(saveService);
		assertThat(result.success()).isTrue();
		assertThat(result.summary().get("zeroTotal")).isEqualTo(1);
	}

	@Test
	void unknownGenreSavesNothingAndFails() {
		when(client.genreStats(PERIOD)).thenReturn(List.of(new KopisGenreStat("오페라", "1")));

		StepResult result = step.run(CTX);

		verifyNoInteractions(saveService);
		assertThat(result.success()).isFalse();
		assertThat(result.errorMessage()).isEqualTo("unknown genre: 오페라");
	}

	@Test
	void kopisFailureFailsTheStep() {
		when(client.genreStats(PERIOD)).thenThrow(new KopisFatalException("KOPIS boxStatsCate returncode=02 errmsg=x"));

		StepResult result = step.run(CTX);

		verifyNoInteractions(saveService);
		assertThat(result.success()).isFalse();
		assertThat(result.errorMessage()).isEqualTo("KopisFatalException: KOPIS boxStatsCate returncode=02 errmsg=x");
	}

	@Test
	void stepAbortedIsNotCaught() {
		when(client.genreStats(any())).thenThrow(new StepAbortedException("aborted"));

		assertThatThrownBy(() -> step.run(CTX)).isInstanceOf(StepAbortedException.class);
		verify(policy).startStep();
	}

}
