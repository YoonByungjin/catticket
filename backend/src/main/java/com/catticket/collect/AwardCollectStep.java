package com.catticket.collect;

import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.catticket.collect.client.KopisCallLimitException;
import com.catticket.collect.client.KopisClient;
import com.catticket.collect.client.KopisFatalException;
import com.catticket.collect.client.KopisPage;
import com.catticket.collect.client.KopisRetryableException;
import com.catticket.collect.client.KopisStoppedException;
import com.catticket.collect.client.xml.KopisAwardItem;
import com.catticket.common.batch.BatchContext;
import com.catticket.common.batch.BatchRunRecorder;
import com.catticket.common.batch.StepResult;
import com.catticket.common.batch.StepSummary;
import com.catticket.common.display.Period;

/**
 * B-01 の S3 受賞作の収集（05 の 3.2 の S3、06 の 4.9）。
 * S1 と同じ窓を 31 日ずつ照会し、公演 ID が一致する公演を「受賞歴あり」にする（立てるだけで消さない）。
 * 1回の照会の結果ごとに1回の UPDATE（06 の 6.4）。
 */
@Component
public class AwardCollectStep {

	public static final String NAME = "S3";

	private static final Logger log = LoggerFactory.getLogger(AwardCollectStep.class);

	private final KopisClient client;

	private final CollectMapper mapper;

	public AwardCollectStep(KopisClient client, CollectMapper mapper) {
		this.client = client;
		this.mapper = mapper;
	}

	/**
	 * @param seenIds 受賞作の公演 ID を足す（S4 の入力。06 の 5.3）。止まったときも、それまでに足した ID は残す
	 */
	public StepResult run(BatchContext ctx, Set<String> seenIds) {
		StepSummary summary = new StepSummary();
		UnitFailures failures = new UnitFailures(summary, log);
		try {
			collect(ctx, seenIds, summary, failures);
		}
		catch (KopisStoppedException | KopisCallLimitException e) {
			// 止まっても立てた分と件数を残す（06 の 4.9 の「ステップ側の扱い」）
			if (e instanceof KopisCallLimitException) {
				summary.increment("callLimitReached");
			}
			log.warn("S3 stopped: {} {}", BatchRunRecorder.describe(e), summary);
			return StepResult.failure(summary.get("marked"), summary, BatchRunRecorder.describe(e));
		}
		log.info("S3 finished: {}", summary);
		long marked = summary.get("marked");
		if (failures.any()) {
			return StepResult.failure(marked, summary, failures.message());
		}
		return StepResult.success(marked, summary);
	}

	private void collect(BatchContext ctx, Set<String> seenIds, StepSummary summary, UnitFailures failures) {
		for (Period period : CollectWindows.window(ctx.today())) {
			for (int page = 1;; page++) {
				if (page > PerformanceCollectStep.MAX_PAGES) {
					failures.record("awards " + period,
							new IllegalStateException("more than " + PerformanceCollectStep.MAX_PAGES + " pages"));
					break;
				}
				KopisPage<KopisAwardItem> result;
				try {
					result = client.awards(period.start(), period.end(), page);
				}
				catch (KopisRetryableException | KopisFatalException e) {
					failures.record("awards " + period.start() + "~" + period.end() + " page " + page, e);
					break;
				}
				Set<String> ids = new LinkedHashSet<>();
				for (KopisAwardItem item : result.items()) {
					if (ProviderValues.isValidId(item.mt20id())) {
						ids.add(item.mt20id());
					}
					else {
						KopisPerformanceImporter.note(summary,
								CollectNote.of("skippedId", ProviderValues.detailValue(item.mt20id())));
					}
				}
				summary.add("listed", ids.size());
				if (!ids.isEmpty()) {
					summary.add("marked", mapper.markAwarded(ids, ctx.now()));
					seenIds.addAll(ids);
				}
				if (result.isLast()) {
					break;
				}
			}
		}
	}

}
