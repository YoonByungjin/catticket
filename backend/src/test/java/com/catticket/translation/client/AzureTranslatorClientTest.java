package com.catticket.translation.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpTimeoutException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.catticket.common.config.CatticketProperties;

/**
 * AzureTranslatorClient（06 の 4.10・7.2）。応答は Translator v3 の公式の形のファイル（external/azure/）。外部 API は呼ばない。
 */
class AzureTranslatorClientTest {

	private static final String ENDPOINT = "https://translator.test";

	private static final String URL = ENDPOINT + "/translate?api-version=3.0&from=ko&to=ja";

	// テスト用の偽のキー
	private static final String KEY = "test-azure-key";

	private MockRestServiceServer server;

	private AzureTranslatorClient newClient(String key, String region) {
		RestClient.Builder builder = RestClient.builder().baseUrl(ENDPOINT);
		server = MockRestServiceServer.bindTo(builder).build();
		CatticketProperties props = new CatticketProperties(null, null, null,
				new CatticketProperties.Azure(ENDPOINT, key, region, 10000, 30000, 1800000), null, null, null, null,
				null);
		return new AzureTranslatorClient(builder.build(), props);
	}

	private static byte[] json(String name) {
		try (InputStream in = AzureTranslatorClientTest.class.getResourceAsStream("/external/azure/" + name)) {
			return in.readAllBytes();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Test
	void translatesInInputOrder() {
		AzureTranslatorClient client = newClient(KEY, "koreacentral");
		server.expect(once(), requestTo(URL))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header("Ocp-Apim-Subscription-Key", KEY))
			.andExpect(header("Ocp-Apim-Subscription-Region", "koreacentral"))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(content().json("[{\"Text\":\"오페라의 유령\"},{\"Text\":\"샤롯데씨어터\"},{\"Text\":\"전석 69,000원\"}]",
					JsonCompareMode.STRICT))
			.andRespond(withSuccess(json("translate-success.json"), MediaType.APPLICATION_JSON));

		List<String> result = client.translate(List.of("오페라의 유령", "샤롯데씨어터", "전석 69,000원"));

		server.verify();
		assertThat(result).containsExactly("オペラ座の怪人", "シャーロットシアター", "全席 69,000ウォン");
	}

	@Test
	void globalResourceSendsNoRegion() {
		AzureTranslatorClient client = newClient(KEY, "");
		server.expect(once(), requestTo(URL))
			.andExpect(headerDoesNotExist("Ocp-Apim-Subscription-Region"))
			.andRespond(withSuccess(json("translate-success.json"), MediaType.APPLICATION_JSON));

		assertThat(client.translate(List.of("a", "b", "c"))).hasSize(3);
		server.verify();
	}

	@Test
	void emptyInputDoesNotCall() {
		AzureTranslatorClient client = newClient(KEY, null);

		assertThat(client.translate(List.of())).isEmpty();
		server.verify();
	}

	@Test
	void quotaExceeded403001() {
		AzureTranslatorClient client = newClient(KEY, null);
		server.expect(once(), requestTo(URL))
			.andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
				.body(json("error-403001.json")));

		assertThatThrownBy(() -> client.translate(List.of("a"))).isInstanceOf(TranslatorQuotaExceededException.class)
			.hasMessageStartingWith("Azure Translator HTTP 403 code=403001");
	}

