package com.catticket.surrounding.batch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;

/**
 * 駅・コインロッカーの CSV を読む（06 の 5.4）。CSV のライブラリは使わず JDK だけで読む（06 の 1.3）。
 * 区切りはカンマ。{@code "} で囲んだ値はカンマを含んでよく、中の {@code ""} は {@code "} 1つ。
 * 引用符の中の改行は扱わない（06 の 5.4。閉じていない {@code "} として失敗になる）。
 */
public class SubwayFileReader {

	private static final char BOM = '﻿';

	/**
	 * @param label           記録・メッセージに出すファイルの用途（例: {@code locker}）
	 * @param requiredColumns 見出しになければ失敗にする列の名前
	 */
	public List<CsvRow> read(Resource resource, Charset charset, String label, List<String> requiredColumns) {
		if (resource == null || !resource.exists()) {
			throw new SubwayFileException(label + ": ファイルがない (" + describe(resource) + ")");
		}
		// getFile() は jar の中で使えないので getInputStream() で読む（06 の 8.1）
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), charset))) {
			return parse(reader, label, requiredColumns);
		}
		catch (IOException e) {
			throw new SubwayFileException(label + ": 読めない (" + describe(resource) + ": " + e.getMessage() + ")",
					e);
		}
	}

	List<CsvRow> parse(BufferedReader reader, String label, List<String> requiredColumns) throws IOException {
		String headerLine = reader.readLine();
		if (headerLine == null) {
			throw new SubwayFileException(label + ": 空のファイル");
		}
		// UTF-8 の版に替わっても読めるように BOM を除く
		if (!headerLine.isEmpty() && headerLine.charAt(0) == BOM) {
			headerLine = headerLine.substring(1);
		}
		List<String> header = split(headerLine, label, 1).stream().map(String::strip).toList();
		for (String column : requiredColumns) {
			if (!header.contains(column)) {
				throw new SubwayFileException(label + " 1行目 " + column + ": 見出しにない");
			}
		}

		List<CsvRow> rows = new ArrayList<>();
		int lineNumber = 1;
		String line;
		while ((line = reader.readLine()) != null) {
			lineNumber++;
			if (line.isBlank()) {
				continue;
			}
			List<String> values = split(line, label, lineNumber);
			if (values.size() < header.size()) {
				throw new SubwayFileException(label + " " + lineNumber + "行目: 値の数が見出しより少ない（" + values.size()
						+ " < " + header.size() + "）");
			}
			for (int i = header.size(); i < values.size(); i++) {
				if (!values.get(i).isBlank()) {
					throw new SubwayFileException(label + " " + lineNumber + "行目: 値の数が見出しより多い（"
							+ values.size() + " > " + header.size() + "）");
				}
			}
			Map<String, String> map = new LinkedHashMap<>();
			for (int i = 0; i < header.size(); i++) {
				// 名前のない列（多言語表記の末尾のカンマ）は無視する
				if (!header.get(i).isEmpty()) {
					map.put(header.get(i), values.get(i).strip());
				}
			}
			rows.add(new CsvRow(lineNumber, map));
		}
		return rows;
	}

	/** 1行をカンマで分ける。引用符の中のカンマは値の一部、{@code ""} は {@code "} */
	static List<String> split(String line, String label, int lineNumber) {
		List<String> values = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean quoted = false;
		boolean fieldStart = true;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (quoted) {
				if (c == '"') {
					if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
						current.append('"');
						i++;
					}
					else {
						quoted = false;
					}
				}
				else {
					current.append(c);
				}
			}
			else if (c == ',') {
				values.add(current.toString());
				current.setLength(0);
				fieldStart = true;
				continue;
			}
			else if (c == '"' && fieldStart) {
				quoted = true;
			}
			else {
				current.append(c);
			}
			fieldStart = false;
		}
		if (quoted) {
			throw new SubwayFileException(label + " " + lineNumber + "行目: 閉じていない \"");
		}
		values.add(current.toString());
		return values;
	}

	private static String describe(Resource resource) {
		return resource == null ? "null" : resource.getDescription();
	}

}
