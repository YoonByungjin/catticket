package com.catticket.collect.client;

/**
 * その実行の KOPIS の呼び出しを止めた（05 の 3.5、06 の 4.9）。止めた後の呼び出しもこれで送らずに終わる。
 * ステップは単位で捕まえず、run の一番外で捕まえて失敗にする（それまでの件数を残す）。
 */
public class KopisStoppedException extends RuntimeException {

	private final KopisStopReason reason;

	public KopisStoppedException(KopisStopReason reason, String message, Throwable cause) {
		super(message, cause);
		this.reason = reason;
	}

	public KopisStopReason reason() {
		return reason;
	}

}
