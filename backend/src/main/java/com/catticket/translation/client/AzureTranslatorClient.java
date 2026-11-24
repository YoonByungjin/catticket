package com.catticket.translation.client;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.catticket.common.config.CatticketProperties;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

/**
 * Azure AI Translator v3（ADR 0007、06 の 4.10）。韓国語 → 日本語。応答は入力と同じ順に返す。
 * 束ね方・速さ・月の上限・再試行は呼ぶ側（TranslationService）が持つ。ここはエラーの種類を分けるだけ。
 */
@Component
public class AzureTranslatorClient {

	private static final Logger log = LoggerFactory.getLogger(AzureTranslatorClient.class);

	/** 無料の割り当てを超えた（Translator v3 のエラーのコード） */
	static final long QUOTA_EXCEEDED = 403001;

	private final RestClient restClient;

	private final String key;

	private final String region;

	// アプリの JSON の設定（03 の型の厳しさ）に左右されないよう、自分の JsonMapper で読み書きする
	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	public AzureTranslatorClient(@Qualifier("azureRestClient") RestClient restClient, CatticketProperties props) {
		this.restClient = restClient;
		this.key = props.azure().translatorKey();
		this.region = props.azure().translatorRegion();
	}

	/**
	 * @return 入力と同じ順の訳文
	 * @throws TranslatorQuotaExceededException エラーの {@code error.code} が 403001
	 * @throws TranslatorThrottledException     HTTP 429
	 * @throws TranslatorException              そのほか（通信の失敗・タイムアウト・5xx・読めない応答・件数の違い）
	 */
	public List<String> translate(List<String> texts) {
		if (texts.isEmpty()) {
			return List.of();
		}
		if (key == null || key.isBlank()) {
			throw new TranslatorException("Azure Translator key is not set (catticket.azure.translator-key)");
		}
		byte[] requestBody = requestBody(texts);
		Result result;
		try {
			result = restClient.post()
				.uri(b -> b.path("/translate")
					.queryParam("api-version", "3.0")
					.queryParam("from", "ko")
					.queryParam("to", "ja")
					.build())
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.headers(h -> {
					h.set("Ocp-Apim-Subscription-Key", key);
					// グローバルのリソースでは地域を送らない
					if (region != null && !region.isBlank()) {
						h.set("Ocp-Apim-Subscription-Region", region);
					}
				})
				.body(requestBody)
				.exchange((request, response) -> new Result(response.getStatusCode(),
						response.getBody().readAllBytes()));
		}
		catch (RestClientException e) {
			Throwable cause = e.getMostSpecificCause();
			String reason = cause == e ? e.getClass().getSimpleName()
					: cause.getClass().getSimpleName() + ": " + cause.getMessage();
			log.warn("Azure Translator request failed: {}", reason);
			throw new TranslatorException("Azure Translator I/O error: " + reason, cause == e ? null : cause);
		}
		if (!result.status().is2xxSuccessful()) {
			throw error(result);
		}
		return translations(result.body(), texts.size());
	}

	private byte[] requestBody(List<String> texts) {
		ArrayNode array = jsonMapper.createArrayNode();
		for (String text : texts) {
			array.addObject().put("Text", text);
		}
		return jsonMapper.writeValueAsBytes(array);
	}

	private List<String> translations(byte[] body, int expected) {
		JsonNode root;
		try {
			root = jsonMapper.readTree(body);
		}
		catch (JacksonException e) {
			throw new TranslatorException("Azure Translator unreadable response: " + e.getOriginalMessage());
		}
		if (root == null || !root.isArray() || root.size() != expected) {
			throw new TranslatorException("Azure Translator returned " + (root != null && root.isArray() ? root.size() : "no")
					+ " items for " + expected + " texts");
		}
		List<String> result = new ArrayList<>(expected);
		for (int i = 0; i < expected; i++) {
			JsonNode text = root.get(i).path("translations").path(0).path("text");
			if (!text.isString()) {
				throw new TranslatorException("Azure Translator response item " + i + " has no translations[0].text");
			}
			result.add(text.asString());
		}
		return result;
	}

	private TranslatorException error(Result result) {
		Long code = null;
		String message = null;
		try {
			JsonNode error = jsonMapper.readTree(result.body()).path("error");
			if (error.path("code").isNumber()) {
				code = error.path("code").asLong();
			}
			if (error.path("message").isString()) {
				message = error.path("message").asString();
			}
		}
		catch (JacksonException e) {
			// エラーの本文が JSON でなくても、HTTP の状態で分ける
		}
		String text = "Azure Translator HTTP " + result.status().value() + " code=" + code + " message=" + message;
		log.warn(text);
		if (code != null && code == QUOTA_EXCEEDED) {
			return new TranslatorQuotaExceededException(text);
		}
		if (result.status().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
			return new TranslatorThrottledException(text);
		}
		return new TranslatorException(text);
	}

	private record Result(HttpStatusCode status, byte[] body) {
	}

}
