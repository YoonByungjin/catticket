package com.catticket.collect;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.catticket.collect.client.xml.KopisPerformanceDetail;
import com.catticket.collect.client.xml.KopisRelate;
import com.catticket.collect.client.xml.KopisVenueDetail;
import com.catticket.collect.row.BookingSiteValue;
import com.catticket.collect.row.PerformanceValues;
import com.catticket.collect.row.VenueValues;
import com.catticket.common.code.Genre;
import com.catticket.common.code.SeoulDistrict;
import com.catticket.common.text.Texts;

/**
 * 提供元（KOPIS）の値を列に入る値にする（05 の 3.3 の 10.・04 の 11.7 の A-02、06 の 7.1 の「値の形」）。Spring に依存しない。
 * 長さはコードポイントで数える。ID・ジャンルが使えない公演は保存しない。URL が長い項目は保存しない。文は切り詰める。
 */
public final class ProviderValues {

	/** 公演 ID・施設 ID（VARCHAR(20)） */
	static final int ID_MAX = 20;

	/** ポスター・予約サイトの URL（VARCHAR(2000)） */
	static final int URL_MAX = 2000;

	static final int TITLE_MAX = 500;

	/** 上演時間・観覧年齢・会場名・予約サイトの名前（VARCHAR(200)） */
	static final int SHORT_TEXT_MAX = 200;

	static final int ADDRESS_MAX = 500;

	/** TEXT の列（出演者・チケット価格・公演時間の案内）のバイト数 */
	static final int TEXT_MAX_BYTES = 65535;

	/** StepSummary の内容に入れる値の長さ（長い ID などで記録が大きくならないように） */
	static final int DETAIL_VALUE_MAX = 50;

	static final String SEOUL = "서울특별시";

	private static final DateTimeFormatter PERIOD_DATE = DateTimeFormatter.ofPattern("uuuu.MM.dd")
		.withResolverStyle(ResolverStyle.STRICT);

	// 小数の秒は 1〜9 桁を任意で受ける（06 の 7.1）。ありえない日時は STRICT で失敗させる
	private static final DateTimeFormatter UPDATED_AT = new DateTimeFormatterBuilder()
		.appendPattern("uuuu-MM-dd HH:mm:ss")
		.optionalStart()
		.appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
		.optionalEnd()
		.toFormatter()
		.withResolverStyle(ResolverStyle.STRICT);

	private ProviderValues() {
	}

	/** 公演 ID・施設 ID として使えるか（空・20 文字を超える ID は切り詰めると別物になるので使わない） */
	public static boolean isValidId(String id) {
		String value = Texts.stripToNull(id);
		return value != null && Texts.codePointLength(value) <= ID_MAX;
	}

	/**
	 * 公演詳細を自動収集の値にする。保存しない公演は value が null（notes に理由: skippedId・skippedVenueId・skippedGenre・skippedInvalid）。
	 * 保存する公演の notes: truncated（切り詰めた項目）・posterUrlTooLong・bookingSiteNoUrl・bookingSiteUrlTooLong。
	 */
	public static Conversion<PerformanceValues> performance(KopisPerformanceDetail d) {
		List<CollectNote> notes = new ArrayList<>();
		String id = d.mt20id();
		if (!isValidId(id)) {
			return skip("skippedId", "mt20id=" + detailValue(id));
		}
		if (!isValidId(d.mt10id())) {
			return skip("skippedVenueId", id + "|mt10id=" + detailValue(d.mt10id()));
		}
		Optional<Genre> genre = Genre.fromKopisName(d.genrenm());
		if (genre.isEmpty()) {
			// genre_code は必須で、推測のコードを入れない
			return skip("skippedGenre", id + "|" + detailValue(d.genrenm()));
		}
		// 必須の列（公演名・期間）が読めない公演は保存できない
		if (d.prfnm() == null) {
			return skip("skippedInvalid", id + "|prfnm");
		}
		LocalDate start = periodDate(d.prfpdfrom());
		if (start == null) {
			return skip("skippedInvalid", id + "|prfpdfrom=" + detailValue(d.prfpdfrom()));
		}
		LocalDate end = periodDate(d.prfpdto());
		if (end == null) {
			return skip("skippedInvalid", id + "|prfpdto=" + detailValue(d.prfpdto()));
		}
		String poster = d.poster();
		if (poster != null && Texts.codePointLength(poster) > URL_MAX) {
			// 切り詰めた URL は開けないので保存しない
			notes.add(CollectNote.of("posterUrlTooLong", id));
			poster = null;
		}
		PerformanceValues values = new PerformanceValues(id, d.mt10id(),
				truncate(d.prfnm(), TITLE_MAX, id, "title", notes), start, end, genre.get().code(),
				truncateText(d.prfcast(), id, "cast", notes), truncateText(d.pcseguidance(), id, "ticketPrice", notes),
				truncateText(d.dtguidance(), id, "schedule", notes),
				truncate(d.prfruntime(), SHORT_TEXT_MAX, id, "runtime", notes),
				truncate(d.prfage(), SHORT_TEXT_MAX, id, "ageLimit", notes), poster, "Y".equals(d.openrun()),
				SEOUL.equals(d.area()), updatedAt(d.updatedate()), bookingSites(id, d.relates(), notes));
		return new Conversion<>(values, notes);
	}

