package com.catticket.collect.client;

/**
 * その実行の KOPIS の呼び出しを止めた理由（05 の 3.5、06 の 4.9）。
 */
public enum KopisStopReason {

	/** 遮断の印を受けた */
	BLOCKED,

	/** 実行の中で続けて 5 回失敗した */
	CONSECUTIVE_FAILURES

}
