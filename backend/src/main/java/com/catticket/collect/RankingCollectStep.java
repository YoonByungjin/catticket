package com.catticket.collect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.catticket.collect.client.KopisCallPolicy;
import com.catticket.collect.client.KopisClient;
import com.catticket.collect.client.KopisFatalException;
import com.catticket.collect.client.KopisRetryableException;
import com.catticket.collect.client.xml.KopisBoxOfficeItem;
import com.catticket.collect.row.RankingCollectionKey;
import com.catticket.collect.row.RankingEntryInsert;
import com.catticket.common.batch.BatchContext;
import com.catticket.common.batch.StepResult;
import com.catticket.common.batch.StepSummary;
import com.catticket.common.code.Genre;
import com.catticket.ranking.RankingPeriods;
import com.catticket.ranking.RankingTarget;

/**
 * B-01 の S2 人気順位の収集（05 の 3.4、06 の 4.9）。
 * 6 期間それぞれで全体を先に照会し、0 件なら未集計（何も保存しない）、1 件以上なら全体と 9 ジャンルを組ごとに入れ替える。
 */
@Component
public class RankingCollectStep {

	public static final String NAME = "S2";

	private static final Logger log = LoggerFactory.getLogger(RankingCollectStep.class);

	private final KopisCallPolicy policy;

	private final KopisClient client;

	private final RankingSaveService saveService;

	public RankingCollectStep(KopisCallPolicy policy, KopisClient client, RankingSaveService saveService) {
		this.policy = policy;
		this.client = client;
		this.saveService = saveService;
	}

	/**
	 * @param seenIds 保存した順位の行の公演 ID を足す（S4 の入力。DailyCollectJob がメモリで渡す。06 の 5.3）
	 * @throws com.catticket.collect.client.StepAbortedException KOPIS の呼び出しが続けて失敗した（捕まえずに外へ出す）
	 */
	public StepResult run(BatchContext ctx, Set<String> seenIds) {
		policy.startStep();
		StepSummary summary = new StepSummary();
		UnitFailures failures = new UnitFailures(summary, log);
		for (RankingTarget target : RankingPeriods.collectTargets(ctx.today())) {
			RankingCollectionKey all = new RankingCollectionKey(target.type(), RankingCollectionKey.ALL,
					target.period());
			Optional<List<KopisBoxOfficeItem>> overall = query(all, Optional.empty(), failures);
			if (overall.isEmpty()) {
				// 全体が失敗した期間は、全体が 0 件か分からないのでジャンルも照会しない（05 の 3.4）
				continue;
			}
			if (overall.get().isEmpty()) {
				// 集計の前と 0 件は同じ空の応答なので未集計とみなす（1つ前の期間が表示される。R-62）
				summary.increment("notYetAvailable");
				summary.addDetail("notYetAvailable", target.type() + "|" + target.period().start());
				continue;
			}
			save(all, overall.get(), ctx, summary, seenIds);
			for (Genre genre : Genre.values()) {
				RankingCollectionKey key = new RankingCollectionKey(target.type(), genre.code(), target.period());
				// その期間に集計があることは全体で分かっているので、ジャンルの 0 件は 0 件として完了を記録する
				query(key, Optional.of(genre), failures).ifPresent(items -> save(key, items, ctx, summary, seenIds));
			}
		}
		log.info("S2 finished: {}", summary);
		long collected = summary.get("collected");
		if (failures.any()) {
			return StepResult.failure(collected, summary, failures.message());
		}
		return StepResult.success(collected, summary);
	}

	private Optional<List<KopisBoxOfficeItem>> query(RankingCollectionKey key, Optional<Genre> genre,
			UnitFailures failures) {
		try {
			return Optional.of(client.boxOffice(key.period(), genre));
		}
		catch (KopisRetryableException | KopisFatalException e) {
			// 失敗した組は行も記録も変えない（前の実行で保存した内容が残る）
			failures.record("ranking " + key, e);
			return Optional.empty();
		}
	}

	private void save(RankingCollectionKey key, List<KopisBoxOfficeItem> items, BatchContext ctx, StepSummary summary,
			Set<String> seenIds) {
		List<RankingEntryInsert> entries = toEntries(key, items, summary);
		saveService.replace(key, entries, ctx.now());
		summary.increment("collected");
		summary.add("rows", entries.size());
		entries.forEach(e -> seenIds.add(e.kopisPerformanceId()));
	}

	// 順位・公演 ID が使えない行と、同じ組の同じ公演の2行目は保存しない（一意制約に当たる前に記録して除く）
	static List<RankingEntryInsert> toEntries(RankingCollectionKey key, List<KopisBoxOfficeItem> items,
			StepSummary summary) {
		List<RankingEntryInsert> entries = new ArrayList<>();
		Set<String> ids = new HashSet<>();
		for (KopisBoxOfficeItem item : items) {
			Integer rank = rank(item.rnum());
			if (rank == null || !ProviderValues.isValidId(item.mt20id()) || !ids.add(item.mt20id())) {
				KopisPerformanceImporter.note(summary, CollectNote.of("skippedRow",
						key + "|" + ProviderValues.detailValue(item.rnum()) + "|"
								+ ProviderValues.detailValue(item.mt20id())));
				continue;
			}
			entries.add(new RankingEntryInsert(rank, item.mt20id()));
		}
		return entries;
	}

	private static Integer rank(String rnum) {
		if (rnum == null) {
			return null;
		}
		try {
			int value = Integer.parseInt(rnum);
			return value >= 1 ? value : null;
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

}
