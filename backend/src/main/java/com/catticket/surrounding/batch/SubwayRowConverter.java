package com.catticket.surrounding.batch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.catticket.common.batch.StepSummary;
import com.catticket.common.text.Texts;

/**
 * 4つのファイルの行を、入れる駅・コインロッカーと件数にする（05 の 4.3、06 の 5.4）。Spring に依存しない。
 * ファイルの誤りは {@link SubwayFileException}。
 */
public final class SubwayRowConverter {

	// 件数の名前（06 の 5.4。StepSummary のキー）
	public static final String STATION_ROWS = "stationRows";

	public static final String STATIONS_SAVED = "stationsSaved";

	public static final String STATION_SKIPPED_LINE = "stationSkippedLine";

	public static final String STATION_OUTSIDE_SEOUL = "stationOutsideSeoul";

	public static final String STATION_NO_ADDRESS = "stationNoAddress";

	public static final String STATION_ADDRESS_BY_NAME = "stationAddressByName";

	public static final String JA_NAME_MISSING = "jaNameMissing";

	public static final String JA_NAME_GARBLED = "jaNameGarbled";

	public static final String LOCKER_ROWS = "lockerRows";

	public static final String LOCKERS_SAVED = "lockersSaved";

	public static final String LOCKER_SKIPPED_LINE = "lockerSkippedLine";

	public static final String LOCKER_OUTSIDE_SEOUL = "lockerOutsideSeoul";

	public static final String LOCKER_UNMATCHED = "lockerUnmatched";

	/** 値が 0 でも入れる（「0 件」と「数えていない」を区別できるように）。この順で記録に出る */
	static final List<String> COUNT_NAMES = List.of(STATION_ROWS, STATIONS_SAVED, STATION_SKIPPED_LINE,
			STATION_OUTSIDE_SEOUL, STATION_NO_ADDRESS, STATION_ADDRESS_BY_NAME, JA_NAME_MISSING, JA_NAME_GARBLED,
			LOCKER_ROWS, LOCKERS_SAVED, LOCKER_SKIPPED_LINE, LOCKER_OUTSIDE_SEOUL, LOCKER_UNMATCHED);

	// 列の長さ（04 の station・coin_locker。A-02）
	static final int MAX_EXTERNAL_CODE = 20;

	static final int MAX_NAME_KO = 50;

	static final int MAX_NAME_JA = 100;

	static final int MAX_LOCKER_NAME = 200;

	static final int MAX_LOCATION_TEXT = 500;

	// ファイルの用途（メッセージに出す）
	static final String LOCATION_FILE = "station-location";

	static final String ADDRESS_FILE = "station-address";

	static final String LOCKER_FILE = "locker";

	static final String NAME_FILE = "station-name-ja";

	private static final String SEOUL_PREFIX = "서울특별시";

	private static final Pattern TRAILING_PAREN = Pattern.compile("\\s*[(（][^()（）]*[)）]$");

	private static final Pattern LINE_DIGITS = Pattern.compile("[0-9]{1,3}");

	// 보관함명の末尾の番号の範囲・番号・英大文字・空白（05 の 4.3 の 4.）
	private static final Pattern LOCKER_SUFFIX = Pattern
		.compile("(?:\\([0-9]+[~～][0-9]+\\)|[0-9]+[~～][0-9]+|[0-9]+|[A-Z]+|\\s+)$");

	/** (路線, 正規化した名前) */
	record LineName(int line, String name) {
	}

	/** コインロッカーの結びつけ先の駅 */
	private record StationRef(String externalCode, boolean saved) {
	}

	/** 日本語の駅名を整えた結果 */
	record JaName(Status status, String value) {

		enum Status {

			OK, BLANK, GARBLED

		}

		static JaName ok(String value) {
			return new JaName(Status.OK, value);
		}

		static final JaName BLANK = new JaName(Status.BLANK, null);

		static final JaName GARBLED = new JaName(Status.GARBLED, null);

	}

