package com.catticket.collect;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.ibatis.annotations.Mapper;

import com.catticket.collect.row.BookingSiteValue;
import com.catticket.collect.row.CollectVenueRow;
import com.catticket.collect.row.GenreTrendRow;
import com.catticket.collect.row.PerformanceValues;
import com.catticket.collect.row.RankingCollectionKey;
import com.catticket.collect.row.RankingEntryInsert;
import com.catticket.collect.row.VenueValues;
import com.catticket.common.display.Period;

/**
 * B-01 の S1〜S4・S6 の保存（mapper/collect/CollectMapper.xml。06 の 4.9）。
 * 自動収集の値だけを書く（管理者の修正は performance_override にあり、触れない）。
 */
@Mapper
public interface CollectMapper {

	/** 会場の id と住所（住所はソウルの公演の判定に使う） */
	Optional<CollectVenueRow> findVenueByKopisId(String kopisVenueId);

	void upsertVenue(VenueValues venue, LocalDateTime now);

	Optional<Long> findPerformanceIdByKopisId(String kopisPerformanceId);

	/** is_hidden・has_award・created_at は更新しない */
	void upsertPerformance(PerformanceValues performance, long venueId, boolean seoul, LocalDateTime now);

	int deleteBookingSites(long performanceId, String source);

	/** sites が空なら呼ばない（空の VALUES は SQL の誤り） */
	void insertBookingSites(long performanceId, String source, List<BookingSiteValue> sites, LocalDateTime now);

	/** 受賞歴ありにする（立てるだけ）。kopisIds が空なら呼ばない。戻り値は変わった行の数 */
	int markAwarded(Collection<String> kopisIds, LocalDateTime now);

	/** kopisIds のうち performance にあるもの。空なら呼ばない */
	Set<String> findExistingKopisIds(Collection<String> kopisIds);

	Optional<Long> findCollectionId(RankingCollectionKey key);

	void upsertRankingCollection(RankingCollectionKey key, int rowCount, LocalDateTime now);

	int deleteRankingEntries(long collectionId);

	/** entries が空なら呼ばない */
	void insertRankingEntries(long collectionId, List<RankingEntryInsert> entries, LocalDateTime now);

	int deleteGenreTrends(Period period);

	/** rows が空なら呼ばない */
	void insertGenreTrends(Period period, List<GenreTrendRow> rows, GenreTrendBasis basis, LocalDateTime now);

}
