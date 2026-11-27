package com.catticket.collect.client;

import java.util.List;

/**
 * 一覧の1ページ（{@code rows=100}）。
 */
public record KopisPage<T>(List<T> items) {

	/** 1ページの行数（KOPIS の上限。101 以上は結果コード 06） */
	public static final int ROWS = 100;

	public KopisPage {
		items = List.copyOf(items);
	}

	/** 行が 100 件より少なければ最後のページ（空の応答も含む。05 の 3.3 の 1.） */
	public boolean isLast() {
		return items.size() < ROWS;
	}

}
