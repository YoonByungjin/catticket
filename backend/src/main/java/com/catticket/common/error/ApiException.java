package com.catticket.common.error;

/**
 * 業務の条件のエラー。Service が投げ、{@link GlobalExceptionHandler} が {@link ErrorCode} の応答にする。
 */
public class ApiException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final ErrorCode code;

	public ApiException(ErrorCode code) {
		super(code.name());
		this.code = code;
	}

	public ErrorCode code() {
		return code;
	}

}
