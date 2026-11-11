package com.catticket.common.error;

import java.util.List;

/**
 * エラーの本文（03 の 7.3）。{@code errors} は画面が null を気にしなくてよいよう、いつも配列にする。
 */
public record ErrorResponse(String code, String message, List<FieldErrorItem> errors) {

	public ErrorResponse {
		errors = errors == null ? List.of() : List.copyOf(errors);
	}

	public static ErrorResponse of(ErrorCode code) {
		return new ErrorResponse(code.name(), code.message(), List.of());
	}

	public static ErrorResponse of(ErrorCode code, List<FieldErrorItem> errors) {
		return new ErrorResponse(code.name(), code.message(), errors);
	}

}
