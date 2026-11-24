package com.catticket.collect.client;

import java.util.List;
import java.util.Objects;

import com.catticket.collect.client.xml.KopisEnvelope;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * KOPIS の XML の読み方（06 の 7.1）。Spring に依存しない。
 * 結果コードの応答はどのサービスも {@code dbs/db/returncode} なので、根要素の名前ではなく中身で見分ける。
 */
public class KopisXmlReader {

	/** データなし（詳細は存在しない ID）。失敗にしない */
	static final String NO_DATA = "04";

	/** DB エラー。再試行する */
	static final String DB_ERROR = "03";

	private final XmlMapper xmlMapper = XmlMapper.builder().build();

	/**
	 * 行を読む。空の根要素（{@code <dbs/>}・{@code <boxofs/>} など）と結果コード 04 は空のリスト。
	 *
	 * @throws KopisFatalException     01・02・05・06・知らないコード
	 * @throws KopisRetryableException 03、XML として読めない本文
	 */
	public <T> List<T> readRows(String service, byte[] body, Class<? extends KopisEnvelope<T>> envelopeType) {
		JsonNode tree = readTree(service, body);
		if (isEmpty(tree)) {
			return List.of();
		}
		JsonNode first = firstDb(tree);
		if (first != null && first.has("returncode")) {
			String code = text(first.get("returncode"));
			if (NO_DATA.equals(code)) {
				return List.of();
			}
			String message = "KOPIS " + service + " returncode=" + code + " errmsg=" + text(first.get("errmsg"));
			if (DB_ERROR.equals(code)) {
				throw new KopisRetryableException(message);
			}
			throw new KopisFatalException(message);
		}
		// 繰り返しの要素は木からの変換より readValue の方が確かなので、本文から読み直す
		KopisEnvelope<T> envelope;
		try {
			envelope = xmlMapper.readValue(body, envelopeType);
		}
		catch (JacksonException e) {
			throw unreadable(service, e);
		}
		List<T> rows = envelope == null ? null : envelope.rows();
		return rows == null ? List.of() : rows.stream().filter(Objects::nonNull).toList();
	}

	private JsonNode readTree(String service, byte[] body) {
		if (body == null || body.length == 0) {
			throw new KopisRetryableException("KOPIS " + service + " unreadable response: empty body");
		}
		try {
			return xmlMapper.readTree(body);
		}
		catch (JacksonException e) {
			throw unreadable(service, e);
		}
	}

	// 子のない根要素は Jackson のバージョンで空の object・空の文字列のどちらにもなりうるので、両方を 0 件にする
	private static boolean isEmpty(JsonNode tree) {
		if (tree == null || tree.isMissingNode() || tree.isNull()) {
			return true;
		}
		if (tree.isString()) {
			return tree.asString().isBlank();
		}
		return (tree.isObject() || tree.isArray()) && tree.isEmpty();
	}

	// db が1つなら object、複数なら array になる
	private static JsonNode firstDb(JsonNode tree) {
		JsonNode db = tree.get("db");
		if (db == null) {
			return null;
		}
		if (db.isArray()) {
			return db.isEmpty() ? null : db.get(0);
		}
		return db.isObject() ? db : null;
	}

	private static String text(JsonNode node) {
		return node == null || node.isNull() ? null : node.asString().strip();
	}

	private static KopisRetryableException unreadable(String service, JacksonException e) {
		// 読めない本文（途中で切れた応答・保守中の HTML など）は通信の失敗と同じく再試行し、続けば 10 回で止まる
		return new KopisRetryableException(
				"KOPIS " + service + " unreadable response: " + e.getClass().getSimpleName() + ": "
						+ e.getOriginalMessage());
	}

}
