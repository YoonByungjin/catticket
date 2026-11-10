package com.catticket.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.catticket.auth.AuthSessionService;
import com.catticket.auth.UserStatus;
import com.catticket.auth.row.SessionUserRow;
import com.catticket.common.config.JacksonConfig;
import com.catticket.common.support.TestApiController;

/**
 * 認可の規則・CSRF・ログアウト・セッションの確認（06 の 3.4、03 の 7.4）と U-06 を確かめる。
 */
@WebMvcTest(controllers = TestApiController.class)
@Import({ SecurityConfig.class, JacksonConfig.class })
class SecurityWebMvcTest {

	private static final String HASH = "{bcrypt}$2a$10$abcdefghijklmnopqrstuv";

	@Autowired
	MockMvc mvc;

	@Autowired
	PasswordEncoder passwordEncoder;

	@MockitoBean
	AuthSessionService authSessionService;

	private static List<String> setCookies(MockHttpServletResponse response, String name) {
		return response.getHeaders(HttpHeaders.SET_COOKIE).stream().filter(h -> h.startsWith(name + "=")).toList();
	}

	/** establish でセッションを作り、そのセッションを返す */
	private MockHttpSession login(long userId, Role role, String hash) throws Exception {
		MvcResult result = mvc.perform(post("/api/test/login").with(csrf())
			.param("userId", String.valueOf(userId))
			.param("role", role.name())
			.param("passwordHash", hash)).andExpect(status().isOk()).andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	@Nested
	class Authorization {

		@Test
		void anonymousOnUserApiIsUnauthenticated() throws Exception {
			mvc.perform(get("/api/me/test"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
				.andExpect(jsonPath("$.errors").isArray());
		}

		@Test
		void anonymousOnAdminApiIsUnauthenticated() throws Exception {
			mvc.perform(get("/api/admin/test"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
		}

		@Test
		void userOnAdminApiIsForbidden() throws Exception {
			mvc.perform(get("/api/admin/test").with(TestAuth.user(1, "Cat")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		}

		@Test
		void adminOnUserApiIsForbidden() throws Exception {
			// 管理者は「一般」の API を使えない（03 の 1.3）
			mvc.perform(get("/api/me/test").with(TestAuth.admin(9)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
			mvc.perform(post("/api/performances/PF1/reviews").with(TestAuth.admin(9)).with(csrf()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		}

		@Test
		void userAndAdminReachTheirApis() throws Exception {
			mvc.perform(get("/api/me/test").with(TestAuth.user(1, "Cat")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(1))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.nickname").value("Cat"));
			mvc.perform(get("/api/admin/test").with(TestAuth.admin(9)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("ADMIN"));
		}

		@Test
		void reviewWriteNeedsUserButReadIsOpen() throws Exception {
			mvc.perform(post("/api/performances/PF1/reviews").with(csrf()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
			mvc.perform(put("/api/reviews/1").with(csrf()))
				.andExpect(status().isUnauthorized());
			mvc.perform(post("/api/performances/PF1/reviews").with(csrf()).with(TestAuth.user(1, "Cat")))
				.andExpect(status().isOk());
			mvc.perform(get("/api/performances/PF1/reviews")).andExpect(status().isOk());
		}

		@Test
		void publicApiGivesNullPrincipalToAnonymous() throws Exception {
			mvc.perform(get("/api/test/whoami"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authenticated").value(false));
		}

	}

	@Nested
	class Csrf {

		@Test
		void postWithoutTokenIsCsrfInvalid() throws Exception {
			mvc.perform(post("/api/test/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"cat\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));
		}

		@Test
		void csrfIsCheckedBeforeLogin() throws Exception {
			// 判定の順番 ① CSRF → ② ログイン（03 の 7.4）
			mvc.perform(post("/api/performances/PF1/reviews"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));
		}

	}

	@Nested
	class LoginSession {

		@Test
		void establishChangesExistingSessionId() throws Exception {
			MockHttpSession before = new MockHttpSession();
			String oldId = before.getId();
			MvcResult result = mvc.perform(post("/api/test/login").with(csrf())
				.session(before)
				.param("userId", "1")
				.param("role", "USER")
				.param("passwordHash", HASH)).andExpect(status().isOk()).andReturn();
			assertThat(result.getRequest().getSession(false).getId()).isNotEqualTo(oldId);
		}

		@Test
		void validSessionReadsCurrentUserFromDb() throws Exception {
			MockHttpSession session = login(1, Role.USER, HASH);
			// ニックネームはセッションに持たず、毎回 DB の値を使う（03 の 7.4）
			given(authSessionService.findSessionUser(1))
				.willReturn(Optional.of(new SessionUserRow(1, Role.USER, UserStatus.ACTIVE, "NewName", HASH)));

			mvc.perform(get("/api/me/test").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(1))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.nickname").value("NewName"));
			assertThat(session.isInvalid()).isFalse();
		}

		@Test
		void adminNicknameIsNull() throws Exception {
			MockHttpSession session = login(9, Role.ADMIN, HASH);
			given(authSessionService.findSessionUser(9))
				.willReturn(Optional.of(new SessionUserRow(9, Role.ADMIN, UserStatus.ACTIVE, "catticket-admin", HASH)));

			mvc.perform(get("/api/admin/test").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("ADMIN"))
				.andExpect(jsonPath("$.nickname").doesNotExist());
		}

		@Test
		void deletedUserSessionIsInvalidated() throws Exception {
			// 1. 行がない（退会）
			MockHttpSession session = login(1, Role.USER, HASH);
			given(authSessionService.findSessionUser(1)).willReturn(Optional.empty());

			mvc.perform(get("/api/me/test").session(session))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
			assertThat(session.isInvalid()).isTrue();
		}

		@Test
		void inactiveUserSessionIsInvalidated() throws Exception {
			// 2. 状態が有効でない
			MockHttpSession session = login(1, Role.USER, HASH);
			given(authSessionService.findSessionUser(1))
				.willReturn(Optional.of(new SessionUserRow(1, Role.USER, UserStatus.PENDING, "Cat", HASH)));

			mvc.perform(get("/api/me/test").session(session)).andExpect(status().isUnauthorized());
			assertThat(session.isInvalid()).isTrue();
		}

		@Test
		void changedPasswordSessionIsInvalidated() throws Exception {
			// 3. ほかの端末でパスワードを変えた
			MockHttpSession session = login(1, Role.USER, HASH);
			given(authSessionService.findSessionUser(1))
				.willReturn(Optional.of(new SessionUserRow(1, Role.USER, UserStatus.ACTIVE, "Cat", HASH + "x")));

			mvc.perform(get("/api/me/test").session(session)).andExpect(status().isUnauthorized());
			assertThat(session.isInvalid()).isTrue();
		}

		@Test
		void invalidatedSessionOnPublicApiIsAnonymous() throws Exception {
			// 「全員」の API は未ログインの内容で続ける
			MockHttpSession session = login(1, Role.USER, HASH);
			given(authSessionService.findSessionUser(1)).willReturn(Optional.empty());

			mvc.perform(get("/api/test/whoami").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authenticated").value(false));
			assertThat(session.isInvalid()).isTrue();
		}

		@Test
		void requestWithoutSessionDoesNotReadDb() throws Exception {
			mvc.perform(get("/api/test/whoami")).andExpect(status().isOk());
			verify(authSessionService, never()).findSessionUser(anyLong());
		}

		@Test
		void terminateEndsSessionAndClearsCookies() throws Exception {
			MockHttpSession session = login(1, Role.USER, HASH);
			given(authSessionService.findSessionUser(1))
				.willReturn(Optional.of(new SessionUserRow(1, Role.USER, UserStatus.ACTIVE, "Cat", HASH)));

			MockHttpServletResponse response = mvc
				.perform(post("/api/me/test/terminate").session(session).with(csrf()))
				.andExpect(status().isNoContent())
				.andReturn()
				.getResponse();
			assertThat(session.isInvalid()).isTrue();
			assertThat(setCookies(response, "JSESSIONID")).isNotEmpty().allSatisfy(h -> assertThat(h).contains("Max-Age=0"));
			assertThat(setCookies(response, "XSRF-TOKEN")).isNotEmpty().allSatisfy(h -> assertThat(h).contains("Max-Age=0"));
		}

	}

	@Nested
	class Logout {

		@Test
		void logoutWithoutLoginIs204AndClearsSessionCookie() throws Exception {
			// API-32: ログインしていなくても 204
			MockHttpServletResponse response = mvc.perform(post("/api/auth/logout").with(csrf()))
				.andExpect(status().isNoContent())
				.andReturn()
				.getResponse();
			assertThat(setCookies(response, "JSESSIONID")).isNotEmpty().allSatisfy(h -> assertThat(h).contains("Max-Age=0"));
		}

		@Test
		void logoutInvalidatesSession() throws Exception {
			MockHttpSession session = login(1, Role.USER, HASH);
			given(authSessionService.findSessionUser(1))
				.willReturn(Optional.of(new SessionUserRow(1, Role.USER, UserStatus.ACTIVE, "Cat", HASH)));

			MockHttpServletResponse response = mvc.perform(post("/api/auth/logout").session(session).with(csrf()))
				.andExpect(status().isNoContent())
				.andReturn()
				.getResponse();
			assertThat(session.isInvalid()).isTrue();
			assertThat(setCookies(response, "XSRF-TOKEN")).isNotEmpty().allSatisfy(h -> assertThat(h).contains("Max-Age=0"));
		}

		@Test
		void logoutNeedsCsrf() throws Exception {
			mvc.perform(post("/api/auth/logout"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));
		}

	}

	@Test
	void passwordEncoderIsBcrypt() {
		assertThat(passwordEncoder.encode("Password1")).startsWith("{bcrypt}");
	}

}
