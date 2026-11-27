package com.catticket.collect.client;

/**
 * 再試行してよい KOPIS の失敗（05 の 3.5）。結果コード 03・通信の失敗・タイムアウト・HTTP 5xx。
 * メッセージに API キーを入れない（原因の例外は URL を含まないものだけを付ける）。
 */
public class KopisRetryableException extends RuntimeException {

	public KopisRetryableException(String message) {
		super(message);
	}

	public KopisRetryableException(String message, Throwable cause) {
		super(message, cause);
	}

}