	public SubwayImportPlan convert(SubwayFiles files) {
		StepSummary summary = new StepSummary();
		COUNT_NAMES.forEach(name -> summary.add(name, 0));

		// 1. 住所: 駅コード → 行、正規化した名前 → 行（路線は問わない。도로명주소が空の行は入れない）
		Map<String, CsvRow> addressByCode = new HashMap<>();
		Map<String, List<CsvRow>> addressByName = new HashMap<>();
		for (CsvRow row : files.addresses()) {
			String code = row.get(SubwayColumns.ADDRESS_CODE);
			if (addressByCode.put(code, row) != null) {
				throw error(ADDRESS_FILE, row, SubwayColumns.ADDRESS_CODE, code + " が重複");
			}
			if (!row.get(SubwayColumns.ROAD_ADDRESS).isEmpty()) {
				addressByName.computeIfAbsent(normalizeName(row.get(SubwayColumns.STATION_NAME)),
						k -> new ArrayList<>())
					.add(row);
			}
		}

		// 2. 多言語表記: (路線, 正規化した한글) → 行。1〜8号線の外は数えない
		Map<LineName, CsvRow> japaneseByKey = new HashMap<>();
		for (CsvRow row : files.names()) {
			OptionalInt line = parseLine(row.get(SubwayColumns.LINE));
			if (line.isEmpty()) {
				continue;
			}
			LineName key = new LineName(line.getAsInt(), normalizeName(row.get(SubwayColumns.NAME_KO)));
			if (japaneseByKey.put(key, row) != null) {
				throw error(NAME_FILE, row, SubwayColumns.NAME_KO, key.line() + "|" + key.name() + " が重複");
			}
		}

		// 3. 駅
		Map<LineName, StationRef> stationsByKey = new HashMap<>();
		List<StationInsert> stations = new ArrayList<>();
		for (CsvRow row : files.locations()) {
			summary.increment(STATION_ROWS);
			OptionalInt parsedLine = parseLine(row.get(SubwayColumns.LINE));
			if (parsedLine.isEmpty()) {
				summary.increment(STATION_SKIPPED_LINE);
				continue;
			}
			int line = parsedLine.getAsInt();
			String code = row.get(SubwayColumns.LOCATION_CODE);
			String name = row.get(SubwayColumns.STATION_NAME);
			if (code.isEmpty()) {
				throw error(LOCATION_FILE, row, SubwayColumns.LOCATION_CODE, "空");
			}
			if (name.isEmpty()) {
				throw error(LOCATION_FILE, row, SubwayColumns.STATION_NAME, "空");
			}
			LineName key = new LineName(line, normalizeName(name));

			Boolean seoul = findSeoul(code, name, addressByCode, addressByName, line, summary);
			if (seoul == null) {
				summary.increment(STATION_NO_ADDRESS);
				summary.addDetail(STATION_NO_ADDRESS, line + "|" + name + "|" + code);
			}
			else if (!seoul) {
				summary.increment(STATION_OUTSIDE_SEOUL);
				summary.addDetail(STATION_OUTSIDE_SEOUL, line + "|" + name);
			}
			boolean saved = Boolean.TRUE.equals(seoul);
			// 保存しない駅もコインロッカーの結びつけに使う（結びついたが入れない、を数えるため）
			if (stationsByKey.put(key, new StationRef(code, saved)) != null) {
				throw error(LOCATION_FILE, row, SubwayColumns.STATION_NAME, line + "|" + key.name() + " が重複");
			}
			if (!saved) {
				continue;
			}

			String nameJa = findJapanese(japaneseByKey.get(key), line, name, summary);
			checkLength(LOCATION_FILE, row, SubwayColumns.LOCATION_CODE, code, MAX_EXTERNAL_CODE);
			checkLength(LOCATION_FILE, row, SubwayColumns.STATION_NAME, name, MAX_NAME_KO);
			if (nameJa != null) {
				checkLength(NAME_FILE, japaneseByKey.get(key), SubwayColumns.NAME_JA, nameJa, MAX_NAME_JA);
			}
			BigDecimal latitude = parseCoordinate(row, SubwayColumns.LATITUDE, 90);
			BigDecimal longitude = parseCoordinate(row, SubwayColumns.LONGITUDE, 180);
			stations.add(new StationInsert(code, line, name, nameJa, latitude, longitude));
		}

		// 4. 保存する駅どうしの重複は、04 の一意制約に当たる前に理由の分かる失敗にする
		Set<String> codes = new HashSet<>();
		Set<LineName> lineNames = new HashSet<>();
		for (StationInsert s : stations) {
			if (!codes.add(s.externalStationCode())) {
				throw new SubwayFileException(
						LOCATION_FILE + " " + SubwayColumns.LOCATION_CODE + ": " + s.externalStationCode() + " が重複");
			}
			if (!lineNames.add(new LineName(s.line(), s.nameKo()))) {
				throw new SubwayFileException(
						LOCATION_FILE + " " + SubwayColumns.STATION_NAME + ": " + s.line() + "|" + s.nameKo() + " が重複");
			}
		}

		// 5. コインロッカー
		List<LockerInsert> lockers = new ArrayList<>();
		for (CsvRow row : files.lockers()) {
			summary.increment(LOCKER_ROWS);
			OptionalInt parsedLine = parseLine(row.get(SubwayColumns.LINE));
			if (parsedLine.isEmpty()) {
				summary.increment(LOCKER_SKIPPED_LINE);
				continue;
			}
			int line = parsedLine.getAsInt();
			String name = row.get(SubwayColumns.LOCKER_NAME);
			String location = row.get(SubwayColumns.LOCKER_LOCATION);
			StationRef station = stationsByKey.get(new LineName(line, normalizeLockerName(name)));
			if (station == null) {
				summary.increment(LOCKER_UNMATCHED);
				summary.addDetail(LOCKER_UNMATCHED, line + "|" + name + "|" + location);
				continue;
			}
			if (!station.saved()) {
				summary.increment(LOCKER_OUTSIDE_SEOUL);
				summary.addDetail(LOCKER_OUTSIDE_SEOUL, line + "|" + name);
				continue;
			}
			if (name.isEmpty()) {
				throw error(LOCKER_FILE, row, SubwayColumns.LOCKER_NAME, "空");
			}
			if (location.isEmpty()) {
				throw error(LOCKER_FILE, row, SubwayColumns.LOCKER_LOCATION, "空");
			}
			checkLength(LOCKER_FILE, row, SubwayColumns.LOCKER_NAME, name, MAX_LOCKER_NAME);
			checkLength(LOCKER_FILE, row, SubwayColumns.LOCKER_LOCATION, location, MAX_LOCATION_TEXT);
			lockers.add(new LockerInsert(station.externalCode(), name, location));
		}

		summary.add(STATIONS_SAVED, stations.size());
		summary.add(LOCKERS_SAVED, lockers.size());
		return new SubwayImportPlan(List.copyOf(stations), List.copyOf(lockers), summary);
	}

