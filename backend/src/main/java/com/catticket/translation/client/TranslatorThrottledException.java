package com.catticket.translation.client;

/**
 * 要求が多すぎる（HTTP 429）。S7 は 60 秒待って最大 3 回やり直す（05 の 3.7）。
 */
public class TranslatorThrottledException extends TranslatorException {

	public TranslatorThrottledException(String message) {
		super(message);
	}

}
