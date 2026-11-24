package com.catticket.collect;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.catticket.collect.client.KopisClient;
import com.catticket.collect.client.KopisFatalException;
import com.catticket.collect.client.KopisRetryableException;
import com.catticket.collect.client.xml.KopisPerformanceDetail;
import com.catticket.collect.client.xml.KopisVenueDetail;
import com.catticket.collect.row.PerformanceValues;
import com.catticket.common.batch.StepSummary;

/**
 * 公演1件を KOPIS から取って保存する（S1・S4 で同じ方法。05 の 3.3 の 4.・5.、3.2 の S4）。
 * 公演詳細 → （会場がなければ）施設詳細 → 保存（トランザクション）。外部の呼び出しはトランザクションの前に終える（06 の 6.1）。
 */
@Component
public class KopisPerformanceImporter {

	private static final Logger log = LoggerFactory.getLogger(KopisPerformanceImporter.class);

	private final KopisClient client;

	private final CollectMapper mapper;

	private final PerformanceSaveService saveService;

	public KopisPerformanceImporter(KopisClient client, CollectMapper mapper, PerformanceSaveService saveService) {
		this.client = client;
		this.mapper = mapper;
		this.saveService = saveService;
	}

	/**
	 * 保存しなかった理由・切り詰めなどは summary に数える。KOPIS の呼び出しの失敗は failures に数える。
	 * StepAbortedException は捕まえない（ステップを止める）。
	 *
	 * @param awarded S3 で見た受賞作なら true（S4 だけ。S1 は false）
	 */
	void importOne(String mt20id, boolean awarded, LocalDateTime now, StepSummary summary, UnitFailures failures) {
		Optional<KopisPerformanceDetail> detail;
		try {
			detail = client.getPerformance(mt20id);
		}
		catch (KopisRetryableException | KopisFatalException e) {
			failures.record("performance " + mt20id, e);
			return;
		}
		if (detail.isEmpty()) {
			// 結果コード 04（目録・順位にあるが詳細がない）。失敗にしない
			note(summary, CollectNote.of("detailNotFound", mt20id));
			return;
		}
		// 保存しない公演の会場を照会しないよう、先に値を確かめる（同じ確かめは保存の中でもう一度行う）
		Conversion<PerformanceValues> check = ProviderValues.performance(detail.get());
		if (check.skipped()) {
			check.notes().forEach(n -> note(summary, n));
			return;
		}
		String mt10id = check.value().kopisVenueId();
		KopisVenueDetail newVenue = null;
		if (mapper.findVenueByKopisId(mt10id).isEmpty()) {
			Optional<KopisVenueDetail> venue;
			try {
				venue = client.getVenue(mt10id);
			}
			catch (KopisRetryableException | KopisFatalException e) {
				failures.record("venue " + mt10id + " of " + mt20id, e);
				return;
			}
			if (venue.isEmpty()) {
				// 施設詳細が 04。venue_id は必須なので公演を保存しない
				note(summary, CollectNote.of("venueNotFound", mt20id + "|" + mt10id));
				return;
			}
			newVenue = venue.get();
		}
		SaveOutcome outcome = saveService.saveFromKopis(detail.get(), newVenue, awarded, now);
		outcome.notes().forEach(n -> note(summary, n));
		if (!outcome.saved()) {
			log.info("performance {} not saved: {}", mt20id, outcome.notes());
		}
	}

	static void note(StepSummary summary, CollectNote note) {
		summary.increment(note.name());
		if (note.detail() != null) {
			summary.addDetail(note.name(), note.detail());
		}
	}

}
