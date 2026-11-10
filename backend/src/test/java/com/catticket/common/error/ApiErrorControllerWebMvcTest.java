package com.catticket.common.error;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.RequestDispatcher;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.catticket.auth.AuthSessionService;
import com.catticket.common.security.SecurityConfig;

/**
 * /error（フィルターなど MVC の外で起きたエラー）も 03 の 7.3 の形で返すこと（06 の 3.2 の ApiErrorController）。
 */
@WebMvcTest(controllers = ApiErrorController.class)
@Import(SecurityConfig.class)
class ApiErrorControllerWebMvcTest {

	@Autowired
	MockMvc mvc;

	@MockitoBean
	AuthSessionService authSessionService;

	@ParameterizedTest
	@CsvSource({ "400, MALFORMED_REQUEST", "401, UNAUTHENTICATED", "403, FORBIDDEN", "404, NOT_FOUND",
			"405, METHOD_NOT_ALLOWED", "415, UNSUPPORTED_MEDIA_TYPE", "500, INTERNAL_ERROR",
			"502, INTERNAL_ERROR" })
	void statusIsMappedToErrorCode(int status, String code) throws Exception {
		int expected = status == 502 ? 500 : status;
		mvc.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, status))
			.andExpect(status().is(expected))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.code").value(code))
			.andExpect(jsonPath("$.errors", hasSize(0)));
	}

	@Test
	void exceptionDetailsAreNotReturned() throws Exception {
		mvc.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
			.requestAttr(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("secret")))
			.andExpect(status().isInternalServerError())
			.andExpect(content().string(not(containsString("secret"))));
	}

}
