package com.catticket.translation.client;

/**
 * Azure Translator の失敗（06 の 4.10）。403001・429 以外（通信の失敗・タイムアウト・5xx・読めない応答など）。
 * S7 は 2・4・8 秒で最大 3 回やり直す（05 の 3.7）。メッセージにキーを入れない。
 */
public class TranslatorException extends RuntimeException {

	public TranslatorException(String message) {
		super(message);
	}

	public TranslatorException(String message, Throwable cause) {
		super(message, cause);
	}

}
