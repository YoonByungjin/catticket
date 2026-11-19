package com.catticket.surrounding.batch;

import java.util.Map;

/**
 * CSV の1行（06 の 5.4）。lineNumber は見出しを 1 行目とした行番号。値は strip 済みで、見出しの名前で引く。
 */
public record CsvRow(int lineNumber, Map<String, String> values) {

	public CsvRow {
		values = Map.copyOf(values);
	}

	/** 列の値（ない列は空文字） */
	public String get(String column) {
		return values.getOrDefault(column, "");
	}

}
