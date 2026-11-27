package com.catticket.summary;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.catticket.common.code.Lang;
import com.catticket.common.display.LocalizedTextFactory;
import com.catticket.common.display.PerformanceStatus;
import com.catticket.common.display.PerformanceSummary;
import com.catticket.common.display.PerformanceSummaryRow;
import com.catticket.common.display.Rating;
import com.catticket.common.display.VenueRef;
import com.catticket.review.RatingStat;
import com.catticket.review.ReviewService;
import com.catticket.translation.TranslationLookupService;

/**
 * 公演のカード（03 の 7.2 の PerformanceSummary）の組み立て（06 の 3.5）。
 * 翻訳と評価は行全体で1回ずつまとめて引く（1行ずつ引くと N+1 になる）。
 */
@Component
public class PerformanceSummaryAssembler {

	private final TranslationLookupService translationLookupService;

	private final ReviewService reviewService;

	public PerformanceSummaryAssembler(TranslationLookupService translationLookupService,
			ReviewService reviewService) {
		this.translationLookupService = translationLookupService;
		this.reviewService = reviewService;
	}

	/**
	 * @param withRating false なら rating は null（API-20・21。02 の SC-03 のカードは評価を除く）
	 * @return 行の順番のまま
	 */
	public List<PerformanceSummary> assemble(List<PerformanceSummaryRow> rows, Lang lang, LocalDate today,
			boolean withRating) {
		if (rows.isEmpty()) {
			return List.of();
		}
		// 翻訳は lang = ja のときだけ引く（06 の 4.10）
		Map<String, String> translations = Map.of();
		if (lang == Lang.JA) {
			Set<String> originals = new LinkedHashSet<>();
			for (PerformanceSummaryRow row : rows) {
				originals.add(row.title());
				originals.add(row.venueName());
			}
			translations = translationLookupService.lookup(originals);
		}
		Map<Long, RatingStat> stats = Map.of();
		if (withRating) {
			Set<Long> ids = new LinkedHashSet<>();
			for (PerformanceSummaryRow row : rows) {
				ids.add(row.id());
			}
			stats = reviewService.findRatingStats(ids);
		}
		List<PerformanceSummary> result = new ArrayList<>(rows.size());
		for (PerformanceSummaryRow row : rows) {
			Rating rating = null;
			if (withRating) {
				// レビューのない公演は stats に入らないので 0 件
				RatingStat stat = stats.get(row.id());
				rating = stat == null ? Rating.of(0, 0) : Rating.of(stat.sum(), stat.count());
			}
			result.add(new PerformanceSummary(row.performanceId(),
					LocalizedTextFactory.of(row.title(), lang, translations),
					new VenueRef(row.venueId(), LocalizedTextFactory.of(row.venueName(), lang, translations)),
					row.startDate(), row.endDate(), row.genreCode(),
					PerformanceStatus.of(row.startDate(), row.endDate(), today), row.posterUrl(), row.hasAward(),
					rating));
		}
		return result;
	}

}
