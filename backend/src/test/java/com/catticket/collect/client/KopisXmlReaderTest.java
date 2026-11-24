package com.catticket.collect.client;

import static com.catticket.collect.client.KopisFixtures.bytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.catticket.collect.client.xml.AwardListResponse;
import com.catticket.collect.client.xml.BoxOfficeResponse;
import com.catticket.collect.client.xml.GenreStatsResponse;
import com.catticket.collect.client.xml.KopisAwardItem;
import com.catticket.collect.client.xml.KopisBoxOfficeItem;
import com.catticket.collect.client.xml.KopisGenreStat;
import com.catticket.collect.client.xml.KopisPerformanceDetail;
import com.catticket.collect.client.xml.KopisPerformanceListItem;
import com.catticket.collect.client.xml.KopisRelate;
import com.catticket.collect.client.xml.KopisVenueDetail;
import com.catticket.collect.client.xml.PerformanceDetailResponse;
import com.catticket.collect.client.xml.PerformanceListResponse;
import com.catticket.collect.client.xml.VenueDetailResponse;

/**
 * KOPIS の XML の読み方（06 の 7.1）。応答は実際の KOPIS のもの（external/kopis/README.md）。
 */
class KopisXmlReaderTest {

	private final KopisXmlReader reader = new KopisXmlReader();

	@Test
	void readsPerformanceListPage() {
		List<KopisPerformanceListItem> rows = reader.readRows("pblprfr", bytes("performance-list-page-100.xml"),
				PerformanceListResponse.class);

		assertThat(rows).hasSize(100);
		assertThat(rows).allSatisfy(r -> assertThat(r.mt20id()).startsWith("PF"));
		assertThat(rows.get(0).mt20id()).isEqualTo("PF307015");
	}

	@Test
	void readsLastPage() {
		assertThat(reader.readRows("pblprfr", bytes("performance-list-last-page.xml"), PerformanceListResponse.class))
			.hasSize(73);
	}

	@ParameterizedTest
	@ValueSource(strings = { "performance-list-empty.xml", "boxoffice-empty.xml" })
	void emptyRootIsNoRows(String file) {
		// <dbs/>・<boxofs/> は 0 件（04 ではない。05 の 3.5）
		assertThat(reader.readRows("x", bytes(file), PerformanceListResponse.class)).isEmpty();
		assertThat(reader.readRows("x", bytes(file), BoxOfficeResponse.class)).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "<box-statsofs/>", "<box-statsofs></box-statsofs>", "<dbs>  </dbs>",
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><prfsts/>" })
	void otherEmptyRootsAreNoRows(String xml) {
		assertThat(reader.readRows("boxStatsCate", xml.getBytes(StandardCharsets.UTF_8), GenreStatsResponse.class))
			.isEmpty();
	}

	@Test
	void readsDetailWithRelates() {
		List<KopisPerformanceDetail> rows = reader.readRows("pblprfr/{id}", bytes("performance-detail-relates.xml"),
				PerformanceDetailResponse.class);

		assertThat(rows).hasSize(1);
		KopisPerformanceDetail d = rows.get(0);
		assertThat(d.mt20id()).isEqualTo("PF303043");
		assertThat(d.prfnm()).isEqualTo("스위니토드 (SWEENEY TODD)");
		assertThat(d.prfpdfrom()).isEqualTo("2026.12.04");
		assertThat(d.prfpdto()).isEqualTo("2027.05.02");
		assertThat(d.prfcast()).startsWith("조승우");
		assertThat(d.prfruntime()).isEqualTo("2시간 50분");
		assertThat(d.prfage()).isEqualTo("만 13세 이상");
		assertThat(d.pcseguidance()).startsWith("VIP석 180,000원");
		assertThat(d.dtguidance()).startsWith("화요일(19:30)");
		assertThat(d.poster()).isEqualTo("http://www.kopis.or.kr/upload/pfmPoster/PF_PF303043_261012_171201.gif");
		assertThat(d.area()).isEqualTo("서울특별시");
		assertThat(d.genrenm()).isEqualTo("뮤지컬");
		assertThat(d.openrun()).isEqualTo("N");
		assertThat(d.updatedate()).isEqualTo("2026-11-09 14:52:19");
		assertThat(d.mt10id()).isEqualTo("FC000011");
		assertThat(d.relates()).containsExactly(
				new KopisRelate("NHN티켓링크", "https://www.ticketlink.co.kr/product/66145"),
				new KopisRelate("네이버N예약", "https://booking.naver.com/booking/12/bizes/1773733"),
				new KopisRelate("놀유니버스",
						"http://ticket.interpark.com/Ticket/Goods/GoodsInfo.asp?GoodsCode=26014509"));
	}

	@Test
	void singleRelateIsStillAList() {
		KopisPerformanceDetail d = reader
			.readRows("pblprfr/{id}", bytes("performance-detail-blank-items.xml"), PerformanceDetailResponse.class)
			.get(0);

		assertThat(d.relates()).containsExactly(
				new KopisRelate("네이버N예약", "https://booking.naver.com/booking/12/bizes/1661745"));
	}

	@Test
	void blankItemsBecomeNull() {
		KopisPerformanceDetail d = reader
			.readRows("pblprfr/{id}", bytes("performance-detail-blank-items.xml"), PerformanceDetailResponse.class)
			.get(0);

		// <prfcast> </prfcast> は「ない」
		assertThat(d.prfcast()).isNull();
		assertThat(d.genrenm()).isEqualTo("연극");
		assertThat(d.updatedate()).isEqualTo("2026-11-06 18:00:03.02675");
	}

	@Test
	void detailWithoutRelatesHasEmptyList() {
		KopisPerformanceDetail d = reader
			.readRows("pblprfr/{id}", bytes("performance-detail-no-relates.xml"), PerformanceDetailResponse.class)
			.get(0);

		assertThat(d.mt20id()).isEqualTo("PF304568");
		assertThat(d.relates()).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "<relates/>", "<relates></relates>", "<relates> </relates>" })
	void emptyRelatesIsEmptyList(String relates) {
		String xml = "<dbs><db><mt20id>PF1</mt20id>" + relates + "</db></dbs>";

		KopisPerformanceDetail d = reader
			.readRows("pblprfr/{id}", xml.getBytes(StandardCharsets.UTF_8), PerformanceDetailResponse.class)
			.get(0);

		assertThat(d.relates()).isEmpty();
	}

