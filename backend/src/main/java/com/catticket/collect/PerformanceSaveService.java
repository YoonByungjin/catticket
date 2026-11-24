package com.catticket.collect;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.catticket.collect.client.xml.KopisPerformanceDetail;
import com.catticket.collect.client.xml.KopisVenueDetail;
import com.catticket.collect.row.CollectVenueRow;
import com.catticket.collect.row.PerformanceValues;
import com.catticket.collect.row.VenueValues;

/**
 * S1・S4 の公演1件の保存（06 の 4.9・6.4）。公演1件で1トランザクション（会場の保存も同じ）。
 * 外部の呼び出し（施設詳細）は呼ぶ側がトランザクションの前に済ませて渡す（06 の 6.1）。
 */
@Service
public class PerformanceSaveService {

	/** 自動収集の予約サイト（04 の booking_site.source） */
	static final String COLLECTED = "COLLECTED";

	private final CollectMapper mapper;

	public PerformanceSaveService(CollectMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * 自動収集の値を追加・更新する。管理者の修正（performance_override）・is_hidden・has_award は書き換えない。
	 * 予約サイトは COLLECTED の行だけを入れ替える（ADMIN の行は変えない）。
	 *
	 * @param newVenue 会場がまだ DB にないときの施設詳細（あるときは null）
	 */
	@Transactional
	public SaveOutcome saveFromKopis(KopisPerformanceDetail detail, KopisVenueDetail newVenue, LocalDateTime now) {
		Conversion<PerformanceValues> performance = ProviderValues.performance(detail);
		List<CollectNote> notes = new ArrayList<>(performance.notes());
		if (performance.skipped()) {
			return new SaveOutcome(false, notes);
		}
		PerformanceValues values = performance.value();
		if (newVenue != null) {
			Conversion<VenueValues> venue = ProviderValues.venue(values.kopisVenueId(), newVenue);
			notes.addAll(venue.notes());
			if (venue.skipped()) {
				return new SaveOutcome(false, notes);
			}
			mapper.upsertVenue(venue.value(), now);
			notes.add(CollectNote.of("venueSaved"));
		}
		Optional<CollectVenueRow> venueRow = mapper.findVenueByKopisId(values.kopisVenueId());
		if (venueRow.isEmpty()) {
			// 呼ぶ側が施設詳細を渡さなかった（DB にあると読んだ後に消えた）。venue_id は必須なので保存しない
			notes.add(CollectNote.of("venueNotFound", values.kopisPerformanceId() + "|" + values.kopisVenueId()));
			return new SaveOutcome(false, notes);
		}
		boolean seoul = values.areaSeoul() || ProviderValues.isSeoulAddress(venueRow.get().address());
		mapper.upsertPerformance(values, venueRow.get().id(), seoul, now);
		long performanceId = mapper.findPerformanceIdByKopisId(values.kopisPerformanceId())
			.orElseThrow(() -> new IllegalStateException("performance not found after upsert: "
					+ values.kopisPerformanceId()));
		// 同じ照会の結果なら何度実行しても同じ行になる（05 の 3.3 の 9.）
		mapper.deleteBookingSites(performanceId, COLLECTED);
		if (!values.bookingSites().isEmpty()) {
			mapper.insertBookingSites(performanceId, COLLECTED, values.bookingSites(), now);
		}
		notes.add(CollectNote.of("saved"));
		return new SaveOutcome(true, notes);
	}

}
