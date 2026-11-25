package com.catticket.collect.client;

import static com.catticket.collect.client.KopisFixtures.bytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

import com.catticket.collect.client.xml.KopisBoxOfficeItem;
import com.catticket.collect.client.xml.KopisGenreStat;
import com.catticket.collect.client.xml.KopisPerformanceDetail;
import com.catticket.collect.client.xml.KopisPerformanceListItem;
import com.catticket.collect.client.xml.KopisVenueDetail;
import com.catticket.common.batch.Retry;
import com.catticket.common.code.Genre;
import com.catticket.common.config.CatticketProperties;
import com.catticket.common.config.CatticketProperties.Kopis.CallLimit;
import com.catticket.common.display.Period;

/**
 * KopisClient（06 の 4.9・7.1・7.2）。MockRestServiceServer と実際の応答のファイルで確かめ、外部 API は呼ばない。
 */
@ExtendWith(OutputCaptureExtension.class)
class KopisClientTest {

	private static final String BASE = "http://kopis.test/openApi/restful";

	// テスト用の偽のキー（本物ではない）
	private static final String KEY = "0123456789abcdef0123456789abcdef";

	/** 再試行の待ち（05 の 3.5。30・60 秒で最大 2 回） */
	private static final List<Duration> BACKOFF = List.of(Duration.ofSeconds(30), Duration.ofSeconds(60));

	/** 遮断の印のテストで、割り当てに当たらない大きさ */
	private static final CallLimit LIMIT = new CallLimit(1500, 70, 20, 200, 5);

	private final List<Duration> slept = new ArrayList<>();

	private MockRestServiceServer server;

	private KopisCallPolicy policy;

	private KopisClient client;

	@BeforeEach
	void setUp() {
		client = newClient(KEY);
	}

	private KopisClient newClient(String key) {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
		server = MockRestServiceServer.bindTo(builder).build();
		CatticketProperties props = new CatticketProperties(null, null,
				new CatticketProperties.Kopis(BASE, key, Duration.ofSeconds(7), LIMIT), null, null, null, null, null,
				null);
		policy = new KopisCallPolicy(new Retry(slept::add), slept::add, props);
		policy.startJob();
		policy.startStep("S1");
		return new KopisClient(builder.build(), props, policy);
	}

	private static ResponseCreator xml(String file) {
		return withSuccess(bytes(file), MediaType.APPLICATION_XML);
	}

	@Test
	void listPerformancesSendsQueryAndReadsFullPage() {
		server.expect(once(), requestTo(startsWith(BASE + "/pblprfr?")))
			.andExpect(method(HttpMethod.GET))
			.andExpect(queryParam("stdate", "20261120"))
			.andExpect(queryParam("eddate", "20261220"))
			.andExpect(queryParam("cpage", "1"))
			.andExpect(queryParam("rows", "100"))
			.andExpect(queryParam("signgucode", "11"))
			.andExpect(queryParam("afterdate", "20261119"))
			.andExpect(queryParam("service", KEY))
			.andRespond(xml("performance-list-page-100.xml"));

		KopisPage<KopisPerformanceListItem> page = client.listPerformances(LocalDate.of(2026, 11, 20),
				LocalDate.of(2026, 12, 20), LocalDate.of(2026, 11, 19), 1);

		server.verify();
		assertThat(page.items()).hasSize(100);
		// 100 件ちょうどなら次のページを照会する
		assertThat(page.isLast()).isFalse();
	}

	@Test
	void listPerformancesWithoutAfterDate() {
		server.expect(once(), requestTo(BASE + "/pblprfr?stdate=20261120&eddate=20261220&cpage=12&rows=100"
				+ "&signgucode=11&service=" + KEY))
			.andRespond(xml("performance-list-last-page.xml"));

		KopisPage<KopisPerformanceListItem> page = client.listPerformances(LocalDate.of(2026, 11, 20),
				LocalDate.of(2026, 12, 20), null, 12);

		server.verify();
		assertThat(page.items()).hasSize(73);
		assertThat(page.isLast()).isTrue();
	}

	@Test
	void emptyListIsLastPage() {
		server.expect(once(), requestTo(startsWith(BASE + "/pblprfr?"))).andRespond(xml("performance-list-empty.xml"));

		KopisPage<KopisPerformanceListItem> page = client.listPerformances(LocalDate.of(2026, 11, 20),
				LocalDate.of(2026, 12, 20), null, 13);

		assertThat(page.items()).isEmpty();
		assertThat(page.isLast()).isTrue();
	}

