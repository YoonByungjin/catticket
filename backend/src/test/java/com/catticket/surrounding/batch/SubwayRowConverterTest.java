package com.catticket.surrounding.batch;

import static com.catticket.surrounding.batch.SubwayRowConverter.cleanJapanese;
import static com.catticket.surrounding.batch.SubwayRowConverter.isSeoul;
import static com.catticket.surrounding.batch.SubwayRowConverter.normalizeLockerName;
import static com.catticket.surrounding.batch.SubwayRowConverter.normalizeName;
import static com.catticket.surrounding.batch.SubwayRowConverter.parseLine;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import com.catticket.common.batch.StepSummary;
import com.catticket.surrounding.batch.SubwayRowConverter.JaName;

/**
 * SubwayRowConverter（05 の 4.3、06 の 5.4）。行はコードで作る。
 */
class SubwayRowConverterTest {

	private final SubwayRowConverter converter = new SubwayRowConverter();

	// --- 行を作る小さな部品 ---

	private static CsvRow row(int line, String... keyValues) {
		Map<String, String> map = new LinkedHashMap<>();
		for (int i = 0; i < keyValues.length; i += 2) {
			map.put(keyValues[i], keyValues[i + 1]);
		}
		return new CsvRow(line, map);
	}

	private static CsvRow location(String line, String code, String name) {
		return location(line, code, name, "37.5", "127.0");
	}

	private static CsvRow location(String line, String code, String name, String lat, String lng) {
		return row(2, SubwayColumns.LINE, line, SubwayColumns.LOCATION_CODE, code, SubwayColumns.STATION_NAME, name,
				SubwayColumns.LATITUDE, lat, SubwayColumns.LONGITUDE, lng);
	}

	private static CsvRow address(String code, String name, String road) {
		return row(2, SubwayColumns.ADDRESS_CODE, code, SubwayColumns.STATION_NAME, name, SubwayColumns.ROAD_ADDRESS,
				road);
	}

	private static CsvRow locker(String line, String name, String location) {
		return row(2, SubwayColumns.LINE, line, SubwayColumns.LOCKER_NAME, name, SubwayColumns.LOCKER_LOCATION,
				location);
	}

	private static CsvRow ja(String line, String ko, String ja) {
		return row(2, SubwayColumns.LINE, line, SubwayColumns.NAME_KO, ko, SubwayColumns.NAME_JA, ja);
	}

	private static final String SEOUL = "서울특별시 중구 세종대로 지하2(남대문로 5가)";

	private static final String GYEONGGI = "경기도 고양시 덕양구 삼송로 300(지축동)";

	// --- 規則 ---

	@Test
	void parseLineAcceptsOneToEight() {
		assertThat(parseLine("1")).isEqualTo(OptionalInt.of(1));
		assertThat(parseLine("01")).isEqualTo(OptionalInt.of(1));
		assertThat(parseLine("1호선")).isEqualTo(OptionalInt.of(1));
		assertThat(parseLine("01호선")).isEqualTo(OptionalInt.of(1));
		assertThat(parseLine(" 2호선(지선) ")).isEqualTo(OptionalInt.of(2));
		assertThat(parseLine("8")).isEqualTo(OptionalInt.of(8));
	}

	@Test
	void parseLineRejectsOthers() {
		assertThat(parseLine("9")).isEmpty();
		assertThat(parseLine("9호선")).isEmpty();
		assertThat(parseLine("0")).isEmpty();
		assertThat(parseLine("식물원")).isEmpty();
		assertThat(parseLine("경의중앙선")).isEmpty();
		assertThat(parseLine("")).isEmpty();
		assertThat(parseLine(null)).isEmpty();
		assertThat(parseLine("1234")).isEmpty();
	}

	@Test
	void normalizeNameRemovesSubNameAndYeok() {
		assertThat(normalizeName("청량리(서울시립대입구)")).isEqualTo("청량리");
		assertThat(normalizeName("서울역")).isEqualTo("서울");
		assertThat(normalizeName("교대(법원, 검찰청)")).isEqualTo("교대");
		assertThat(normalizeName("교대(법원·검찰청)")).isEqualTo("교대");
		assertThat(normalizeName(" 시청 ")).isEqualTo("시청");
		assertThat(normalizeName("총신대입구（이수）")).isEqualTo("총신대입구");
		// 1文字の「역」は除かない
		assertThat(normalizeName("역")).isEqualTo("역");
	}

