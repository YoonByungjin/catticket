package com.catticket.ranking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;

import com.catticket.common.code.PeriodType;
import com.catticket.ranking.row.CollectionRow;
import com.catticket.ranking.row.RankingEntryRow;

/**
 * 人気順位の Mapper（mapper/ranking/RankingMapper.xml。06 の 4.4）。急上昇・ジャンル別トレンドの読みは別の Issue で足す。
 */
@Mapper
public interface RankingMapper {

	/** すでに終わった期間（末日 &lt; 今日）のうち、収集が終わっている直近の期間（03 の 4.9）。genreCode は ALL かジャンルのコード */
	Optional<CollectionRow> findLatestCompleted(PeriodType periodType, String genreCode, LocalDate today);

	/** 表示できない公演（非表示・公演の行がない）を除いた上位（03 の 9.1）。提供元の順位 → 公演 ID の順 */
	List<RankingEntryRow> findTopVisible(long collectionId, int limit);

}
