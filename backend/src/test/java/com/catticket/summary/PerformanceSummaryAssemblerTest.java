package com.catticket.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.catticket.common.code.Lang;
import com.catticket.common.display.LocalizedText;
import com.catticket.common.display.PerformanceStatus;
import com.catticket.common.display.PerformanceSummary;
import com.catticket.common.display.PerformanceSummaryRow;
import com.catticket.common.display.Rating;
import com.catticket.common.display.VenueRef;
import com.catticket.review.RatingStat;
import com.catticket.review.ReviewService;
import com.catticket.translation.TranslationLookupService;

/**
 * PerformanceSummaryAssembler（06 の 3.5）。翻訳・評価を1回ずつまとめて引くこと（N+1 にしない）を確かめる。
 */
class PerformanceSummaryAssemblerTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 27);

	private final TranslationLookupService translationLookupService = mock(TranslationLookupService.class);

	private final ReviewService reviewService = mock(ReviewService.class);

	private final PerformanceSummaryAssembler assembler = new PerformanceSummaryAssembler(translationLookupService,
			reviewService);

	private final PerformanceSummaryRow ongoing = new PerformanceSummaryRow(1, "PF1", "오페라의 유령",
			LocalDate.of(2026, 9, 1), LocalDate.of(2027, 2, 28), "GGGA", "FC1", "샤롯데씨어터", "https://example.com/1.jpg",
			true);

	private final PerformanceSummaryRow upcoming = new PerformanceSummaryRow(2, "PF2", "햄릿", TODAY.plusDays(1),
			TODAY.plusDays(30), "AAAA", "FC1", "샤롯데씨어터", null, false);

	private final PerformanceSummaryRow ended = new PerformanceSummaryRow(3, "PF3", "번역없음", TODAY.minusDays(30),
			TODAY.minusDays(1), "AAAA", "FC2", "극장", null, false);

	@Test
	void koreanWithRatingKeepsOrderAndDoesNotTranslate() {
		when(reviewService.findRatingStats(any())).thenReturn(Map.of(1L, new RatingStat(17, 4)));

		List<PerformanceSummary> result = assembler.assemble(List.of(ongoing, upcoming, ended), Lang.KO, TODAY, true);

		verifyNoInteractions(translationLookupService);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Collection<Long>> ids = ArgumentCaptor.forClass(Collection.class);
		verify(reviewService, times(1)).findRatingStats(ids.capture());
		// 内部の ID で引く
		assertThat(ids.getValue()).containsExactly(1L, 2L, 3L);

		assertThat(result).extracting(PerformanceSummary::performanceId).containsExactly("PF1", "PF2", "PF3");
		assertThat(result.get(0)).isEqualTo(new PerformanceSummary("PF1",
				new LocalizedText("오페라의 유령", "오페라의 유령", false),
				new VenueRef("FC1", new LocalizedText("샤롯데씨어터", "샤롯데씨어터", false)), LocalDate.of(2026, 9, 1),
				LocalDate.of(2027, 2, 28), "GGGA", PerformanceStatus.ONGOING, "https://example.com/1.jpg", true,
				new Rating(new BigDecimal("4.3"), 4)));
		assertThat(result).extracting(PerformanceSummary::status)
			.containsExactly(PerformanceStatus.ONGOING, PerformanceStatus.UPCOMING, PerformanceStatus.ENDED);
		// レビューのない公演は 0 件（平均なし）
		assertThat(result.get(1).rating()).isEqualTo(new Rating(null, 0));
		assertThat(result.get(2).rating()).isEqualTo(new Rating(null, 0));
	}

	@Test
	void japaneseLooksUpTitlesAndVenueNamesOnce() {
		when(translationLookupService.lookup(any()))
			.thenReturn(Map.of("오페라의 유령", "オペラ座の怪人", "샤롯데씨어터", "シャルロッテシアター"));

		List<PerformanceSummary> result = assembler.assemble(List.of(ongoing, upcoming, ended), Lang.JA, TODAY,
				false);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Collection<String>> originals = ArgumentCaptor.forClass(Collection.class);
		verify(translationLookupService, times(1)).lookup(originals.capture());
		assertThat(originals.getValue()).containsExactlyInAnyOrder("오페라의 유령", "샤롯데씨어터", "햄릿", "번역없음", "극장");
		// withRating = false なら評価を引かず rating は null（API-20・21）
		verifyNoInteractions(reviewService);
		assertThat(result).extracting(PerformanceSummary::rating).containsOnlyNulls();

		assertThat(result.get(0).title()).isEqualTo(new LocalizedText("オペラ座の怪人", "오페라의 유령", true));
		assertThat(result.get(0).venue().name()).isEqualTo(new LocalizedText("シャルロッテシアター", "샤롯데씨어터", true));
		// 翻訳がなければ原文で translated = false
		assertThat(result.get(2).title()).isEqualTo(new LocalizedText("번역없음", "번역없음", false));
		assertThat(result.get(2).venue()).isEqualTo(new VenueRef("FC2", new LocalizedText("극장", "극장", false)));
	}

	@Test
	void emptyRowsCallNothing() {
		assertThat(assembler.assemble(List.of(), Lang.JA, TODAY, true)).isEmpty();
		verifyNoInteractions(translationLookupService, reviewService);
	}

}
