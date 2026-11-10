package com.catticket.common.error;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

/**
 * CSRF の失敗と役割の違いを、画面が見分けられる別のコードにする（03 の 7.3）。
 */
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

	private static final Logger log = LoggerFactory.getLogger(JsonAccessDeniedHandler.class);

	private final ErrorResponseWriter writer;

	public JsonAccessDeniedHandler(ErrorResponseWriter writer) {
		this.writer = writer;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		ErrorCode code = accessDeniedException instanceof CsrfException ? ErrorCode.CSRF_INVALID : ErrorCode.FORBIDDEN;
		log.info("Access denied ({}): {} {}", code, request.getMethod(), request.getRequestURI());
		writer.write(response, code);
	}

}
