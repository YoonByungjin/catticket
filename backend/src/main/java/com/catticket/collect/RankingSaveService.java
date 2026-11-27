package com.catticket.collect;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.catticket.collect.row.RankingCollectionKey;
import com.catticket.collect.row.RankingEntryInsert;

/**
 * S2 の組1つの保存（05 の 3.4、06 の 6.4）。行の入れ替えと「収集の完了の記録」を同じトランザクションで行う
 * （行だけ新しく記録が古い状態を API が読まないため）。
 */
@Service
public class RankingSaveService {

	private final CollectMapper mapper;

	public RankingSaveService(CollectMapper mapper) {
		this.mapper = mapper;
	}

	/** 0 件も完了として記録する（全体が 1 件以上の期間のジャンルだけ。呼ぶ側が決める） */
	@Transactional
	public void replace(RankingCollectionKey key, List<RankingEntryInsert> entries, LocalDateTime now) {
		mapper.upsertRankingCollection(key, entries.size(), now);
		long collectionId = mapper.findCollectionId(key)
			.orElseThrow(() -> new IllegalStateException("ranking_collection not found after upsert: " + key));
		mapper.deleteRankingEntries(collectionId);
		if (!entries.isEmpty()) {
			mapper.insertRankingEntries(collectionId, entries, now);
		}
	}

}