	/**
	 * 駅がソウルか。住所と結びつかない（または住所が空の）ときは null。
	 * 駅コードで結び、なければ正規化した名前で路線を問わず探す（05 の 4.3 の 2.）
	 */
	private static Boolean findSeoul(String code, String name, Map<String, CsvRow> addressByCode,
			Map<String, List<CsvRow>> addressByName, int line, StepSummary summary) {
		CsvRow byCode = addressByCode.get(code);
		if (byCode != null) {
			String road = byCode.get(SubwayColumns.ROAD_ADDRESS);
			return road.isEmpty() ? null : isSeoul(road);
		}
		List<CsvRow> candidates = addressByName.getOrDefault(normalizeName(name), List.of());
		if (candidates.isEmpty()) {
			return null;
		}
		boolean first = isSeoul(candidates.get(0).get(SubwayColumns.ROAD_ADDRESS));
		boolean allSame = candidates.stream().allMatch(c -> isSeoul(c.get(SubwayColumns.ROAD_ADDRESS)) == first);
		if (!allSame) {
			// ソウルとソウル外が混ざると決められない
			return null;
		}
		summary.increment(STATION_ADDRESS_BY_NAME);
		summary.addDetail(STATION_ADDRESS_BY_NAME, line + "|" + name);
		return first;
	}

