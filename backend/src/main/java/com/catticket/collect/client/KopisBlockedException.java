package com.catticket.collect.client;

/**
 * KOPIS の遮断の印（05 の 3.5 の「遮断の印」、06 の 4.9）。403・429、結果コードの XML でない 4xx、本文の {@code Request Blocked}。
 * 再試行せず、{@link KopisCallPolicy} がその実行の呼び出しをすべて止める。
 * ステップの単位で捕まらないよう、KopisRetryableException・KopisFatalException の子にしない。メッセージにキー・URL・本文を入れない。
 */
public class KopisBlockedException extends RuntimeException {

	public KopisBlockedException(String message) {
		super(message);
	}

}
