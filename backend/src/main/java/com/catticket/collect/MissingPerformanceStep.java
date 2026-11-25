package com.catticket.collect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.catticket.collect.client.KopisCallLimitException;
import com.catticket.collect.client.KopisStoppedException;
import com.catticket.common.batch.BatchContext;
import com.catticket.common.batch.BatchRunRecorder;
import com.catticket.common.batch.StepResult;
import com.catticket.common.batch.StepSummary;

/**
 * B-01 の S4 未収集の公演の補完（05 の 3.2 の S4、06 の 4.9）。
 * S2・S3 で見た公演 ID のうち DB にないものを、S1 と同じ方法で保存する（終わった公演も。ADR 0008 2.）。
 */
@Component
public class MissingPerformanceStep {

	public static final String NAME = "S4";

	/** 存在の確認の IN の要素数（1回の SQL を大きくしすぎない） */
	static final int LOOKUP_CHUNK = 500;

	private static final Logger log = LoggerFactory.getLogger(MissingPerformanceStep.class);

	private final CollectMapper mapper;

	private final KopisPerformanceImporter importer;

	public MissingPerformanceStep(CollectMapper mapper, KopisPerformanceImporter importer) {
		this.mapper = mapper;
		this.importer = importer;
	}

	/**
	 * S2・S3 で見た公演 ID は DailyCollectJob がメモリで渡す（06 の 5.3）。受賞作かどうかを保存に渡すため、2つに分けて受ける。
	 *
	 * @param rankingIds S2 で見た公演 ID
	 * @param awardIds S3 で見た受賞作の公演 ID
	 */
	public StepResult run(BatchContext ctx, Set<String> rankingIds, Set<String> awardIds) {
		StepSummary summary = new StepSummary();
		UnitFailures failures = new UnitFailures(summary, log);
		// rankingIds の順、続けて awardIds（06 の 4.9）
		List<String> candidates = Stream.concat(rankingIds.stream(), awardIds.stream())
			.filter(ProviderValues::isValidId)
			.distinct()
			.toList();
		summary.add("candidates", candidates.size());

		Set<String> existing = new HashSet<>();
		for (int i = 0; i < candidates.size(); i += LOOKUP_CHUNK) {
			existing.addAll(mapper
				.findExistingKopisIds(new ArrayList<>(candidates.subList(i, Math.min(i + LOOKUP_CHUNK, candidates.size())))));
		}
		Set<String> missing = new LinkedHashSet<>(candidates);
		missing.removeAll(existing);
		summary.add("missing", missing.size());

		try {
			for (String id : missing) {
				importer.importOne(id, awardIds.contains(id), ctx.now(), summary, failures);
			}
		}
		catch (KopisStoppedException | KopisCallLimitException e) {
			// 止まっても保存した分と件数を残す。残りは翌日の S4 が取る（05 の 3.5）
			if (e instanceof KopisCallLimitException) {
				summary.increment("callLimitReached");
			}
			log.warn("S4 stopped: {} {}", BatchRunRecorder.describe(e), summary);
			return StepResult.failure(summary.get("saved"), summary, BatchRunRecorder.describe(e));
		}
		log.info("S4 finished: {}", summary);
		long saved = summary.get("saved");
		if (failures.any()) {
			return StepResult.failure(saved, summary, failures.message());
		}
		return StepResult.success(saved, summary);
	}

}