	@Test
	void normalizeLockerNameRemovesNumbersAndLetters() {
		assertThat(normalizeLockerName("서울역1~22")).isEqualTo("서울");
		assertThat(normalizeLockerName("시청29~44")).isEqualTo("시청");
		assertThat(normalizeLockerName("충정로(1~15)")).isEqualTo("충정로");
		assertThat(normalizeLockerName("충정로B")).isEqualTo("충정로");
		assertThat(normalizeLockerName("제기동")).isEqualTo("제기동");
		assertThat(normalizeLockerName("강남 12")).isEqualTo("강남");
	}

	@Test
	void cleanJapaneseRemovesSubNameAndDetectsGarbled() {
		assertThat(cleanJapanese("ワンシムニ(ソンドン??)")).isEqualTo(new JaName(JaName.Status.OK, "ワンシムニ"));
		assertThat(cleanJapanese(" シチョン ")).isEqualTo(new JaName(JaName.Status.OK, "シチョン"));
		assertThat(cleanJapanese("クロ?デジタルダンジ").status()).isEqualTo(JaName.Status.GARBLED);
		assertThat(cleanJapanese("カン？ム").status()).isEqualTo(JaName.Status.GARBLED);
		assertThat(cleanJapanese("カン�ム").status()).isEqualTo(JaName.Status.GARBLED);
		assertThat(cleanJapanese("").status()).isEqualTo(JaName.Status.BLANK);
		assertThat(cleanJapanese(null).status()).isEqualTo(JaName.Status.BLANK);
	}

	@Test
	void isSeoulChecksPrefix() {
		assertThat(isSeoul(SEOUL)).isTrue();
		assertThat(isSeoul("  " + SEOUL)).isTrue();
		assertThat(isSeoul(GYEONGGI)).isFalse();
		assertThat(isSeoul("서울시 중구")).isFalse();
		assertThat(isSeoul(null)).isFalse();
	}

	// --- convert ---

	@Test
	void convertLinksByCodeAndSavesSeoulStations() {
		SubwayFiles files = new SubwayFiles(List.of(location("1", "150", "서울", "37.55315", "126.972533")),
				List.of(address("150", "서울", SEOUL)), List.of(locker("1", "서울역1~22", "1번 출입구")),
				List.of(ja("1호선", "서울역", "ソウルヨク")));

		SubwayImportPlan plan = converter.convert(files);

		assertThat(plan.stations()).containsExactly(new StationInsert("150", 1, "서울", "ソウルヨク",
				new BigDecimal("37.55315"), new BigDecimal("126.972533")));
		assertThat(plan.lockers()).containsExactly(new LockerInsert("150", "서울역1~22", "1번 출입구"));
		assertThat(plan.summary().get(SubwayRowConverter.STATIONS_SAVED)).isEqualTo(1);
		assertThat(plan.summary().get(SubwayRowConverter.LOCKERS_SAVED)).isEqualTo(1);
	}

	@Test
	void codeLinksEvenWhenNamesDiffer() {
		// 당고개 / 불암산（改名。コードは同じ 409）
		SubwayFiles files = new SubwayFiles(List.of(location("4", "409", "당고개")),
				List.of(address("409", "불암산", "서울특별시 노원구 상계로 지하 305(상계동)")), List.of(), List.of());

		SubwayImportPlan plan = converter.convert(files);

		assertThat(plan.stations()).extracting(StationInsert::nameKo).containsExactly("당고개");
		assertThat(plan.summary().get(SubwayRowConverter.STATION_ADDRESS_BY_NAME)).isZero();
	}

	@Test
	void kkachisanLinksByNameAcrossLines() {
		// 2号線の 까치산（コード 200）は住所のファイルになく、5号線の 까치산（2519）に名前で結ぶ
		SubwayFiles files = new SubwayFiles(List.of(location("2", "200", "까치산")),
				List.of(address("2519", "까치산", "서울특별시 강서구 강서로 지하54(화곡동)")), List.of(),
				List.of(ja("2호선(지선)", "까치산", "カチサン")));

		SubwayImportPlan plan = converter.convert(files);

		assertThat(plan.stations()).containsExactly(
				new StationInsert("200", 2, "까치산", "カチサン", new BigDecimal("37.5"), new BigDecimal("127.0")));
		assertThat(plan.summary().get(SubwayRowConverter.STATION_ADDRESS_BY_NAME)).isEqualTo(1);
		assertThat(plan.summary().details(SubwayRowConverter.STATION_ADDRESS_BY_NAME)).containsExactly("2|까치산");
	}

