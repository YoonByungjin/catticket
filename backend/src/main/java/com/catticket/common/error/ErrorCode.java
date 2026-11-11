package com.catticket.common.error;

import org.springframework.http.HttpStatus;

/**
 * API のエラーコード（03 の 7.3）。画面は {@code code} で文言を決めるので、名前を変えない。
 */
public enum ErrorCode {

	VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed"),
	MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Request body is malformed"),
	REVIEW_NOT_OPEN(HttpStatus.BAD_REQUEST, "Reviews are not open before the performance starts"),
	TOKEN_INVALID(HttpStatus.BAD_REQUEST, "Token is invalid"),
	CURRENT_PASSWORD_INCORRECT(HttpStatus.BAD_REQUEST, "Current password is incorrect"),
	RECAPTCHA_FAILED(HttpStatus.BAD_REQUEST, "reCAPTCHA verification failed"),

	UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication is required"),
	LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "Email or password is incorrect"),
	ACCOUNT_LOCKED(HttpStatus.UNAUTHORIZED, "Account is locked"),
	EMAIL_NOT_VERIFIED(HttpStatus.UNAUTHORIZED, "Email is not verified"),

	FORBIDDEN(HttpStatus.FORBIDDEN, "Access is denied"),
	CSRF_INVALID(HttpStatus.FORBIDDEN, "CSRF token is missing or invalid"),

	NOT_FOUND(HttpStatus.NOT_FOUND, "API path not found"),
	PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Performance not found"),
	VENUE_NOT_FOUND(HttpStatus.NOT_FOUND, "Venue not found"),
	REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "Review not found"),

	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed"),

	EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "Email is already registered"),
	NICKNAME_ALREADY_USED(HttpStatus.CONFLICT, "Nickname is already used"),
	NICKNAME_TAKEN_AT_VERIFICATION(HttpStatus.CONFLICT, "Nickname was taken before verification"),
	REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "Review already exists"),

	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Request body must be JSON"),

	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),

	MAIL_LIMIT_REACHED(HttpStatus.SERVICE_UNAVAILABLE, "Mail daily limit reached"),
	MAIL_SEND_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "Failed to send mail"),
	RECAPTCHA_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "reCAPTCHA is unavailable");

	private final HttpStatus status;

	private final String message;

	ErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	public HttpStatus status() {
		return status;
	}

	public String message() {
		return message;
	}

}
