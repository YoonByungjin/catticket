package com.catticket.collect;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.catticket.collect.row.GenreTrendRow;
import com.catticket.common.display.Period;

/**
 * S6 の集計期間1つの保存（05 の 3.2、06 の 6.4）。その期間の行を全部入れ替える。
 */
@Service
public class GenreTrendSaveService {

	private final CollectMapper mapper;

	public GenreTrendSaveService(CollectMapper mapper) {
		this.mapper = mapper;
	}

	/** rows は空でないこと（合計 0 のときは呼ばずに前の期間の行を残す） */
	@Transactional
	public void replace(Period period, List<GenreTrendRow> rows, GenreTrendBasis basis, LocalDateTime now) {
		if (rows.isEmpty()) {
			throw new IllegalArgumentException("rows must not be empty");
		}
		mapper.deleteGenreTrends(period);
		mapper.insertGenreTrends(period, rows, basis, now);
	}

}