	@Test
	void nameLinkWithMixedSeoulIsNoAddress() {
		SubwayFiles files = new SubwayFiles(List.of(location("2", "200", "까치산")),
				List.of(address("2519", "까치산", SEOUL), address("9999", "까치산(별명)", GYEONGGI)), List.of(),
				List.of());

		SubwayImportPlan plan = converter.convert(files);

		assertThat(plan.stations()).isEmpty();
		assertThat(plan.summary().get(SubwayRowConverter.STATION_NO_ADDRESS)).isEqualTo(1);
		assertThat(plan.summary().details(SubwayRowConverter.STATION_NO_ADDRESS)).containsExactly("2|까치산|200");
	}

	@Test
	void unlinkedOrBlankAddressIsNoAddress() {
		SubwayFiles files = new SubwayFiles(List.of(location("1", "999", "미지"), location("1", "151", "시청")),
				List.of(address("151", "시청", "")), List.of(), List.of());

		SubwayImportPlan plan = converter.convert(files);

		assertThat(plan.stations()).isEmpty();
		assertThat(plan.summary().get(SubwayRowConverter.STATION_NO_ADDRESS)).isEqualTo(2);
	}

	@Test
	void bokjeongWithSeoulAddressIsSaved() {
		// 福井駅は住所のとおりソウルの駅（人間の決定 A案。例外を作らない）
		SubwayFiles files = new SubwayFiles(List.of(location("8", "2828", "복정")),
				List.of(address("2828", "복정", "서울특별시 송파구 송파대로 지하6(장지동)")), List.of(locker("8", "복정", "대합실")),
				List.of());

		SubwayImportPlan plan = converter.convert(files);

		assertThat(plan.stations()).extracting(StationInsert::nameKo).containsExactly("복정");
		assertThat(plan.stations().get(0).nameJa()).isNull();
		assertThat(plan.lockers()).hasSize(1);
		assertThat(plan.summary().get(SubwayRowConverter.JA_NAME_MISSING)).isEqualTo(1);
	}

	@Test
	void outsideSeoulStationAndItsLockersAreNotSaved() {
		SubwayFiles files = new SubwayFiles(List.of(location("3", "320", "지축")),
				List.of(address("320", "지축", GYEONGGI)), List.of(locker("3", "지축1~10", "대합실")), List.of());

		SubwayImportPlan plan = converter.convert(files);

		assertThat(plan.stations()).isEmpty();
		assertThat(plan.lockers()).isEmpty();
		assertThat(plan.summary().get(SubwayRowConverter.STATION_OUTSIDE_SEOUL)).isEqualTo(1);
		assertThat(plan.summary().details(SubwayRowConverter.STATION_OUTSIDE_SEOUL)).containsExactly("3|지축");
		assertThat(plan.summary().get(SubwayRowConverter.LOCKER_OUTSIDE_SEOUL)).isEqualTo(1);
		assertThat(plan.summary().details(SubwayRowConverter.LOCKER_OUTSIDE_SEOUL)).containsExactly("3|지축1~10");
	}

	@Test
	void japaneseGarbledAndMissingAreSavedWithoutJapanese() {
		SubwayFiles files = new SubwayFiles(List.of(location("2", "222", "강남"), location("7", "2738", "총신대입구")),
				List.of(address("222", "강남", SEOUL), address("2738", "총신대입구(이수)", SEOUL)), List.of(),
				List.of(ja("2호선", "강남", "カン?ム"), ja("7호선", "이수", "イス")));

		SubwayImportPlan plan = converter.convert(files);

		assertThat(plan.stations()).extracting(StationInsert::nameJa).containsExactly(null, null);
		assertThat(plan.summary().get(SubwayRowConverter.JA_NAME_GARBLED)).isEqualTo(1);
		assertThat(plan.summary().details(SubwayRowConverter.JA_NAME_GARBLED)).containsExactly("2|강남");
		assertThat(plan.summary().get(SubwayRowConverter.JA_NAME_MISSING)).isEqualTo(1);
		assertThat(plan.summary().details(SubwayRowConverter.JA_NAME_MISSING)).containsExactly("7|총신대입구");
	}

