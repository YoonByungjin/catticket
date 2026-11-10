package com.catticket.common.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import com.catticket.auth.AuthSessionService;
import com.catticket.auth.row.SessionUserRow;

/**
 * セッションのあるリクエストのたびに利用者を DB で読み、無効なセッションを捨てる（03 の 7.4、06 の 3.4）。
 * サーブレットのフィルターとして自動で登録されないよう {@code @Component} にしない（SecurityConfig がチェーンに入れる）。
 */
public class SessionUserFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(SessionUserFilter.class);

	private final AuthSessionService authSessionService;

	private final SecurityContextRepository securityContextRepository;

	public SessionUserFilter(AuthSessionService authSessionService,
			SecurityContextRepository securityContextRepository) {
		this.authSessionService = authSessionService;
		this.securityContextRepository = securityContextRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		SessionPrincipal principal = loadPrincipal(request);
		if (principal != null) {
			Optional<SessionUserRow> row = authSessionService.findSessionUser(principal.userId());
			if (row.isPresent() && isValid(row.get(), principal)) {
				setRequestAuthentication(row.get());
			}
			else {
				invalidate(request, principal);
			}
		}
		chain.doFilter(request, response);
	}

	private SessionPrincipal loadPrincipal(HttpServletRequest request) {
		// セッションがなければ作らない（未ログインのリクエストでセッションを増やさない）
		if (request.getSession(false) == null) {
			return null;
		}
		SecurityContext context = securityContextRepository.loadDeferredContext(request).get();
		Authentication authentication = context == null ? null : context.getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof SessionPrincipal p) {
			return p;
		}
		return null;
	}

	private static boolean isValid(SessionUserRow row, SessionPrincipal principal) {
		return row.isActive() && row.passwordHash() != null && principal.passwordHash() != null
				// 時間の差からハッシュを推測されないよう、一定時間で比べる
				&& MessageDigest.isEqual(row.passwordHash().getBytes(StandardCharsets.UTF_8),
						principal.passwordHash().getBytes(StandardCharsets.UTF_8));
	}

	private static void setRequestAuthentication(SessionUserRow row) {
		String nickname = row.role() == Role.ADMIN ? null : row.nickname();
		SessionUser user = new SessionUser(row.id(), row.role(), nickname);
		var authentication = UsernamePasswordAuthenticationToken.authenticated(user, null,
				List.of(row.role().authority()));
		// このリクエストだけ。明示の保存なので、セッションには SessionPrincipal だけが残る
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
	}

	private static void invalidate(HttpServletRequest request, SessionPrincipal principal) {
		log.info("Session invalidated for user {}", principal.userId());
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		SecurityContextHolder.clearContext();
	}

}
