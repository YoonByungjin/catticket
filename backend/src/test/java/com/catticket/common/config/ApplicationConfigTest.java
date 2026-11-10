package com.catticket.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

/**
 * 本番と同じ構成で起動し、06 の 3.1・8.1 の設定が読み込まれていること（API キーが空でも起動できる。06 の 7.2）。
 */
@SpringBootTest
class ApplicationConfigTest {

	@Autowired
	CatticketProperties props;

	@Autowired
	Environment env;

	@Autowired
	Clock clock;

	@Autowired
	@Qualifier("kopisRestClient")
	RestClient kopisRestClient;

	@Autowired
	@Qualifier("azureRestClient")
	RestClient azureRestClient;

	@Autowired
	@Qualifier("recaptchaRestClient")
	RestClient recaptchaRestClient;

	@Test
	void propertiesAreBound() {
		// テストの設定（src/test/resources/config/application.yml）で自動のバッチを止めている
		assertThat(props.scheduler().enabled()).isFalse();
		assertThat(props.task()).isNull();
		assertThat(props.kopis().baseUrl()).isEqualTo("http://www.kopis.or.kr/openApi/restful");
		assertThat(props.kopis().interval()).isEqualTo(Duration.ofMillis(500));
		assertThat(props.azure().requestMaxChars()).isEqualTo(10000);
		assertThat(props.azure().charsPerMinute()).isEqualTo(30000);
		assertThat(props.azure().monthlyLimitChars()).isEqualTo(1_800_000);
		assertThat(props.mail().dailyLimit()).isEqualTo(400);
		assertThat(props.publicBaseUrl()).isNotBlank();
		assertThat(props.recaptcha()).isNotNull();
		assertThat(props.admin()).isNotNull();
	}

	@Test
	void secretsAreNotPrintedByToString() {
		assertThat(props.kopis().toString()).doesNotContain("apiKey");
		assertThat(props.admin().toString()).doesNotContain("password");
	}

	@Test
	void sessionAndClockSettings() {
		// 自動ログアウト 30 分（ADR 0013）と Cookie の属性（06 の 3.4）
		assertThat(env.getProperty("server.servlet.session.timeout", Duration.class)).isEqualTo(Duration.ofMinutes(30));
		assertThat(env.getProperty("server.servlet.session.cookie.http-only", Boolean.class)).isTrue();
		assertThat(env.getProperty("server.servlet.session.cookie.same-site")).isEqualTo("lax");
		assertThat(env.getProperty("server.forward-headers-strategy")).isEqualTo("native");
		assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
	}

	@Test
	void restClientsExist() {
		assertThat(kopisRestClient).isNotNull();
		assertThat(azureRestClient).isNotNull();
		assertThat(recaptchaRestClient).isNotNull();
	}

}
