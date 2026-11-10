package com.catticket.common.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.catticket.common.code.Genre;
import com.catticket.common.code.Lang;
import com.catticket.common.error.ApiException;
import com.catticket.common.error.ErrorCode;
import com.catticket.common.error.FieldErrorItem;
import com.catticket.common.error.RequestValidationException;
import com.catticket.common.security.LoginSessionManager;
import com.catticket.common.security.Role;
import com.catticket.common.security.SessionUser;
import com.catticket.common.time.DateTimes;
import com.catticket.common.validation.CodePointLength;
import com.catticket.common.validation.LangQuery;
import com.catticket.common.validation.OneOfEnum;
import com.catticket.common.validation.PageQuery;

/**
 * 共通の部品（エラーの応答・JSON だけ・認可・CSRF・セッション）を確かめるための、テストだけの Controller。
 * 本物の機能の Controller は Wave 2 で作るので、それまでの代わり（main には置かない）。
 */
@RestController
public class TestApiController {

	private final LoginSessionManager loginSessionManager;

	public TestApiController(LoginSessionManager loginSessionManager) {
		this.loginSessionManager = loginSessionManager;
	}

	public record TestBody(@NotNull(message = "REQUIRED") @CodePointLength(min = 1, max = 5) String name,
			Integer count, Boolean flag, BigDecimal ratio, List<@CodePointLength(max = 3) String> tags) {
	}

	public record TestFilter(List<@OneOfEnum(Genre.class) String> genre) {

		public TestFilter {
			// 繰り返しのクエリの同じ値は1つにする（03 の 7.1）
			genre = genre == null ? List.of() : List.copyOf(new LinkedHashSet<>(genre));
		}

	}

	public record QueryEcho(int page, int size, Lang lang, List<String> genre) {
	}

	public record TimeEcho(OffsetDateTime at, LocalDate date) {
	}

	@GetMapping("/api/test/time")
	public TimeEcho time() {
		return new TimeEcho(DateTimes.toApi(LocalDateTime.of(2026, 11, 5, 14, 3, 0)), LocalDate.of(2026, 11, 5));
	}

	@PostMapping("/api/test/body")
	public TestBody body(@Valid @RequestBody TestBody body) {
		return body;
	}

	@PutMapping("/api/test/body")
	public Map<String, Object> putBody(@Valid @RequestBody TestBody body) {
		return Map.of("ok", true);
	}

	@GetMapping("/api/test/query")
	public QueryEcho query(@Valid PageQuery page, @Valid LangQuery lang, @Valid TestFilter filter) {
		return new QueryEcho(page.page(), page.size(), lang.toLang(), filter.genre());
	}

	/** 引数に直接の制約があるとメソッドの検証になり、複数の record の誤りがまとめて返る */
	@GetMapping("/api/test/query-all")
	public QueryEcho queryAll(@Valid PageQuery page, @Valid LangQuery lang, @Valid TestFilter filter,
			@RequestParam(required = false) @Min(value = 1, message = "RANGE") Integer limit) {
		return new QueryEcho(page.page(), page.size(), lang.toLang(), filter.genre());
	}

	@GetMapping("/api/test/param")
	public Map<String, Object> param(
			@RequestParam @Min(value = 1, message = "RANGE") @Max(value = 10, message = "RANGE") int n) {
		return Map.of("n", n);
	}

	@GetMapping("/api/test/path/{id}")
	public Map<String, Object> path(@PathVariable long id) {
		return Map.of("id", id);
	}

	@GetMapping("/api/test/api-error")
	public void apiError() {
		throw new ApiException(ErrorCode.PERFORMANCE_NOT_FOUND);
	}

	@GetMapping("/api/test/request-validation")
	public void requestValidation() {
		throw new RequestValidationException(
				List.of(new FieldErrorItem("bookingSites[0].url", "FORMAT"), new FieldErrorItem("title", "LENGTH")));
	}

	@GetMapping("/api/test/boom")
	public void boom() {
		throw new IllegalStateException("secret SQL detail");
	}

	/** API-30 と同じく CSRF のトークンを読み込み、Cookie XSRF-TOKEN を付けさせる */
	@GetMapping("/api/test/csrf")
	public Map<String, Object> csrf(CsrfToken csrfToken) {
		return Map.of("headerName", csrfToken.getHeaderName(), "token", csrfToken.getToken());
	}

	/** API-31 の代わり: 認証の判定は省き、LoginSessionManager.establish だけを呼ぶ */
	@PostMapping("/api/test/login")
	public SessionUser login(@RequestParam long userId, @RequestParam Role role, @RequestParam String passwordHash,
			HttpServletRequest request, HttpServletResponse response) {
		SessionUser user = new SessionUser(userId, role, role == Role.ADMIN ? null : "Cat");
		loginSessionManager.establish(user, passwordHash, request, response);
		return user;
	}

	/** API-41・42 の後のセッションの終了の代わり */
	@PostMapping("/api/me/test/terminate")
	public ResponseEntity<Void> terminate(HttpServletRequest request, HttpServletResponse response) {
		loginSessionManager.terminate(request, response);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/api/test/whoami")
	public Map<String, Object> whoami(@AuthenticationPrincipal SessionUser user) {
		return user == null ? Map.of("authenticated", false) : Map.of("authenticated", true, "user", user);
	}

	@GetMapping("/api/me/test")
	public SessionUser me(@AuthenticationPrincipal SessionUser user) {
		return user;
	}

	@GetMapping("/api/admin/test")
	public SessionUser admin(@AuthenticationPrincipal SessionUser user) {
		return user;
	}

	@PostMapping("/api/performances/{id}/reviews")
	public Map<String, Object> postReview(@PathVariable String id) {
		return Map.of("ok", true);
	}

	@GetMapping("/api/performances/{id}/reviews")
	public Map<String, Object> listReviews(@PathVariable String id) {
		return Map.of("ok", true);
	}

	@PutMapping("/api/reviews/{id}")
	public Map<String, Object> putReview(@PathVariable String id) {
		return Map.of("ok", true);
	}

}
