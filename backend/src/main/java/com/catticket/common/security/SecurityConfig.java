package com.catticket.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import com.catticket.auth.AuthSessionService;
import com.catticket.common.error.ErrorResponseWriter;
import com.catticket.common.error.JsonAccessDeniedHandler;
import com.catticket.common.error.JsonAuthenticationEntryPoint;

/**
 * 認証・認可・CSRF・ログアウト（ADR 0013、03 の 7.4、06 の 3.4）。
 * {@code @WebMvcTest} で {@code @Import(SecurityConfig.class)} だけで動くよう、使う部品もここで読み込む。
 */
@Configuration
@Import({ ErrorResponseWriter.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
		LoginSessionManager.class })
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, AuthSessionService authSessionService,
			HttpSessionSecurityContextRepository securityContextRepository,
			CookieCsrfTokenRepository csrfTokenRepository, JsonAuthenticationEntryPoint authenticationEntryPoint,
			JsonAccessDeniedHandler accessDeniedHandler) {
		http.csrf(c -> c.spa().csrfTokenRepository(csrfTokenRepository))
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.securityContext(c -> c.securityContextRepository(securityContextRepository))
			.logout(l -> l
				.logoutRequestMatcher(
						PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/auth/logout"))
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
				.deleteCookies(LoginSessionManager.SESSION_COOKIE))
			.exceptionHandling(e -> e.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler))
			.authorizeHttpRequests(a -> a.requestMatchers("/api/admin/**")
				.hasRole(Role.ADMIN.name())
				.requestMatchers("/api/me/**")
				.hasRole(Role.USER.name())
				.requestMatchers(HttpMethod.POST, "/api/performances/*/reviews")
				.hasRole(Role.USER.name())
				.requestMatchers(HttpMethod.PUT, "/api/reviews/*")
				.hasRole(Role.USER.name())
				.requestMatchers(HttpMethod.DELETE, "/api/reviews/*")
				.hasRole(Role.USER.name())
				.anyRequest()
				.permitAll())
			.addFilterBefore(new SessionUserFilter(authSessionService, securityContextRepository),
					AuthorizationFilter.class);
		return http.build();
	}

	/** LoginSessionManager と同じものを使い、ログインで保存した場所をフィルターが読む */
	@Bean
	HttpSessionSecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	/** 画面（Angular）が Cookie を読んでヘッダーに入れるので HttpOnly にしない（03 の 1.3） */
	@Bean
	CookieCsrfTokenRepository csrfTokenRepository() {
		return CookieCsrfTokenRepository.withHttpOnlyFalse();
	}

	/** bcrypt（ADR 0013）。DelegatingPasswordEncoder なので、ハッシュに {bcrypt} の印が付く */
	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

}