	@Test
	void getPerformance() {
		server.expect(once(), requestTo(BASE + "/pblprfr/PF303043?service=" + KEY))
			.andRespond(xml("performance-detail-relates.xml"));

		Optional<KopisPerformanceDetail> detail = client.getPerformance("PF303043");

		server.verify();
		assertThat(detail).hasValueSatisfying(d -> {
			assertThat(d.mt20id()).isEqualTo("PF303043");
			assertThat(d.relates()).hasSize(3);
		});
	}

	@Test
	void getPerformanceReturnCode04IsEmpty() {
		server.expect(once(), requestTo(BASE + "/pblprfr/PF000000?service=" + KEY))
			.andRespond(xml("performance-detail-04.xml"));

		assertThat(client.getPerformance("PF000000")).isEmpty();
		server.verify();
		assertThat(slept).isEmpty();
	}

	@Test
	void getVenue() {
		server.expect(once(), requestTo(BASE + "/prfplc/FC004949?service=" + KEY)).andRespond(xml("venue-detail.xml"));

		Optional<KopisVenueDetail> venue = client.getVenue("FC004949");

		assertThat(venue.map(KopisVenueDetail::adres)).contains("서울특별시 마포구 독막로7길 23 (서교동)");
	}

	@Test
	void getVenueReturnCode04IsEmpty() {
		server.expect(once(), requestTo(BASE + "/prfplc/FC000000?service=" + KEY))
			.andRespond(xml("venue-detail-04.xml"));

		assertThat(client.getVenue("FC000000")).isEmpty();
	}

	@Test
	void boxOfficeAllGenres() {
		// cpage・rows・catecode は送らない
		server.expect(once(), requestTo(BASE + "/boxoffice?stdate=20261119&eddate=20261119&area=11&service=" + KEY))
			.andRespond(xml("boxoffice-50.xml"));

		List<KopisBoxOfficeItem> rows = client
			.boxOffice(new Period(LocalDate.of(2026, 11, 19), LocalDate.of(2026, 11, 19)), Optional.empty());

		server.verify();
		assertThat(rows).hasSize(50);
	}

	@Test
	void boxOfficeOneGenre() {
		server.expect(once(), requestTo(BASE + "/boxoffice?stdate=20261001&eddate=20261031&area=11&catecode=BBBC"
				+ "&service=" + KEY))
			.andRespond(xml("boxoffice-1.xml"));

		List<KopisBoxOfficeItem> rows = client
			.boxOffice(new Period(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)), Optional.of(Genre.BBBC));

