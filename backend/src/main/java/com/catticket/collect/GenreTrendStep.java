package com.catticket.collect;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.catticket.collect.client.KopisCallLimitException;
import com.catticket.collect.client.KopisClient;
import com.catticket.collect.client.KopisFatalException;
import com.catticket.collect.client.KopisRetryableException;
import com.catticket.collect.client.KopisStoppedException;
import com.catticket.collect.client.xml.KopisGenreStat;
import com.catticket.collect.row.GenreTrendRow;
import com.catticket.common.batch.BatchContext;
import com.catticket.common.batch.BatchRunRecorder;
import com.catticket.common.batch.StepResult;
import com.catticket.common.batch.StepSummary;
import com.catticket.common.display.Period;

/**
 * B-01 の S6 ジャンル別トレンドの収集（05 の 3.2 の「S6 の詳細」、06 の 4.9）。
 * 前日までの 30 日を1回照会し、全国基準で保存する（ソウルで絞れないため）。単位はステップ全体。
 */
@Component
public class GenreTrendStep {

	public static final String NAME = "S6";

	private static final Logger log = LoggerFactory.getLogger(GenreTrendStep.class);

	private final KopisClient client;

	private final GenreTrendSaveService saveService;

	public GenreTrendStep(KopisClient client, GenreTrendSaveService saveService) {
		this.client = client;
		this.saveService = saveService;
	}

	public StepResult run(BatchContext ctx) {
		StepSummary summary = new StepSummary();
		Period period = new Period(ctx.today().minusDays(30), ctx.today().minusDays(1));
		List<KopisGenreStat> stats;
		try {
			stats = client.genreStats(period);
		}
		catch (KopisRetryableException | KopisFatalException e) {
			log.warn("S6 failed: {}", BatchRunRecorder.describe(e));
			return StepResult.failure(summary, BatchRunRecorder.describe(e));
		}
		catch (KopisStoppedException | KopisCallLimitException e) {
			// 単位はステップ全体なので、何も保存せずに失敗にする（06 の 4.9 の「ステップ側の扱い」）
			if (e instanceof KopisCallLimitException) {
				summary.increment("callLimitReached");
			}
			log.warn("S6 stopped: {}", BatchRunRecorder.describe(e));
			return StepResult.failure(0, summary, BatchRunRecorder.describe(e));
		}
		List<GenreTrendRow> rows;
		try {
			rows = GenreTrendCalculator.calculate(stats);
		}
		catch (InvalidGenreStatException e) {
			// 何も保存しない（前の期間の行を残す）
			log.warn("S6 failed: {}", e.getMessage());
			return StepResult.failure(summary, e.getMessage());
		}
		if (rows.isEmpty()) {
			// 合計 0 は割合を計算できない。保存せず記録に残す（ステップは成功）
			summary.increment("zeroTotal");
			log.info("S6 finished: {} (zero total for {}~{})", summary, period.start(), period.end());
			return StepResult.success(0, summary);
		}
		saveService.replace(period, rows, GenreTrendBasis.NATIONAL, ctx.now());
		summary.add("saved", rows.size());
		log.info("S6 finished: {}", summary);
		return StepResult.success(rows.size(), summary);
	}

}
