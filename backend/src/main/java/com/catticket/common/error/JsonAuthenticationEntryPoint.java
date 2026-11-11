package com.catticket.common.error;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * 未ログインを 401 {@code UNAUTHENTICATED} にする（Spring Security の既定の本文を使わない。03 の 7.3）。
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private static final Logger log = LoggerFactory.getLogger(JsonAuthenticationEntryPoint.class);

	private final ErrorResponseWriter writer;

	public JsonAuthenticationEntryPoint(ErrorResponseWriter writer) {
		this.writer = writer;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		log.info("Unauthenticated: {} {}", request.getMethod(), request.getRequestURI());
		writer.write(response, ErrorCode.UNAUTHENTICATED);
	}

}
