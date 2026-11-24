package com.catticket.collect;

/**
 * ジャンル別統計の応答を使えない（知らないジャンル・同じジャンルの重複・予約数の形の誤り。05 の 3.2 の「S6 の詳細」）。
 * その回は何も保存せず S6 を失敗にする。
 */
public class InvalidGenreStatException extends RuntimeException {

	public InvalidGenreStatException(String message) {
		super(message);
	}

}
