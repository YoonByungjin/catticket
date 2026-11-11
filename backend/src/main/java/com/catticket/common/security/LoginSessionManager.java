package com.catticket.common.security;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfLogoutHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;

/**
 * ログインのセッションを作る・終える（06 の 3.4）。formLogin を使わないので、Spring Security の既定の動きをここで行う。
 */
public class LoginSessionManager {

	static final String SESSION_COOKIE = "JSESSIONID";

	private final SecurityContextRepository securityContextRepository;

	private final CsrfTokenRepository csrfTokenRepository;

	public LoginSessionManager(SecurityContextRepository securityContextRepository,
			CsrfTokenRepository csrfTokenRepository) {
		this.securityContextRepository = securityContextRepository;
		this.csrfTokenRepository = csrfTokenRepository;
	}

	public void establish(SessionUser user, String passwordHash, HttpServletRequest request,
			HttpServletResponse response) {
		// セッション固定攻撃の対策: 既にあれば ID を変える
		if (request.getSession(false) != null) {
			request.changeSessionId();
		}
		else {
			request.getSession(true);
		}
		var authentication = UsernamePasswordAuthenticationToken.authenticated(
				new SessionPrincipal(user.userId(), passwordHash), null, List.of(user.role().authority()));
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		securityContextRepository.saveContext(context, request, response);
		// ログインの成功で CSRF の Cookie を消す（Spring Security の既定と同じ。03 の 7.4）
		csrfTokenRepository.saveToken(null, request, response);
	}

	public void terminate(HttpServletRequest request, HttpServletResponse response) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		new SecurityContextLogoutHandler().logout(request, response, authentication);
		new CookieClearingLogoutHandler(SESSION_COOKIE).logout(request, response, authentication);
		new CsrfLogoutHandler(csrfTokenRepository).logout(request, response, authentication);
	}

}
