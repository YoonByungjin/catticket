package com.catticket.collect.client;

/**
 * 同じステップで、再試行の後も失敗した KOPIS の呼び出しが続けて上限に達した（05 の 3.5）。
 * 制限に当たっているときに呼び続けないため、ステップを止めて失敗にする。
 */
public class StepAbortedException extends RuntimeException {

	public StepAbortedException(String message, Throwable cause) {
		super(message, cause);
	}

	public StepAbortedException(String message) {
		super(message);
	}

}
