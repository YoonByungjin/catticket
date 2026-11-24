package com.catticket.collect.client;

/**
 * 再試行しない KOPIS の失敗（05 の 3.5）。結果コード 01・02・05・06・知らないコードなど。
 * 何度呼んでも同じなので、呼び出し元のステップを失敗にする。メッセージに API キーを入れない。
 */
public class KopisFatalException extends RuntimeException {

	public KopisFatalException(String message) {
		super(message);
	}

}