	/** 公式の日本語表記。ない・壊れているときは null（B-01 の S7 で翻訳する） */
	private static String findJapanese(CsvRow row, int line, String name, StepSummary summary) {
		JaName ja = row == null ? JaName.BLANK : cleanJapanese(row.get(SubwayColumns.NAME_JA));
		switch (ja.status()) {
			case OK -> {
				return ja.value();
			}
			case GARBLED -> {
				summary.increment(JA_NAME_GARBLED);
				summary.addDetail(JA_NAME_GARBLED, line + "|" + name);
			}
			case BLANK -> {
				summary.increment(JA_NAME_MISSING);
				summary.addDetail(JA_NAME_MISSING, line + "|" + name);
			}
		}
		return null;
	}

	/** 路線の値を 1〜8 の整数にする（A-07）。ほかは空 */
	static OptionalInt parseLine(String value) {
		if (value == null) {
			return OptionalInt.empty();
		}
		String v = value.strip();
		if (v.endsWith("(지선)")) {
			v = v.substring(0, v.length() - "(지선)".length()).strip();
		}
		if (v.endsWith("호선")) {
			v = v.substring(0, v.length() - "호선".length()).strip();
		}
		if (!LINE_DIGITS.matcher(v).matches()) {
			return OptionalInt.empty();
		}
		int line = Integer.parseInt(v);
		return line >= 1 && line <= 8 ? OptionalInt.of(line) : OptionalInt.empty();
	}

	/** 比べるための駅名（括弧の副名・末尾の「역」を除く）。保存する駅名には使わない */
	static String normalizeName(String value) {
		if (value == null) {
			return "";
		}
		String v = removeTrailingParen(value.strip()).strip();
		if (v.codePointCount(0, v.length()) >= 2 && v.endsWith("역")) {
			v = v.substring(0, v.length() - 1);
		}
		return v.strip();
	}

	/** 보관함명から末尾の番号の範囲などを除き、駅名の形にする */
	static String normalizeLockerName(String value) {
		if (value == null) {
			return "";
		}
		String v = value.strip();
		while (true) {
			Matcher m = LOCKER_SUFFIX.matcher(v);
			if (!m.find() || m.start() == m.end()) {
				break;
			}
			v = v.substring(0, m.start());
		}
		return normalizeName(v);
	}

	/** 日本語の駅名を整える。? や U+FFFD があれば文字化けとみなす（元に戻せないので使わない） */
	static JaName cleanJapanese(String value) {
		if (value == null) {
			return JaName.BLANK;
		}
		String v = removeTrailingParen(value.strip()).strip();
		if (v.isEmpty()) {
			return JaName.BLANK;
		}
		if (v.indexOf('?') >= 0 || v.indexOf('？') >= 0 || v.indexOf('�') >= 0) {
			return JaName.GARBLED;
		}
		return JaName.ok(v);
	}

	static boolean isSeoul(String roadAddress) {
		return roadAddress != null && roadAddress.strip().startsWith(SEOUL_PREFIX);
	}

	private static String removeTrailingParen(String value) {
		return TRAILING_PAREN.matcher(value).replaceFirst("");
	}

	private static BigDecimal parseCoordinate(CsvRow row, String column, int limit) {
		String raw = row.get(column);
		BigDecimal value;
		try {
			value = new BigDecimal(raw);
		}
		catch (NumberFormatException e) {
			throw error(LOCATION_FILE, row, column, "数でない（" + raw + "）");
		}
		if (value.abs().compareTo(BigDecimal.valueOf(limit)) > 0) {
			throw error(LOCATION_FILE, row, column, "範囲の外（" + raw + "）");
		}
		// 列は DECIMAL(9,6)
		return value.scale() > 6 ? value.setScale(6, RoundingMode.HALF_UP) : value;
	}

	private static void checkLength(String file, CsvRow row, String column, String value, int max) {
		int length = Texts.codePointLength(value);
		if (length > max) {
			// 切り詰めない（A-02。人がファイルを確かめて直す）
			throw error(file, row, column, "長すぎる（" + length + " 文字 > " + max + "）");
		}
	}

	private static SubwayFileException error(String file, CsvRow row, String column, String reason) {
		return new SubwayFileException(file + " " + row.lineNumber() + "行目 " + column + ": " + reason);
	}

}