	/**
	 * 施設詳細を会場の値にする。ID は公演詳細の {@code mt10id}（検査済み）を使う。
	 * 会場名がない会場は保存できない（value が null、skippedVenueInvalid）。notes: truncated・venueNoDistrict・venueNoLocation。
	 */
	public static Conversion<VenueValues> venue(String kopisVenueId, KopisVenueDetail v) {
		List<CollectNote> notes = new ArrayList<>();
		if (v.fcltynm() == null) {
			return skip("skippedVenueInvalid", kopisVenueId + "|fcltynm");
		}
		String name = truncate(v.fcltynm(), SHORT_TEXT_MAX, kopisVenueId, "venueName", notes);
		String address = truncate(v.adres(), ADDRESS_MAX, kopisVenueId, "address", notes);
		Optional<SeoulDistrict> district = DistrictParser.parse(address);
		if (district.isEmpty()) {
			notes.add(CollectNote.of("venueNoDistrict", kopisVenueId));
		}
		BigDecimal latitude = coordinate(v.la(), 90);
		BigDecimal longitude = coordinate(v.lo(), 180);
		if (latitude == null || longitude == null) {
			// 04 の CHECK は両方あるか両方ないか。座標のない会場も保存する（P-11）
			latitude = null;
			longitude = null;
			notes.add(CollectNote.of("venueNoLocation", kopisVenueId));
		}
		return new Conversion<>(new VenueValues(kopisVenueId, name, address,
				district.map(SeoulDistrict::koreanName).orElse(null), latitude, longitude), notes);
	}

	/** 住所が「서울특별시」で始まるか（ソウルの公演の判定。05 の 3.3 の 6.） */
	public static boolean isSeoulAddress(String address) {
		String value = Texts.stripToNull(address);
		return value != null && value.startsWith(SEOUL);
	}

	/**
	 * {@code updatedate}（{@code yyyy-MM-dd HH:mm:ss}、任意で小数の秒）。小数の秒は切り捨てる（列は DATETIME。06 の 7.1）。
	 * 形が違う・ありえない日時・空は null。
	 */
	static LocalDateTime updatedAt(String value) {
		String text = Texts.stripToNull(value);
		if (text == null) {
			return null;
		}
		try {
			return LocalDateTime.parse(text, UPDATED_AT).truncatedTo(ChronoUnit.SECONDS);
		}
		catch (DateTimeParseException e) {
			return null;
		}
	}

	/** 公演期間（{@code yyyy.MM.dd}）。読めなければ null */
	static LocalDate periodDate(String value) {
		String text = Texts.stripToNull(value);
		if (text == null) {
			return null;
		}
		try {
			return LocalDate.parse(text, PERIOD_DATE);
		}
		catch (DateTimeParseException e) {
			return null;
		}
	}

	/** 緯度・経度。小数第6位に HALF_UP（DECIMAL(9,6)）。空・数でない・範囲の外は null */
	static BigDecimal coordinate(String value, int limit) {
		String text = Texts.stripToNull(value);
		if (text == null) {
			return null;
		}
		BigDecimal number;
		try {
			number = new BigDecimal(text);
		}
		catch (NumberFormatException e) {
			return null;
		}
		BigDecimal scaled = number.setScale(6, RoundingMode.HALF_UP);
		BigDecimal max = BigDecimal.valueOf(limit);
		return scaled.abs().compareTo(max) > 0 ? null : scaled;
	}

	// 返された順に 1, 2, … と振る。URL がない・長すぎる項目は保存しない（05 の 3.3 の 9.・10.）
	private static List<BookingSiteValue> bookingSites(String id, List<KopisRelate> relates, List<CollectNote> notes) {
		List<BookingSiteValue> sites = new ArrayList<>();
		for (KopisRelate r : relates) {
			if (r.relateurl() == null) {
				notes.add(CollectNote.of("bookingSiteNoUrl", id));
				continue;
			}
			if (Texts.codePointLength(r.relateurl()) > URL_MAX) {
				notes.add(CollectNote.of("bookingSiteUrlTooLong", id));
				continue;
			}
			// 名前が空なら NULL（URL を名前の代わりにしない）
			String name = truncate(r.relatenm(), SHORT_TEXT_MAX, id, "bookingSiteName", notes);
			sites.add(new BookingSiteValue(name, r.relateurl(), sites.size() + 1));
		}
		return sites;
	}

	private static String truncate(String value, int max, String id, String field, List<CollectNote> notes) {
		String result = Texts.truncateCodePoints(value, max);
		if (result != null && !result.equals(value)) {
			notes.add(CollectNote.of("truncated", id + "|" + field));
		}
		return result;
	}

	private static String truncateText(String value, String id, String field, List<CollectNote> notes) {
		String result = Texts.truncateUtf8Bytes(value, TEXT_MAX_BYTES);
		if (result != null && !result.equals(value)) {
			notes.add(CollectNote.of("truncated", id + "|" + field));
		}
		return result;
	}

	private static <T> Conversion<T> skip(String name, String detail) {
		return new Conversion<>(null, List.of(CollectNote.of(name, detail)));
	}

	/** StepSummary の内容に入れる値（null は "null"、長い値は切る） */
	static String detailValue(String value) {
		return value == null ? "null" : Texts.truncateCodePoints(value, DETAIL_VALUE_MAX);
	}

}
