package com.catticket.surrounding.batch;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;

/**
 * 駅・コインロッカーの入れ替え（mapper/surrounding/SubwayImportMapper.xml。06 の 5.4）。
 */
@Mapper
public interface SubwayImportMapper {

	int deleteAllLockers();

	int deleteAllStations();

	/** 複数行の INSERT を1回の文で（約 270 行） */
	int insertStations(List<StationInsert> stations, LocalDateTime now);

	/** station_id は外部の駅コードから副問い合わせで求める */
	int insertLockers(List<LockerInsert> lockers, LocalDateTime now);

}
