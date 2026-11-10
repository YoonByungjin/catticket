package com.catticket.common.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 外部 API ごとの RestClient（06 の 7.1）。タイムアウトは呼び出し先ごとに変える。
 */
@Configuration
public class HttpClientConfig {

	/** reCAPTCHA Enterprise の API（03 の 7.5） */
	static final String RECAPTCHA_BASE_URL = "https://recaptchaenterprise.googleapis.com";

	@Bean
	@Qualifier("kopisRestClient")
	RestClient kopisRestClient(RestClient.Builder builder, CatticketProperties props) {
		// 05 の 3.5: 接続 5 秒・読み込み 30 秒
		return build(builder, props.kopis().baseUrl(), Duration.ofSeconds(5), Duration.ofSeconds(30));
	}

	@Bean
	@Qualifier("azureRestClient")
	RestClient azureRestClient(RestClient.Builder builder, CatticketProperties props) {
		// 公式の 1 回の最大の待ち 15 秒より長くし、混んでいるときの遅れを許す
		return build(builder, props.azure().translatorEndpoint(), Duration.ofSeconds(5), Duration.ofSeconds(30));
	}

	@Bean
	@Qualifier("recaptchaRestClient")
	RestClient recaptchaRestClient(RestClient.Builder builder) {
		// 利用者が待つ API の中で呼ぶので短くする（5 秒で返らなければ 503）
		return build(builder, RECAPTCHA_BASE_URL, Duration.ofSeconds(3), Duration.ofSeconds(5));
	}

	private static RestClient build(RestClient.Builder builder, String baseUrl, Duration connectTimeout,
			Duration readTimeout) {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
		factory.setReadTimeout(readTimeout);
		// Builder は Spring Boot の共有の設定を持つので、ほかのクライアントに影響しないよう複製して使う
		return builder.clone().baseUrl(baseUrl).requestFactory(factory).build();
	}

}
