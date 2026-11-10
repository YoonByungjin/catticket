package com.catticket.common.error;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;

import tools.jackson.databind.json.JsonMapper;

/**
 * Spring Security のハンドラーは MVC の外で応答を書くので、同じ形の JSON をここで書く。
 */
public class ErrorResponseWriter {

	private final JsonMapper jsonMapper;

	public ErrorResponseWriter(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	public void write(HttpServletResponse response, ErrorCode code) throws IOException {
		response.setStatus(code.status().value());
		response.setCharacterEncoding("UTF-8");
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		jsonMapper.writeValue(response.getOutputStream(), ErrorResponse.of(code));
	}

}
