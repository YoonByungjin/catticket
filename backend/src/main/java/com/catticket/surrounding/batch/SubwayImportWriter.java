package com.catticket.surrounding.batch;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 駅・コインロッカーを1つのトランザクションで全部入れ替える（05 の 4.3 の 5.、06 の 6.4）。
 * 失敗したら戻るので、前の版のデータが残る。
 */
@Service
public class SubwayImportWriter {

	private final SubwayImportMapper mapper;

	public SubwayImportWriter(SubwayImportMapper mapper) {
		this.mapper = mapper;
	}

	@Transactional
	public void replaceAll(SubwayImportPlan plan, LocalDateTime now) {
		mapper.deleteAllLockers();
		mapper.deleteAllStations();
		// 空の VALUES は SQL の誤りになるので、0 件なら INSERT しない
		if (!plan.stations().isEmpty()) {
			mapper.insertStations(plan.stations(), now);
		}
		if (!plan.lockers().isEmpty()) {
			mapper.insertLockers(plan.lockers(), now);
		}
	}

}
