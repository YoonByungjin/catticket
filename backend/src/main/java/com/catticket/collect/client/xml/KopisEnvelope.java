package com.catticket.collect.client.xml;

import java.util.List;

/**
 * KOPIS の応答の根要素（06 の 7.1）。行要素のリストを返す。
 */
public interface KopisEnvelope<T> {

	/** 行。行要素がなければ null のことがある（読む側で空のリストにする） */
	List<T> rows();

}
