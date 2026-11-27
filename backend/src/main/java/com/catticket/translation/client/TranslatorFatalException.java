package com.catticket.translation.client;

/**
 * 何度送っても同じ結果になる失敗（06 の 4.10 の 7.。U-15 ③）。キーが空、HTTP 4xx（408・429・403001 以外）。
 * S7 は再試行せずに止める（待っても直らないため）。
 */
public class TranslatorFatalException extends TranslatorException {

	public TranslatorFatalException(String message) {
		super(message);
	}

}
