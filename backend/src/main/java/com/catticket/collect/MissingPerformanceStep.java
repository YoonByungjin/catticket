package com.catticket.collect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.catticket.collect.client.KopisCallPolicy;
import com.catticket.common.batch.BatchContext;
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

	private final KopisCallPolicy policy;

	private final CollectMapper mapper;

	private final KopisPerformanceImporter importer;

	public MissingPerformanceStep(KopisCallPolicy policy, CollectMapper mapper, KopisPerformanceImporter importer) {
		this.policy = policy;
		this.mapper = mapper;
		this.importer = importer;
	}

	/**
	 * @param seenIds S2・S3 で見た公演 ID（DailyCollectJob がメモリで渡す。06 の 5.3）
	 * @throws com.catticket.collect.client.StepAbortedException KOPIS の呼び出しが続けて失敗した（捕まえずに外へ出す）
	 */
	public StepResult run(BatchContext ctx, Set<String> seenIds) {
		policy.startStep();
		StepSummary summary = new StepSummary();
		UnitFailures failures = new UnitFailures(summary, log);
		List<String> candidates = seenIds.stream().filter(ProviderValues::isValidId).distinct().toList();
		summary.add("candidates", candidates.size());

		Set<String> existing = new HashSet<>();
		for (int i = 0; i < candidates.size(); i += LOOKUP_CHUNK) {
			existing.addAll(mapper
				.findExistingKopisIds(new ArrayList<>(candidates.subList(i, Math.min(i + LOOKUP_CHUNK, candidates.size())))));
		}
		Set<String> missing = new LinkedHashSet<>(candidates);
		missing.removeAll(existing);
		summary.add("missing", missing.size());

		for (String id : missing) {
			importer.importOne(id, ctx.now(), summary, failures);
		}
		log.info("S4 finished: {}", summary);
		long saved = summary.get("saved");
		if (failures.any()) {
			return StepResult.failure(saved, summary, failures.message());
		}
		return StepResult.success(saved, summary);
	}

}
