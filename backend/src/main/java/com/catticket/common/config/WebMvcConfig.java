package com.catticket.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API のリクエスト・レスポンスを JSON だけにする（03 の 1.1・7.1、06 の 3.1）。
 * KOPIS のために jackson-dataformat-xml があると、MVC が XML の本文を受け付け、XML で返してしまうため。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	@Override
	public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
		// XML の本文は 415 UNSUPPORTED_MEDIA_TYPE にする（RestClient のコンバーターには影響しない）
		builder.configureMessageConvertersList(list -> list.removeIf(c -> c instanceof JacksonXmlHttpMessageConverter));
	}

	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		// Accept にかかわらず JSON で返す（406 にしない。03 の 7.3 に 406 のコードはない）
		configurer.ignoreAcceptHeader(true).defaultContentType(MediaType.APPLICATION_JSON);
	}

}
