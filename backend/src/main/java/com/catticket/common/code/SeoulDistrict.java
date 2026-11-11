package com.catticket.common.code;

import java.util.Arrays;
import java.util.Optional;

/**
 * ソウルの 25 区（03 の 4.2）。定数の名前はローマ字で、API・DB（{@code venue.district}）では韓国語の名前を使う。
 */
public enum SeoulDistrict implements ApiCode {

	JONGNO("종로구"),
	JUNG("중구"),
	YONGSAN("용산구"),
	SEONGDONG("성동구"),
	GWANGJIN("광진구"),
	DONGDAEMUN("동대문구"),
	JUNGNANG("중랑구"),
	SEONGBUK("성북구"),
	GANGBUK("강북구"),
	DOBONG("도봉구"),
	NOWON("노원구"),
	EUNPYEONG("은평구"),
	SEODAEMUN("서대문구"),
	MAPO("마포구"),
	YANGCHEON("양천구"),
	GANGSEO("강서구"),
	GURO("구로구"),
	GEUMCHEON("금천구"),
	YEONGDEUNGPO("영등포구"),
	DONGJAK("동작구"),
	GWANAK("관악구"),
	SEOCHO("서초구"),
	GANGNAM("강남구"),
	SONGPA("송파구"),
	GANGDONG("강동구");

	private final String koreanName;

	SeoulDistrict(String koreanName) {
		this.koreanName = koreanName;
	}

	public String koreanName() {
		return koreanName;
	}

	/** API で受け取る値は韓国語の名前（03 の API-01 の district） */
	@Override
	public String apiValue() {
		return koreanName;
	}

	/** 見つからない・null なら空（Genre.fromKopisName と同じ扱い） */
	public static Optional<SeoulDistrict> fromKoreanName(String name) {
		if (name == null) {
			return Optional.empty();
		}
		return Arrays.stream(values()).filter(d -> d.koreanName.equals(name)).findFirst();
	}

}