		server.verify();
		assertThat(rows).containsExactly(new KopisBoxOfficeItem("1", "PF306800"));
	}

	@Test
	void boxOfficeEmptyIsEmptyList() {
		server.expect(once(), requestTo(startsWith(BASE + "/boxoffice?"))).andRespond(xml("boxoffice-empty.xml"));

		assertThat(client.boxOffice(new Period(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 20)),
				Optional.empty()))
			.isEmpty();
	}

	@Test
	void awards() {
		server.expect(once(), requestTo(BASE + "/prfawad?stdate=20261120&eddate=20261220&cpage=1&rows=100"
				+ "&signgucode=11&service=" + KEY))
			.andRespond(xml("awards.xml"));

		KopisPage<?> page = client.awards(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 12, 20), 1);

		server.verify();
		assertThat(page.items()).hasSize(5);
		assertThat(page.isLast()).isTrue();
	}

	@Test
	void genreStats() {
		// 地域の変数は送らない（効かない）
		server.expect(once(), requestTo(BASE + "/boxStatsCate?stdate=20261021&eddate=20261119&service=" + KEY))
			.andRespond(xml("genre-stats.xml"));

		List<KopisGenreStat> rows = client
			.genreStats(new Period(LocalDate.of(2026, 10, 21), LocalDate.of(2026, 11, 19)));

		server.verify();
		assertThat(rows).hasSize(10).extracting(KopisGenreStat::catenm).contains("합계");
	}

	@ParameterizedTest
	@CsvSource({ "error-01.xml,01", "error-02.xml,02", "error-05.xml,05", "error-06.xml,06", "error-99.xml,99" })
	void fatalReturnCodesAreNotRetried(String file, String code) {
		server.expect(once(), requestTo(startsWith(BASE + "/boxoffice?"))).andRespond(xml(file));

		assertThatThrownBy(() -> client
			.boxOffice(new Period(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 31)), Optional.empty()))
			.isInstanceOf(KopisFatalException.class)
			.hasMessageStartingWith("KOPIS boxoffice returncode=" + code + " errmsg=");

		// 1回だけ呼ぶ
		server.verify();
		assertThat(slept).isEmpty();
	}

	@Test
	void returnCode03IsRetriedThenSucceeds() {
		server.expect(times(2), requestTo(startsWith(BASE + "/pblprfr/PF303043"))).andRespond(xml("error-03.xml"));
		server.expect(once(), requestTo(startsWith(BASE + "/pblprfr/PF303043")))
			.andRespond(xml("performance-detail-relates.xml"));

		assertThat(client.getPerformance("PF303043")).isPresent();

		server.verify();
		assertThat(slept).containsExactlyElementsOf(BACKOFF);
	}

	@Test
	void returnCode03GivesUpAfterTwoRetries() {
		server.expect(times(3), requestTo(startsWith(BASE + "/pblprfr/PF303043"))).andRespond(xml("error-03.xml"));

		assertThatThrownBy(() -> client.getPerformance("PF303043")).isInstanceOf(KopisRetryableException.class)
			.hasMessage("KOPIS pblprfr/{id} returncode=03 errmsg=DB ERROR");

		server.verify();
		assertThat(slept).containsExactlyElementsOf(BACKOFF);
	}

	@Test
	void serverErrorIsRetried() {
		server.expect(once(), requestTo(startsWith(BASE + "/prfplc/FC004949"))).andRespond(withServerError());
		server.expect(once(), requestTo(startsWith(BASE + "/prfplc/FC004949")))
			.andRespond(withStatus(HttpStatus.BAD_GATEWAY));
		server.expect(once(), requestTo(startsWith(BASE + "/prfplc/FC004949"))).andRespond(xml("venue-detail.xml"));

		assertThat(client.getVenue("FC004949")).isPresent();

		server.verify();
		assertThat(slept).containsExactlyElementsOf(BACKOFF);
	}

	@Test
	void timeoutIsRetried() {
		server.expect(once(), requestTo(startsWith(BASE + "/boxStatsCate")))
			.andRespond(withException(new HttpTimeoutException("request timed out")));
		server.expect(once(), requestTo(startsWith(BASE + "/boxStatsCate"))).andRespond(xml("genre-stats.xml"));

		assertThat(client.genreStats(new Period(LocalDate.of(2026, 10, 21), LocalDate.of(2026, 11, 19)))).hasSize(10);

		server.verify();
		assertThat(slept).containsExactly(Duration.ofSeconds(30));
	}

	@Test
	void timeoutGivesUpAsRetryable() {
		server.expect(times(3), requestTo(startsWith(BASE + "/boxStatsCate")))
			.andRespond(withException(new HttpTimeoutException("request timed out")));

		assertThatThrownBy(
				() -> client.genreStats(new Period(LocalDate.of(2026, 10, 21), LocalDate.of(2026, 11, 19))))
			.isInstanceOf(KopisRetryableException.class)
			.hasMessage("KOPIS boxStatsCate I/O error: HttpTimeoutException: request timed out")
			.hasCauseInstanceOf(HttpTimeoutException.class);
		server.verify();
	}

	@Test
	void redirectIsFatal() {
		// https で呼んだときの 301 など。リダイレクトに頼らず、再試行もしない（05 の 3.5）
		server.expect(once(), requestTo(startsWith(BASE + "/pblprfr/PF1")))
			.andRespond(withStatus(HttpStatus.MOVED_PERMANENTLY).header("Location", "https://kopis.test/x"));

		assertThatThrownBy(() -> client.getPerformance("PF1")).isInstanceOf(KopisFatalException.class)
			.hasMessage("KOPIS pblprfr/{id} HTTP 301");
		server.verify();
		assertThat(slept).isEmpty();
	}

	@Test
	void serverErrorGivesUpAsRetryable() {
		server.expect(times(3), requestTo(startsWith(BASE + "/prfplc/FC1"))).andRespond(withServerError());

		assertThatThrownBy(() -> client.getVenue("FC1")).isInstanceOf(KopisRetryableException.class)
			.hasMessage("KOPIS prfplc/{id} HTTP 500");
		server.verify();
		assertThat(slept).containsExactlyElementsOf(BACKOFF);
	}

	@Test
	void unexpectedWellFormedPageIsRetried() {
		// 行要素のない応答を 0 件にしない（06 の 7.1 の 6.）
		server.expect(once(), requestTo(startsWith(BASE + "/pblprfr?")))
			.andRespond(withSuccess(bytes("maintenance.html"), MediaType.TEXT_HTML));
		server.expect(once(), requestTo(startsWith(BASE + "/pblprfr?"))).andRespond(xml("performance-list-last-page.xml"));

		assertThat(client.listPerformances(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 12, 20), null, 12).items())
			.hasSize(73);
		server.verify();
		assertThat(slept).containsExactly(Duration.ofSeconds(30));
	}

	@Test
	void truncatedBodyIsRetried() {
		server.expect(once(), requestTo(startsWith(BASE + "/pblprfr?"))).andRespond(xml("performance-list-truncated.xml"));
		server.expect(once(), requestTo(startsWith(BASE + "/pblprfr?"))).andRespond(xml("performance-list-page-100.xml"));

		assertThat(client.listPerformances(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 12, 20), null, 1).items())
			.hasSize(100);
		server.verify();
		assertThat(slept).containsExactly(Duration.ofSeconds(30));
	}

	@Test
	void fiveConsecutiveFailedCallsStopTheRun() {
		// 5 回の呼び出し × (最初 + 再試行 2 回)。どれも 5xx
		server.expect(times(15), requestTo(startsWith(BASE + "/pblprfr/"))).andRespond(withServerError());

		for (int i = 1; i <= 4; i++) {
			String id = "PF" + i;
			assertThatThrownBy(() -> client.getPerformance(id)).isInstanceOf(KopisRetryableException.class);
		}
		assertThatThrownBy(() -> client.getPerformance("PF5")).isInstanceOf(KopisStoppedException.class)
			.hasMessageContaining("5 consecutive failures");

		// 止めた後は送らない（16 回目の送信はない）
		assertThatThrownBy(() -> client.getVenue("FC1")).isInstanceOf(KopisStoppedException.class);
		server.verify();
	}

	@Nested
	class BlockedSigns {

		private void assertBlocked(Runnable call, String message) {
			KopisStoppedException e = catchThrowableOfType(KopisStoppedException.class, call::run);
			assertThat(e.reason()).isEqualTo(KopisStopReason.BLOCKED);
			assertThat(e.getCause()).isInstanceOf(KopisBlockedException.class).hasMessage(message);
			assertThat(e.getMessage()).doesNotContain(KEY).doesNotContain("http");
			// 再試行しない（1回だけ送る）
			server.verify();
			assertThat(slept).isEmpty();
			assertThat(policy.stopped()).isTrue();
		}

		@Test
		void http400RequestBlockedHtml() {
			server.expect(once(), requestTo(startsWith(BASE + "/pblprfr?")))
				.andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_HTML)
					.body(bytes("blocked-400.html")));

			assertBlocked(() -> client.listPerformances(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 12, 20), null, 1),
					"KOPIS pblprfr blocked: HTTP 400 (Request Blocked)");
		}

		@Test
		void stoppedRunSendsNothingMore() {
			server.expect(once(), requestTo(startsWith(BASE + "/pblprfr?")))
				.andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_HTML)
					.body(bytes("blocked-400.html")));
			assertThatThrownBy(() -> client.listPerformances(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 12, 20),
					null, 1))
				.isInstanceOf(KopisStoppedException.class);

			// 期待していない送信があれば MockRestServiceServer が失敗させる
			assertThatThrownBy(() -> client.getPerformance("PF1")).isInstanceOf(KopisStoppedException.class)
				.hasMessageContaining("KOPIS calls stopped in this run: S1: KOPIS blocked:");
			server.verify();
		}

		@ParameterizedTest
		@CsvSource({ "403,", "403,error-01.xml", "429,", "429,error-01.xml" })
		void http403And429AreBlockedWhateverTheBody(int status, String file) {
			ResponseCreator response = file == null ? withStatus(HttpStatus.valueOf(status))
					: withStatus(HttpStatus.valueOf(status)).contentType(MediaType.APPLICATION_XML).body(bytes(file));
			server.expect(once(), requestTo(startsWith(BASE + "/boxoffice?"))).andRespond(response);

			assertBlocked(() -> client
				.boxOffice(new Period(LocalDate.of(2026, 11, 19), LocalDate.of(2026, 11, 19)), Optional.empty()),
					"KOPIS boxoffice blocked: HTTP " + status);
		}

		@Test
		void http404HtmlIsBlocked() {
			server.expect(once(), requestTo(startsWith(BASE + "/pblprfr/PF1")))
				.andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_HTML)
					.body("<html><body>Not Found</body></html>"));

			assertBlocked(() -> client.getPerformance("PF1"), "KOPIS pblprfr/{id} blocked: HTTP 404 (non-XML body)");
		}

		@Test
		void http400EmptyBodyIsBlocked() {
			server.expect(once(), requestTo(startsWith(BASE + "/prfplc/FC1")))
				.andRespond(withStatus(HttpStatus.BAD_REQUEST));

			assertBlocked(() -> client.getVenue("FC1"), "KOPIS prfplc/{id} blocked: HTTP 400 (non-XML body)");
		}

		@Test
		void http200WithTheBlockedBodyIsBlocked() {
			server.expect(once(), requestTo(startsWith(BASE + "/prfawad?")))
				.andRespond(withSuccess(bytes("blocked-200.html"), MediaType.TEXT_HTML));

			assertBlocked(() -> client.awards(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 12, 20), 1),
					"KOPIS prfawad blocked: HTTP 200 (Request Blocked)");
		}

		@Test
		void http503WithRequestBlockedIsBlocked() {
			server.expect(once(), requestTo(startsWith(BASE + "/boxStatsCate")))
				.andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("request blocked"));

			assertBlocked(
					() -> client.genreStats(new Period(LocalDate.of(2026, 10, 21), LocalDate.of(2026, 11, 19))),
					"KOPIS boxStatsCate blocked: HTTP 503 (Request Blocked)");
		}

		@Test
		void http400WithReturnCodeXmlIsNotBlocked() {
			server.expect(once(), requestTo(startsWith(BASE + "/boxoffice?")))
				.andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_XML)
					.body(bytes("error-01.xml")));

			assertThatThrownBy(() -> client
				.boxOffice(new Period(LocalDate.of(2026, 11, 19), LocalDate.of(2026, 11, 19)), Optional.empty()))
				.isInstanceOf(KopisFatalException.class)
				.hasMessageStartingWith("KOPIS boxoffice returncode=01 errmsg=");
			server.verify();
			assertThat(slept).isEmpty();
			assertThat(policy.stopped()).isFalse();
		}

		@Test
		void maintenancePageWithoutRequestBlockedIsStillRetryable() {
			server.expect(times(3), requestTo(startsWith(BASE + "/pblprfr?")))
				.andRespond(withSuccess(bytes("maintenance.html"), MediaType.TEXT_HTML));

			assertThatThrownBy(() -> client.listPerformances(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 12, 20),
					null, 1))
				.isInstanceOf(KopisRetryableException.class);
			server.verify();
			assertThat(policy.stopped()).isFalse();
		}

		@Test
		void serverErrorWithoutBodyIsStillRetryable() {
			server.expect(times(3), requestTo(startsWith(BASE + "/prfplc/FC1")))
				.andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

			assertThatThrownBy(() -> client.getVenue("FC1")).isInstanceOf(KopisRetryableException.class)
				.hasMessage("KOPIS prfplc/{id} HTTP 503");
			server.verify();
			assertThat(policy.stopped()).isFalse();
		}

		@Test
		void blockedLogHasNoKey(CapturedOutput output) {
			server.expect(once(), requestTo(startsWith(BASE + "/pblprfr/PF1")))
				.andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_HTML)
					.body(bytes("blocked-400.html")));

			assertThatThrownBy(() -> client.getPerformance("PF1")).isInstanceOf(KopisStoppedException.class);

			assertThat(output.getAll()).contains("KOPIS GET " + BASE + "/pblprfr/PF1?service=*** blocked")
				.contains("Request Blocked")
				.doesNotContain(KEY);
		}

	}

	@Test
	void missingApiKeyFailsWithoutCalling() {
		client = newClient("");

		assertThatThrownBy(() -> client.getVenue("FC1")).isInstanceOf(KopisFatalException.class)
			.hasMessageContaining("api key is not set");
		server.verify();
	}

	@Test
	void logsMaskTheApiKey(CapturedOutput output) {
		server.expect(times(3), requestTo(startsWith(BASE + "/pblprfr/PF1")))
			.andRespond(withException(new ConnectException("Connection refused")));

		assertThatThrownBy(() -> client.getPerformance("PF1")).isInstanceOf(KopisRetryableException.class)
			.satisfies(e -> assertThat(e.getMessage()).doesNotContain(KEY));

		assertThat(output.getAll()).contains("KOPIS GET " + BASE + "/pblprfr/PF1?service=*** failed")
			.doesNotContain(KEY);
	}

	@Test
	void maskKeyReplacesServiceParameter() {
		assertThat(KopisClient.maskKey(BASE + "/boxoffice?service=" + KEY + "&stdate=20261119"))
			.isEqualTo(BASE + "/boxoffice?service=***&stdate=20261119");
		assertThat(KopisClient.maskKey(BASE + "/pblprfr/PF1?stdate=1&service=" + KEY))
			.isEqualTo(BASE + "/pblprfr/PF1?stdate=1&service=***");
		assertThat(KopisClient.maskKey(null)).isNull();
	}

}