	@Test
	void lockerLinkingRules() {
		SubwayFiles files = new SubwayFiles(List.of(location("2", "243", "충정로")),
				List.of(address("243", "충정로(경기대입구)", SEOUL)),
				List.of(locker("2", "충정로(1~15)", "대합실"), locker("2", "충정로B", "B 구역"),
						locker("7", "이수", "7호선 대합실"), locker("9", "신논현1~10", "대합실"), locker("식물원", "식물원", "대합실")),
				List.of());

		SubwayImportPlan plan = converter.convert(files);

		// 保管庫の名前は原文のまま
		assertThat(plan.lockers()).containsExactly(new LockerInsert("243", "충정로(1~15)", "대합실"),
				new LockerInsert("243", "충정로B", "B 구역"));
		assertThat(plan.summary().get(SubwayRowConverter.LOCKER_UNMATCHED)).isEqualTo(1);
		assertThat(plan.summary().details(SubwayRowConverter.LOCKER_UNMATCHED)).containsExactly("7|이수|7호선 대합실");
		assertThat(plan.summary().get(SubwayRowConverter.LOCKER_SKIPPED_LINE)).isEqualTo(2);
	}

	@Test
	void countsAddUp() {
		SubwayFiles files = new SubwayFiles(
				List.of(location("1", "150", "서울"), location("3", "320", "지축"), location("1", "999", "미지"),
						location("경의중앙선", "1111", "가좌")),
				List.of(address("150", "서울", SEOUL), address("320", "지축", GYEONGGI)),
				List.of(locker("1", "서울역1~22", "a"), locker("3", "지축", "b"), locker("7", "이수", "c"),
						locker("9", "신논현", "d")),
				List.of());

		StepSummary s = converter.convert(files).summary();

		// 0 件の名前も入っている
		assertThat(s.counts().keySet()).containsExactlyElementsOf(SubwayRowConverter.COUNT_NAMES);
		assertThat(s.get(SubwayRowConverter.STATION_ROWS)).isEqualTo(4);
		assertThat(s.get(SubwayRowConverter.STATION_ROWS)).isEqualTo(s.get(SubwayRowConverter.STATIONS_SAVED)
				+ s.get(SubwayRowConverter.STATION_SKIPPED_LINE) + s.get(SubwayRowConverter.STATION_OUTSIDE_SEOUL)
				+ s.get(SubwayRowConverter.STATION_NO_ADDRESS));
		assertThat(s.get(SubwayRowConverter.LOCKER_ROWS)).isEqualTo(4);
		assertThat(s.get(SubwayRowConverter.LOCKER_ROWS)).isEqualTo(s.get(SubwayRowConverter.LOCKERS_SAVED)
				+ s.get(SubwayRowConverter.LOCKER_SKIPPED_LINE) + s.get(SubwayRowConverter.LOCKER_OUTSIDE_SEOUL)
				+ s.get(SubwayRowConverter.LOCKER_UNMATCHED));
		assertThat(s.get(SubwayRowConverter.STATIONS_SAVED)).isEqualTo(1);
		assertThat(s.get(SubwayRowConverter.LOCKERS_SAVED)).isEqualTo(1);
	}

	@Test
	void coordinatesAreRoundedToSixDigitsAndValidated() {
		SubwayFiles files = new SubwayFiles(List.of(location("1", "150", "서울", "37.1234565", "126.9")),
				List.of(address("150", "서울", SEOUL)), List.of(), List.of());
		assertThat(converter.convert(files).stations().get(0).latitude()).isEqualTo(new BigDecimal("37.123457"));

		assertThatThrownBy(() -> converter.convert(new SubwayFiles(List.of(location("1", "150", "서울", "abc", "126.9")),
				List.of(address("150", "서울", SEOUL)), List.of(), List.of())))
			.isInstanceOf(SubwayFileException.class)
			.hasMessageContaining("위도");
		assertThatThrownBy(() -> converter.convert(new SubwayFiles(List.of(location("1", "150", "서울", "37.5", "181")),
				List.of(address("150", "서울", SEOUL)), List.of(), List.of())))
			.isInstanceOf(SubwayFileException.class)
			.hasMessageContaining("경도");
	}

	// --- ファイルの誤り（A-02 など） ---

