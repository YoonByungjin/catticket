package com.catticket.common.batch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * ステップの件数と内容（06 の 5.2）。ステップの中で数えるので可変。
 * JSON は件数を最上位のキーに展開し、内容は空でなければ "details" の下に置く（toJson）。
 */
public final class StepSummary {

	/** 内容は名前ごとにこの件数まで（多い分の件数は counts にある） */
	public static final int MAX_DETAILS = 50;

	// 足した順を保つ（記録を読む人が見やすいように）
	private final Map<String, Long> counts = new LinkedHashMap<>();

	private final Map<String, List<String>> details = new LinkedHashMap<>();

	public void increment(String name) {
		add(name, 1);
	}

	public void add(String name, long n) {
		counts.merge(name, n, Long::sum);
	}

	public void addDetail(String name, String value) {
		List<String> list = details.computeIfAbsent(name, k -> new ArrayList<>());
		if (list.size() < MAX_DETAILS) {
			list.add(value);
		}
	}

	/** ないときは 0 */
	public long get(String name) {
		return counts.getOrDefault(name, 0L);
	}

	public Map<String, Long> counts() {
		return Collections.unmodifiableMap(counts);
	}

	public Map<String, List<String>> details() {
		return Collections.unmodifiableMap(details);
	}

	/**
	 * JSON の形（例: {@code {"saved":120,"skippedGenre":2}}）。件数を先に並べ、内容は最後に "details" の下に置く。
	 * Jackson 3 の any-getter は他の項目の後ろに並ぶので、順番を決めるため Map にして出す
	 */
	@JsonValue
	Map<String, Object> toJson() {
		Map<String, Object> json = new LinkedHashMap<>(counts);
		if (!details.isEmpty()) {
			json.put("details", details);
		}
		return json;
	}

	/** 名前の内容（ないときは空） */
	public List<String> details(String name) {
		return Collections.unmodifiableList(details.getOrDefault(name, List.of()));
	}

	@Override
	public String toString() {
		return counts.toString();
	}

}