	@Test
	void relateWithBlankNameKeepsUrl() {
		String xml = "<dbs><db><mt20id>PF1</mt20id><relates><relate><relatenm> </relatenm>"
				+ "<relateurl>https://a.example/</relateurl></relate><relate><relatenm>B</relatenm></relate>"
				+ "</relates></db></dbs>";

		KopisPerformanceDetail d = reader
			.readRows("pblprfr/{id}", xml.getBytes(StandardCharsets.UTF_8), PerformanceDetailResponse.class)
			.get(0);

		assertThat(d.relates()).containsExactly(new KopisRelate(null, "https://a.example/"),
				new KopisRelate("B", null));
	}

	@Test
	void readsVenue() {
		List<KopisVenueDetail> rows = reader.readRows("prfplc/{id}", bytes("venue-detail.xml"),
				VenueDetailResponse.class);

		assertThat(rows).containsExactly(new KopisVenueDetail("FC004949", "로드 유미플레이 1",
				"서울특별시 마포구 독막로7길 23 (서교동)", "37.548989000000000", "126.91894500000000"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "performance-detail-04.xml", "venue-detail-04.xml" })
	void returnCode04IsNoRows(String file) {
		assertThat(reader.readRows("x", bytes(file), PerformanceDetailResponse.class)).isEmpty();
	}

	@Test
	void readsBoxOffice() {
		List<KopisBoxOfficeItem> rows = reader.readRows("boxoffice", bytes("boxoffice-50.xml"),
				BoxOfficeResponse.class);

		assertThat(rows).hasSize(50);
		assertThat(rows.get(0)).isEqualTo(new KopisBoxOfficeItem("1", "PF306800"));
		assertThat(rows.get(49).rnum()).isEqualTo("50");
	}

	@Test
	void readsSingleBoxOfficeRow() {
		assertThat(reader.readRows("boxoffice", bytes("boxoffice-1.xml"), BoxOfficeResponse.class))
			.containsExactly(new KopisBoxOfficeItem("1", "PF306800"));
	}

	@Test
	void readsAwards() {
		List<KopisAwardItem> rows = reader.readRows("prfawad", bytes("awards.xml"), AwardListResponse.class);

		assertThat(rows).hasSize(5);
		assertThat(rows.get(0).mt20id()).isEqualTo("PF306970");
	}

	@Test
	void readsGenreStatsIncludingTotal() {
		List<KopisGenreStat> rows = reader.readRows("boxStatsCate", bytes("genre-stats.xml"),
				GenreStatsResponse.class);

		// 9 ジャンル + 합계。負の totnmrssm があっても読める（使わない）
		assertThat(rows).hasSize(10);
		assertThat(rows).contains(new KopisGenreStat("대중무용", "2758"), new KopisGenreStat("합계", "3428245"),
				new KopisGenreStat("무용(서양/한국무용)", "117300"));
	}

	@ParameterizedTest
	@CsvSource({ "error-01.xml,01,INVALID REQUEST PARAMETER ERROR", "error-02.xml,02,SERVICE KEY IS NOT REGISTERED ERROR",
			"error-05.xml,05,최대 31일까지 조회가능합니다.", "error-06.xml,06,최대 조회수는 100건까지 가능합니다.",
			"error-99.xml,99,UNKNOWN ERROR" })
	void fatalReturnCodes(String file, String code, String errmsg) {
		// boxoffice のエラーも根要素は dbs
		assertThatThrownBy(() -> reader.readRows("boxoffice", bytes(file), BoxOfficeResponse.class))
			.isInstanceOf(KopisFatalException.class)
			.hasMessage("KOPIS boxoffice returncode=" + code + " errmsg=" + errmsg);
	}

	@Test
	void returnCode03IsRetryable() {
		assertThatThrownBy(() -> reader.readRows("pblprfr", bytes("error-03.xml"), PerformanceListResponse.class))
			.isInstanceOf(KopisRetryableException.class)
			.hasMessage("KOPIS pblprfr returncode=03 errmsg=DB ERROR");
	}

	@ParameterizedTest
	@ValueSource(strings = { "<html><body>maintenance", "", "not xml" })
	void unreadableBodyIsRetryable(String body) {
		assertThatThrownBy(
				() -> reader.readRows("pblprfr", body.getBytes(StandardCharsets.UTF_8), PerformanceListResponse.class))
			.isInstanceOf(KopisRetryableException.class)
			.hasMessageStartingWith("KOPIS pblprfr unreadable response");
	}

}
