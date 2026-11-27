package com.catticket.collect.client;

/**
 * そのステップの1回の実行の割り当てを使い切った（05 の 3.5 の「1回の実行の上限」、06 の 4.9）。送らずに投げる。
 * 遮断と違い、その実行の KOPIS は止めない（次のステップは自分の割り当てで呼ぶ）。
 */
public class KopisCallLimitException extends RuntimeException {

	public KopisCallLimitException(String message) {
		super(message);
	}

}
