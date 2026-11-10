package com.catticket.common.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * {@code @WebMvcTest} でログインした状態にする（06 の 7.2）。spring-security-test の with(user(...)) の代わりに、
 * 本番と同じ {@link SessionUser} を principal にした Authentication を入れる（@AuthenticationPrincipal で受けられる）。
 * SessionUserFilter はセッションに SessionPrincipal がないので何もせず、この Authentication がそのまま使われる。
 */
public final class TestAuth {

	private TestAuth() {
	}

	public static RequestPostProcessor user(long userId, String nickname) {
		return as(new SessionUser(userId, Role.USER, nickname));
	}

	public static RequestPostProcessor admin(long userId) {
		return as(new SessionUser(userId, Role.ADMIN, null));
	}

	public static RequestPostProcessor as(SessionUser user) {
		return authentication(
				UsernamePasswordAuthenticationToken.authenticated(user, null, List.of(user.role().authority())));
	}

}
