package com.catticket.collect;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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
import com.catticket.collect.client.xml.KopisPerformanceListItem;
import com.catticket.common.batch.BatchContext;
import com.catticket.common.batch.BatchRunMapper;
import com.catticket.common.batch.BatchRunRecorder;
import com.catticket.common.batch.StepResult;
import com.catticket.common.batch.StepSummary;

/**
 * B-01 の S1 公演の収集（05 の 3.3、06 の 4.9）。
 * 窓の計画 → 区間ごとに、目録の全ページ → その区間の公演を保存（1件で1トランザクション）→ 次の区間。
 * 上限・遮断で途中で止まっても読んだ区間の保存が進むよう、区間ごとに保存する。
 * 基準日の 0 時以降に収集した公演は飛ばす（続き。05 の 3.3 の 12.）。
 * すべて成功したときだけ、結果に今回の窓の末日を入れる（記録は DailyCollectJob が BatchRunRecorder で書く）。
 */
@Component
public class PerformanceCollectStep {

	public static final String NAME = "S1";

	/** 目録のページの上限（100 件 × 1000 ページ。応答が終わらないときに呼び続けないための安全弁） */
	static final int MAX_PAGES = 1000;

	/** 収集済みの確認の IN の要素数（1回の SQL を大きくしすぎない） */
	static final int LOOKUP_CHUNK = 500;

	private static final Logger log = LoggerFactory.getLogger(PerformanceCollectStep.class);

	private final KopisClient client;

	private final BatchRunMapper batchRunMapper;

	private final CollectMapper collectMapper;

	private final KopisPerformanceImporter importer;

	public PerformanceCollectStep(KopisClient client, BatchRunMapper batchRunMapper, CollectMapper collectMapper,
			KopisPerformanceImporter importer) {
		this.client = client;
		this.batchRunMapper = batchRunMapper;
		this.collectMapper = collectMapper;
		this.importer = importer;
	}

	public StepResult run(BatchContext ctx) {
		StepSummary summary = new StepSummary();
		UnitFailures failures = new UnitFailures(summary, log);
		CollectPlan plan = CollectWindows.plan(ctx.today(), batchRunMapper.findLastSuccessfulS1());
		LocalDate baseDate = CollectWindows.baseDate(ctx.today(), batchRunMapper.findLatestS1Before(ctx.jobRunId()));
		summary.add("ranges", plan.ranges().size());
		try {
			// 区間が重なって同じ公演が出たら、この実行ですでに扱った ID は飛ばす
			Set<String> handled = new HashSet<>();
			for (CollectRange range : plan.ranges()) {
				Set<String> ids = new LinkedHashSet<>();
				listRange(range, ids, summary, failures);
				ids.removeAll(handled);
				handled.addAll(ids);
				summary.add("listed", ids.size());
				importRange(ids, baseDate, ctx, summary, failures);
			}
		}
		catch (KopisStoppedException | KopisCallLimitException e) {
			// 止まっても件数と基準日を残す。窓の末日は記録しないので、次の実行が続きから取る（05 の 3.3 の 7.・12.）
			if (e instanceof KopisCallLimitException) {
				summary.increment("callLimitReached");
			}
			log.warn("S1 stopped: {} {}", BatchRunRecorder.describe(e), summary);
			return StepResult.s1Failure(summary.get("saved"), summary, BatchRunRecorder.describe(e), baseDate);
		}

		long saved = summary.get("saved");
		log.info("S1 finished: {}", summary);
		if (failures.any()) {
			// 失敗したら窓の末日を記録しない（次の実行で同じ範囲を取り直す。05 の 3.3 の 7.）
			return StepResult.s1Failure(saved, summary, failures.message(), baseDate);
		}
		return StepResult.s1Success(saved, summary, plan.windowEnd(), baseDate);
	}

	// 基準日の 0 時以降に収集した公演は、その後の変更を次の実行の afterdate で取れるので呼ばない（05 の 3.3 の 12.）
	private void importRange(Set<String> ids, LocalDate baseDate, BatchContext ctx, StepSummary summary,
			UnitFailures failures) {
		if (ids.isEmpty()) {
			return;
		}
		List<String> list = new ArrayList<>(ids);
		Set<String> collected = new HashSet<>();
		for (int i = 0; i < list.size(); i += LOOKUP_CHUNK) {
			collected.addAll(collectMapper.findKopisIdsCollectedSince(
					new ArrayList<>(list.subList(i, Math.min(i + LOOKUP_CHUNK, list.size()))),
					baseDate.atStartOfDay()));
		}
		for (String id : list) {
			if (collected.contains(id)) {
				summary.increment("alreadyCollected");
				continue;
			}
			importer.importOne(id, false, ctx.now(), summary, failures);
		}
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
