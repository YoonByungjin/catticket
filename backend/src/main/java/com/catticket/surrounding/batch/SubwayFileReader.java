package com.catticket.surrounding.batch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;

/**
 * 駅・コインロッカーの CSV を読む（06 の 5.4）。CSV のライブラリは使わず JDK だけで読む（06 の 1.3）。
 * RFC 4180 のとおり: 区切りはカンマ。{@code "} で囲んだ値はカンマ・改行を含んでよく、中の {@code ""} は {@code "} 1つ。
 * 行（レコード）は引用符の外の改行（{@code \r\n}・{@code \n}・{@code \r}）で終わる。
 * 行番号はその行が始まる物理的な行の番号（テキストエディタの行と同じにして、人がファイルを直しやすくする）。
 */
public class SubwayFileReader {

	private static final char BOM = '﻿';

	/** 物理的な行番号つきの1行（レコード）の値 */
	record Record(int lineNumber, List<String> values) {
	}

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

	List<CsvRow> parse(Reader reader, String label, List<String> requiredColumns) throws IOException {
		List<Record> records = records(reader, label);
		if (records.isEmpty()) {
			throw new SubwayFileException(label + ": 空のファイル");
		}
		Record headerRecord = records.get(0);
		List<String> header = new ArrayList<>(headerRecord.values().stream().map(String::strip).toList());
		// UTF-8 の版に替わっても読めるように BOM を除く
		if (!header.get(0).isEmpty() && header.get(0).charAt(0) == BOM) {
			header.set(0, header.get(0).substring(1).strip());
		}
		for (String column : requiredColumns) {
			if (!header.contains(column)) {
				throw new SubwayFileException(
						label + " " + headerRecord.lineNumber() + "行目 " + column + ": 見出しにない");
			}
		}

		List<CsvRow> rows = new ArrayList<>();
		for (Record record : records.subList(1, records.size())) {
			int lineNumber = record.lineNumber();
			List<String> values = record.values();
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
				// 名前のない列（多言語表記の末尾のカンマ）は、値が入っていても捨てる
				if (!header.get(i).isEmpty()) {
					map.put(header.get(i), values.get(i).strip());
				}
			}
			rows.add(new CsvRow(lineNumber, map));
		}
		return rows;
	}

	/**
	 * 行（レコード）に分ける。空の行（引用符の外で空白だけの行を含む）は除く。
	 * ファイルの終わりまで閉じない {@code "} は、その行が始まる行番号で失敗にする
	 */
	static List<Record> records(Reader reader, String label) throws IOException {
		List<Record> records = new ArrayList<>();
		List<String> values = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean quoted = false;
		boolean fieldStart = true;
		int physicalLine = 1;
		int recordStart = 1;
		int c = reader.read();
		while (c != -1) {
			int next = reader.read();
			char ch = (char) c;
			if (quoted) {
				if (ch == '"') {
					if (next == '"') {
						current.append('"');
						next = reader.read();
					}
					else {
						quoted = false;
					}
				}
				else {
					// 値の中の改行はそのまま残す。物理的な行は数える
					if (ch == '\n' || (ch == '\r' && next != '\n')) {
						physicalLine++;
					}
					current.append(ch);
				}
				fieldStart = false;
			}
			else if (ch == ',') {
				values.add(current.toString());
				current.setLength(0);
				fieldStart = true;
			}
			else if (ch == '\r' || ch == '\n') {
				if (ch == '\r' && next == '\n') {
					next = reader.read();
				}
				values.add(current.toString());
				addRecord(records, recordStart, values);
				values = new ArrayList<>();
				current.setLength(0);
				fieldStart = true;
				physicalLine++;
				recordStart = physicalLine;
			}
			else if (ch == '"' && fieldStart) {
				quoted = true;
				fieldStart = false;
			}
			else {
				current.append(ch);
				fieldStart = false;
			}
			c = next;
		}
		if (quoted) {
			throw new SubwayFileException(label + " " + recordStart + "行目: 閉じていない \"");
		}
		values.add(current.toString());
		addRecord(records, recordStart, values);
		return records;
	}

	private static void addRecord(List<Record> records, int lineNumber, List<String> values) {
		// 空の行・空白だけの行（引用符もカンマもない）は読み飛ばす
		boolean blank = values.size() == 1 && values.get(0).isBlank();
		if (!blank) {
			records.add(new Record(lineNumber, List.copyOf(values)));
		}
	}

	private static String describe(Resource resource) {
		return resource == null ? "null" : resource.getDescription();
	}

}
