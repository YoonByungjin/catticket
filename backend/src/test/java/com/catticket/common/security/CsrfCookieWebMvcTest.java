package com.catticket.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.catticket.auth.AuthSessionService;
import com.catticket.common.config.JacksonConfig;
import com.catticket.common.support.TestApiController;

/**
 * 本物の Cookie XSRF-TOKEN とヘッダー X-XSRF-TOKEN で CSRF を確かめる（03 の 7.4）。U-06 もここで確かめる。
 * spring-security-test の with(csrf()) は、使うとキャッシュされたフィルターの CsrfTokenRepository を
 * テスト用のものに包み、その後のトークンの読み込みで Cookie が付かなくなる。そのため with(csrf()) を使わず、
 * ほかのテストとコンテキストを分ける（TestPropertySource の値はコンテキストを分けるためだけのもの）。
 */
@WebMvcTest(controllers = TestApiController.class)
@Import({ SecurityConfig.class, JacksonConfig.class })
@TestPropertySource(properties = "catticket.test.context=real-csrf-cookie")
class CsrfCookieWebMvcTest {

	@Autowired
	MockMvc mvc;

	@MockitoBean
	AuthSessionService authSessionService;

	private static List<String> setCookies(MockHttpServletResponse response, String name) {
		return response.getHeaders(HttpHeaders.SET_COOKIE).stream().filter(h -> h.startsWith(name + "=")).toList();
	}

	/** API-30 と同じく CsrfToken を読み込むと Cookie XSRF-TOKEN が付く */
	private String fetchToken() throws Exception {
		MockHttpServletResponse response = mvc.perform(get("/api/test/csrf"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
			.andReturn()
			.getResponse();
		Cookie cookie = response.getCookie("XSRF-TOKEN");
		assertThat(cookie).isNotNull();
		// 画面（Angular）が読むので HttpOnly にしない
		assertThat(cookie.isHttpOnly()).isFalse();
		return cookie.getValue();
	}

	@Test
	void cookieTokenAndHeaderPass() throws Exception {
		String token = fetchToken();

		mvc.perform(post("/api/test/body").cookie(new Cookie("XSRF-TOKEN", token))
			.header("X-XSRF-TOKEN", token)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"cat\"}")).andExpect(status().isOk());
	}

	@Test
	void wrongOrMissingHeaderIsCsrfInvalid() throws Exception {
		String token = fetchToken();

		mvc.perform(post("/api/test/body").cookie(new Cookie("XSRF-TOKEN", token))
			.header("X-XSRF-TOKEN", "wrong")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"cat\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		mvc.perform(post("/api/test/body").cookie(new Cookie("XSRF-TOKEN", token))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"cat\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("CSRF_INVALID"));
	}

	@Test
	void establishClearsCsrfCookieAndCreatesSession() throws Exception {
		// U-06: csrf.spa() と共有の CookieCsrfTokenRepository で、ログインの後に XSRF-TOKEN が消える（Max-Age=0）
		String token = fetchToken();

		MvcResult result = mvc.perform(post("/api/test/login").cookie(new Cookie("XSRF-TOKEN", token))
			.header("X-XSRF-TOKEN", token)
			.param("userId", "1")
			.param("role", "USER")
			.param("passwordHash", "{bcrypt}hash")).andExpect(status().isOk()).andReturn();

		List<String> xsrf = setCookies(result.getResponse(), "XSRF-TOKEN");
		// 消した後に新しいトークンで付け直されていない
		assertThat(xsrf).isNotEmpty()
			.allSatisfy(h -> assertThat(h).startsWith("XSRF-TOKEN=;").contains("Max-Age=0"));
		assertThat(result.getRequest().getSession(false)).isNotNull();
	}

	@Test
	void logoutWithoutLoginIs204AndClearsCookies() throws Exception {
		// API-32: ログインしていなくても 204。JSESSIONID と XSRF-TOKEN を消す
		String token = fetchToken();

		MockHttpServletResponse response = mvc
			.perform(post("/api/auth/logout").cookie(new Cookie("XSRF-TOKEN", token)).header("X-XSRF-TOKEN", token))
			.andExpect(status().isNoContent())
			.andReturn()
			.getResponse();
		assertThat(setCookies(response, "JSESSIONID")).isNotEmpty()
			.allSatisfy(h -> assertThat(h).contains("Max-Age=0"));
		assertThat(setCookies(response, "XSRF-TOKEN")).isNotEmpty()
			.allSatisfy(h -> assertThat(h).contains("Max-Age=0"));
	}

}
