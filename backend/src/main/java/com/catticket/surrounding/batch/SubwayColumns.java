package com.catticket.surrounding.batch;

import java.util.List;

/**
 * 4つのファイルの列の名前（05 の 4.2 の表）。列は見出しの名前で取る（順番に頼らない）。
 */
public final class SubwayColumns {

	// 共通
	public static final String SEQ = "연번";

	public static final String LINE = "호선";

	public static final String STATION_NAME = "역명";

	// 駅の座標
	public static final String LOCATION_CODE = "고유역번호(외부역코드)";

	public static final String LATITUDE = "위도";

	public static final String LONGITUDE = "경도";

	public static final String OPENED_DATE = "작성일자";

	public static final String BASE_DATE = "작성기준일";

	// 駅の住所
	public static final String ADDRESS_CODE = "역번호";

	public static final String PHONE = "역전화번호";

	public static final String ROAD_ADDRESS = "도로명주소";

	public static final String LOT_ADDRESS = "지번주소";

	// コインロッカー
	public static final String LOCKER_NAME = "보관함명";

	public static final String LOCKER_LOCATION = "상세위치";

	// 駅名の多言語表記
	public static final String NAME_KO = "한글";

	public static final String NAME_HANJA = "한자";

	public static final String NAME_EN = "영문";

	public static final String NAME_ZH = "중국어";

	public static final String NAME_JA = "일본어";

	/** 見出しになければ失敗にする列（05 の 4.3 の 1.。多言語表記の名前のない8列目は除く） */
	public static final List<String> LOCATION_REQUIRED = List.of(SEQ, LINE, LOCATION_CODE, STATION_NAME, LATITUDE,
			LONGITUDE, OPENED_DATE, BASE_DATE);

	public static final List<String> ADDRESS_REQUIRED = List.of(SEQ, ADDRESS_CODE, LINE, STATION_NAME, PHONE,
			ROAD_ADDRESS, LOT_ADDRESS);

	public static final List<String> LOCKER_REQUIRED = List.of(SEQ, LINE, LOCKER_NAME, LOCKER_LOCATION);

	public static final List<String> NAME_REQUIRED = List.of(SEQ, LINE, NAME_KO, NAME_HANJA, NAME_EN, NAME_ZH,
			NAME_JA);

	private SubwayColumns() {
	}

}
