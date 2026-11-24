package com.catticket.collect;

import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.catticket.collect.client.KopisCallPolicy;
import com.catticket.collect.client.KopisClient;
import com.catticket.collect.client.KopisFatalException;
import com.catticket.collect.client.KopisPage;
import com.catticket.collect.client.KopisRetryableException;
import com.catticket.collect.client.xml.KopisPerformanceListItem;
import com.catticket.common.batch.BatchContext;
import com.catticket.common.batch.BatchRunMapper;
import com.catticket.common.batch.StepResult;
import com.catticket.common.batch.StepSummary;

/**
 * B-01 の S1 公演の収集（05 の 3.3、06 の 4.9）。
 * 窓の計画 → 区間ごとに目録の全ページ → 公演ごとに保存（1件で1トランザクション）。
 * すべて成功したときだけ、結果に今回の窓の末日を入れる（記録は DailyCollectJob が BatchRunRecorder で書く）。
 */
@Component
public class PerformanceCollectStep {

	public static final String NAME = "S1";

	/** 目録のページの上限（100 件 × 1000 ページ。応答が終わらないときに呼び続けないための安全弁） */
	static final int MAX_PAGES = 1000;

	private static final Logger log = LoggerFactory.getLogger(PerformanceCollectStep.class);

	private final KopisCallPolicy policy;

	private final KopisClient client;

	private final BatchRunMapper batchRunMapper;

	private final KopisPerformanceImporter importer;

	public PerformanceCollectStep(KopisCallPolicy policy, KopisClient client, BatchRunMapper batchRunMapper,
			KopisPerformanceImporter importer) {
		this.policy = policy;
		this.client = client;
		this.batchRunMapper = batchRunMapper;
		this.importer = importer;
	}

	/**
	 * @throws com.catticket.collect.client.StepAbortedException KOPIS の呼び出しが続けて失敗した（捕まえずに外へ出す）
	 */
	public StepResult run(BatchContext ctx) {
		// 前のステップの続けての失敗の数を引き継がない（06 の 4.9）
		policy.startStep();
		StepSummary summary = new StepSummary();
		UnitFailures failures = new UnitFailures(summary, log);
		CollectPlan plan = CollectWindows.plan(ctx.today(), batchRunMapper.findLastSuccessfulS1());
		summary.add("ranges", plan.ranges().size());

		Set<String> ids = new LinkedHashSet<>();
		for (CollectRange range : plan.ranges()) {
			listRange(range, ids, summary, failures);
		}
		summary.add("listed", ids.size());

		for (String id : ids) {
			importer.importOne(id, ctx.now(), summary, failures);
		}

		long saved = summary.get("saved");
		log.info("S1 finished: {}", summary);
		if (failures.any()) {
			// 失敗したら窓の末日を記録しない（次の実行で同じ範囲を取り直す。05 の 3.3 の 7.）
			return StepResult.failure(saved, summary, failures.message());
		}
		return StepResult.success(saved, summary, plan.windowEnd());
	}

	// 区間の目録を最後のページまで読む。途中で失敗したら、その区間は失敗（読めた分の公演は保存する）
	private void listRange(CollectRange range, Set<String> ids, StepSummary summary, UnitFailures failures) {
		for (int page = 1;; page++) {
			if (page > MAX_PAGES) {
				failures.record("range " + range,
						new IllegalStateException("more than " + MAX_PAGES + " pages"));
				return;
			}
			KopisPage<KopisPerformanceListItem> result;
			try {
				result = client.listPerformances(range.period().start(), range.period().end(), range.afterDate(),
						page);
			}
			catch (KopisRetryableException | KopisFatalException e) {
				failures.record("range " + range + " page " + page, e);
				return;
			}
			summary.increment("pages");
			for (KopisPerformanceListItem item : result.items()) {
				if (ProviderValues.isValidId(item.mt20id())) {
					ids.add(item.mt20id());
				}
				else {
					KopisPerformanceImporter.note(summary, CollectNote.of("skippedId", ProviderValues.detailValue(item.mt20id())));
				}
			}
			if (result.isLast()) {
				return;
			}
		}
	}

}
