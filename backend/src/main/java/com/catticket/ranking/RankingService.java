package com.catticket.ranking;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.catticket.common.code.Lang;
import com.catticket.common.code.PeriodType;
import com.catticket.common.display.PerformanceSummary;
import com.catticket.common.display.PerformanceSummaryRow;
import com.catticket.common.time.DateTimes;
import com.catticket.ranking.dto.RankingBadge;
import com.catticket.ranking.dto.RankingItem;
import com.catticket.ranking.dto.RankingResponse;
import com.catticket.ranking.row.CollectionRow;
import com.catticket.ranking.row.RankingEntryRow;
import com.catticket.summary.PerformanceSummaryAssembler;

/**
 * 人気順位（06 の 4.4）。
 */
@Service
public class RankingService {

	/** 全体の順位のジャンルのコード（04 の ranking_collection.genre_code） */
	static final String GENRE_ALL = "ALL";

	/** 表示する上位の数（01 の F-29） */
	static final int TOP_SIZE = 10;

	private final RankingMapper rankingMapper;

	private final PerformanceSummaryAssembler summaryAssembler;

	private final Clock clock;

	public RankingService(RankingMapper rankingMapper, PerformanceSummaryAssembler summaryAssembler, Clock clock) {
		this.rankingMapper = rankingMapper;
		this.summaryAssembler = summaryAssembler;
		this.clock = clock;
	}

	/**
	 * API-20（03 の API-20）。表示する期間（03 の 4.9）の、振り直した上位 10 位（03 の 9.1）。
	 *
	 * @param genre ALL かジャンルのコード（Controller で検証済み）
	 * @return 表示できる期間がなければ period = null、items = []
	 */
	@Transactional(readOnly = true)
	public RankingResponse getRanking(PeriodType type, String genre, Lang lang) {
		LocalDate today = DateTimes.today(clock);
		Optional<VisibleTop> top = findVisibleTop(type, genre, today);
		if (top.isEmpty()) {
			return new RankingResponse(type, genre, null, List.of());
		}
		List<VisibleTop.Ranked> entries = top.get().entries();
		List<PerformanceSummaryRow> rows = entries.stream().map(e -> e.row().toSummaryRow()).toList();
		// SC-03 のカードは評価を除く（03 の 7.2）ので withRating = false
		List<PerformanceSummary> summaries = summaryAssembler.assemble(rows, lang, today, false);
		List<RankingItem> items = new ArrayList<>(entries.size());
		for (int i = 0; i < entries.size(); i++) {
			items.add(new RankingItem(entries.get(i).rank(), summaries.get(i)));
		}
		return new RankingResponse(type, genre, top.get().period(), List.copyOf(items));
	}

	/**
	 * API-03 の rankings（03 の 4.3）。3 期間 × 全体・その公演のジャンルの組で API-20 と同じ上位 10 位を求め、入っているものだけ返す。
	 *
	 * @param performanceId KOPIS の公演 ID
	 * @param genre その公演の、修正を反映したジャンルのコード
	 * @return 並び順は DAILY → WEEKLY → MONTHLY、それぞれ ALL → ジャンル（03 の API-03）
	 */
	@Transactional(readOnly = true)
	public List<RankingBadge> findRankingsOf(String performanceId, String genre, LocalDate today) {
		List<RankingBadge> badges = new ArrayList<>();
		for (PeriodType type : PeriodType.values()) {
			for (String genreCode : List.of(GENRE_ALL, genre)) {
				findVisibleTop(type, genreCode, today).ifPresent(top -> top.entries()
					.stream()
					.filter(e -> e.row().performanceId().equals(performanceId))
					.findFirst()
					.ifPresent(e -> badges.add(new RankingBadge(type, genreCode, e.rank(), top.period()))));
			}
		}
		return badges;
	}

	/**
	 * 表示する期間の上位 10 位（API-20 の getRanking と共通）。表示できる期間がなければ空。
	 * 除く行は SQL で落としているので、並んだ順に 1 から振る（同じ提供元の順位でも別の番号。03 の 9.1）。
	 */
	Optional<VisibleTop> findVisibleTop(PeriodType type, String genreCode, LocalDate today) {
		Optional<CollectionRow> collection = rankingMapper.findLatestCompleted(type, genreCode, today);
		if (collection.isEmpty()) {
			return Optional.empty();
		}
		List<RankingEntryRow> rows = rankingMapper.findTopVisible(collection.get().id(), TOP_SIZE);
		List<VisibleTop.Ranked> ranked = new ArrayList<>(rows.size());
		for (int i = 0; i < rows.size(); i++) {
			ranked.add(new VisibleTop.Ranked(i + 1, rows.get(i)));
		}
		return Optional.of(new VisibleTop(collection.get().period(), List.copyOf(ranked)));
	}

}