	@Test
	void tooLongValuesFail() {
		String longCode = "1".repeat(21);
		assertThatThrownBy(() -> converter.convert(new SubwayFiles(List.of(location("1", longCode, "서울")),
				List.of(address(longCode, "서울", SEOUL)), List.of(), List.of())))
			.isInstanceOf(SubwayFileException.class)
			.hasMessageContaining(SubwayColumns.LOCATION_CODE);

		String longName = "가".repeat(51);
		assertThatThrownBy(() -> converter.convert(new SubwayFiles(List.of(location("1", "150", longName)),
				List.of(address("150", longName, SEOUL)), List.of(), List.of())))
			.isInstanceOf(SubwayFileException.class)
			.hasMessageContaining(SubwayColumns.STATION_NAME);

		assertThatThrownBy(() -> converter.convert(new SubwayFiles(List.of(location("1", "150", "서울")),
				List.of(address("150", "서울", SEOUL)), List.of(), List.of(ja("1호선", "서울역", "ア".repeat(101))))))
			.isInstanceOf(SubwayFileException.class)
			.hasMessageContaining(SubwayColumns.NAME_JA);

		assertThatThrownBy(() -> converter.convert(new SubwayFiles(List.of(location("1", "150", "서울")),
				List.of(address("150", "서울", SEOUL)), List.of(locker("1", "서울역" + "1".repeat(198), "a")), List.of())))
			.isInstanceOf(SubwayFileException.class)
			.hasMessageContaining(SubwayColumns.LOCKER_NAME);

		assertThatThrownBy(() -> converter.convert(new SubwayFiles(List.of(location("1", "150", "서울")),
				List.of(address("150", "서울", SEOUL)), List.of(locker("1", "서울역1~22", "가".repeat(501))), List.of())))
			.isInstanceOf(SubwayFileException.class)
			.hasMessageContaining(SubwayColumns.LOCKER_LOCATION);
	}

	@Test
	void valuesAtTheLimitPass() {
		String code = "1".repeat(20);
		String name = "가".repeat(50);
		SubwayFiles files = new SubwayFiles(List.of(location("1", code, name)), List.of(address(code, name, SEOUL)),
				List.of(), List.of());
		assertThat(converter.convert(files).stations()).hasSize(1);
	}

	@Test
	void lengthIsCheckedOnlyForSavedRows() {
		// ソウル外の駅の長い名前は確かめない
		String longName = "가".repeat(51);
		SubwayFiles files = new SubwayFiles(List.of(location("3", "320", longName), location("1", "150", "서울")),
				List.of(address("320", longName, GYEONGGI), address("150", "서울", SEOUL)), List.of(), List.of());
		assertThat(converter.convert(files).stations()).hasSize(1);
	}

	@Test
	void duplicatesFail() {
		assertThatThrownBy(() -> converter.convert(new SubwayFiles(List.of(location("1", "150", "서울")),
				List.of(address("150", "서울", SEOUL), address("150", "서울", SEOUL)), List.of(), List.of())))
			.isInstanceOf(SubwayFileException.class)
			.hasMessageContaining("station-address")
			.hasMessageContaining("150 が重複");

		assertThatThrownBy(() -> converter.convert(new SubwayFiles(List.of(location("1", "150", "서울")),
				List.of(address("150", "서울", SEOUL)), List.of(),
				List.of(ja("1호선", "서울역", "ソウル"), ja("1호선", "서울", "ソウル")))))
			.isInstanceOf(SubwayFileException.class)
			.hasMessageContaining("station-name-ja");

		assertThatThrownBy(() -> converter.convert(new SubwayFiles(
				List.of(location("1", "150", "서울"), location("1", "151", "서울")),
				List.of(address("150", "서울", SEOUL), address("151", "서울", SEOUL)), List.of(), List.of())))
			.isInstanceOf(SubwayFileException.class)
			.hasMessageContaining("station-location");
	}

	@Test
	void blankRequiredValuesFail() {
		assertThatThrownBy(() -> converter.convert(new SubwayFiles(List.of(location("1", "", "서울")), List.of(),
				List.of(), List.of())))
			.isInstanceOf(SubwayFileException.class)
			.hasMessageContaining(SubwayColumns.LOCATION_CODE);

		List<CsvRow> lockers = new ArrayList<>();
		lockers.add(locker("1", "서울역1~22", ""));
		assertThatThrownBy(() -> converter.convert(new SubwayFiles(List.of(location("1", "150", "서울")),
				List.of(address("150", "서울", SEOUL)), lockers, List.of())))
			.isInstanceOf(SubwayFileException.class)
			.hasMessageContaining(SubwayColumns.LOCKER_LOCATION);
	}

}
