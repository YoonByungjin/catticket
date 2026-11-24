package com.catticket.translation.client;

/**
 * 無料の割り当てを超えた（エラーの本文の {@code error.code} が 403001）。S7 はその回をすぐ止める（05 の 3.7）。
 */
public class TranslatorQuotaExceededException extends TranslatorException {

	public TranslatorQuotaExceededException(String message) {
		super(message);
	}

}