	@Test
	void throttled429() {
		AzureTranslatorClient client = newClient(KEY, null);
		server.expect(once(), requestTo(URL))
			.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON)
				.body(json("error-429000.json")));

		assertThatThrownBy(() -> client.translate(List.of("a"))).isInstanceOf(TranslatorThrottledException.class)
			.hasMessageStartingWith("Azure Translator HTTP 429 code=429000");
	}

	@Test
	void throttledWithoutJsonBody() {
		AzureTranslatorClient client = newClient(KEY, null);
		server.expect(once(), requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

		assertThatThrownBy(() -> client.translate(List.of("a"))).isInstanceOf(TranslatorThrottledException.class);
	}

	@Test
	void unauthorizedIsFatal() {
		AzureTranslatorClient client = newClient(KEY, null);
		server.expect(once(), requestTo(URL))
			.andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
				.body(json("error-401000.json")));

		assertThatThrownBy(() -> client.translate(List.of("a"))).isExactlyInstanceOf(TranslatorFatalException.class)
			.hasMessageStartingWith("Azure Translator HTTP 401 code=401000")
			.satisfies(e -> assertThat(e.getMessage()).doesNotContain(KEY));
	}

	@Test
	void badRequestIsFatal() {
		AzureTranslatorClient client = newClient(KEY, null);
		server.expect(once(), requestTo(URL))
			.andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
				.body("{\"error\":{\"code\":400050,\"message\":\"The input text is too long.\"}}"));

		assertThatThrownBy(() -> client.translate(List.of("a"))).isExactlyInstanceOf(TranslatorFatalException.class)
			.hasMessageStartingWith("Azure Translator HTTP 400 code=400050");
	}

	@Test
	void forbiddenWithoutQuotaCodeIsFatal() {
		AzureTranslatorClient client = newClient(KEY, null);
		server.expect(once(), requestTo(URL))
			.andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
				.body("{\"error\":{\"code\":403000,\"message\":\"The operation is not allowed.\"}}"));

		assertThatThrownBy(() -> client.translate(List.of("a"))).isExactlyInstanceOf(TranslatorFatalException.class)
			.hasMessageStartingWith("Azure Translator HTTP 403 code=403000");
	}

	@Test
	void requestTimeoutIsRetryable() {
		AzureTranslatorClient client = newClient(KEY, null);
		server.expect(once(), requestTo(URL))
			.andRespond(withStatus(HttpStatus.REQUEST_TIMEOUT).contentType(MediaType.APPLICATION_JSON)
				.body("{\"error\":{\"code\":408001,\"message\":\"The request timed out.\"}}"));

		// 408 は一時的なので再試行の対象（TranslatorFatalException でない）
		assertThatThrownBy(() -> client.translate(List.of("a"))).isExactlyInstanceOf(TranslatorException.class)
			.hasMessageStartingWith("Azure Translator HTTP 408");
	}

	@Test
	void serviceUnavailableIsRetryable() {
		AzureTranslatorClient client = newClient(KEY, null);
		server.expect(once(), requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

		assertThatThrownBy(() -> client.translate(List.of("a"))).isExactlyInstanceOf(TranslatorException.class)
			.hasMessageStartingWith("Azure Translator HTTP 503");
	}

	@Test
	void serverErrorIsTranslatorException() {
		AzureTranslatorClient client = newClient(KEY, null);
		server.expect(once(), requestTo(URL)).andRespond(withServerError());

		assertThatThrownBy(() -> client.translate(List.of("a"))).isExactlyInstanceOf(TranslatorException.class)
			.hasMessageStartingWith("Azure Translator HTTP 500");
	}

	@Test
	void timeoutIsTranslatorException() {
		AzureTranslatorClient client = newClient(KEY, null);
		server.expect(once(), requestTo(URL)).andRespond(withException(new HttpTimeoutException("request timed out")));

		assertThatThrownBy(() -> client.translate(List.of("a"))).isExactlyInstanceOf(TranslatorException.class)
			.hasMessage("Azure Translator I/O error: HttpTimeoutException: request timed out");
	}

	@Test
	void countMismatchIsTranslatorException() {
		AzureTranslatorClient client = newClient(KEY, null);
		server.expect(once(), requestTo(URL))
			.andRespond(withSuccess(json("translate-success.json"), MediaType.APPLICATION_JSON));

		// 3 件の応答に 2 件の入力。順番で結べないので失敗にする
		assertThatThrownBy(() -> client.translate(List.of("a", "b"))).isExactlyInstanceOf(TranslatorException.class)
			.hasMessageContaining("returned 3 items for 2 texts");
	}

	@Test
	void missingTextIsTranslatorException() {
		AzureTranslatorClient client = newClient(KEY, null);
		server.expect(once(), requestTo(URL))
			.andRespond(withSuccess("[{\"translations\":[]}]", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.translate(List.of("a"))).isExactlyInstanceOf(TranslatorException.class)
			.hasMessageContaining("item 0");
	}

	@Test
	void missingKeyFailsWithoutCalling() {
		AzureTranslatorClient client = newClient("", null);

		assertThatThrownBy(() -> client.translate(List.of("a"))).isExactlyInstanceOf(TranslatorFatalException.class)
			.hasMessage("Azure Translator key is not set (catticket.azure.translator-key)");
		server.verify();
	}

}
