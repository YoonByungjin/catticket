package com.catticket.surrounding.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

/**
 * SubwayFileReader（JDK だけの CSV の読み取り。06 の 5.4）。
 */
class SubwayFileReaderTest {

	private static final Charset MS949 = Charset.forName("MS949");

	private final SubwayFileReader reader = new SubwayFileReader();

	private List<CsvRow> read(String text, Charset charset, List<String> required) {
		return reader.read(new ByteArrayResource(text.getBytes(charset)), charset, "test", required);
	}

	@Test
	void quotedValuesWithCommaAndEscapedQuote() {
		List<CsvRow> rows = read("""
				연번,호선,보관함명,상세위치
				1,1,서울역23~44,"2,3번 출입구 복도 사이"
				2,2,충정로B,\"""B"" 구역\"
				""", MS949, SubwayColumns.LOCKER_REQUIRED);

		assertThat(rows).hasSize(2);
		assertThat(rows.get(0).get(SubwayColumns.LOCKER_LOCATION)).isEqualTo("2,3번 출입구 복도 사이");
		assertThat(rows.get(1).get(SubwayColumns.LOCKER_LOCATION)).isEqualTo("\"B\" 구역");
		// 見出しを 1 行目とした行番号
		assertThat(rows.get(0).lineNumber()).isEqualTo(2);
		assertThat(rows.get(1).lineNumber()).isEqualTo(3);
	}

	@Test
	void trailingCommaColumnIsIgnoredAndValuesAreStripped() {
		List<CsvRow> rows = read("""
				연번,호선,한글,한자,영문,중국어,일본어,
				1, 1호선 ,서울역,-,Seoul Station,首?站, ソウルヨク ,
				""", MS949, SubwayColumns.NAME_REQUIRED);

		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).values()).containsOnlyKeys(SubwayColumns.NAME_REQUIRED);
		assertThat(rows.get(0).get(SubwayColumns.LINE)).isEqualTo("1호선");
		assertThat(rows.get(0).get(SubwayColumns.NAME_JA)).isEqualTo("ソウルヨク");
	}

	@Test
	void blankLinesAreSkippedAndBomIsRemoved() {
		List<CsvRow> rows = read("﻿연번,호선,보관함명,상세위치\n\n1,1,시청1~28,대합실\n   \n2,1,종각,대합실\n",
				StandardCharsets.UTF_8, SubwayColumns.LOCKER_REQUIRED);

		assertThat(rows).extracting(r -> r.get(SubwayColumns.LOCKER_NAME)).containsExactly("시청1~28", "종각");
		assertThat(rows).extracting(CsvRow::lineNumber).containsExactly(3, 5);
	}

	@Test
	void crlfLineEndingsAreRead() {
		// 提供元のファイルは CRLF
		List<CsvRow> rows = read("연번,호선,보관함명,상세위치\r\n1,1,시청,대합실\r\n", MS949, SubwayColumns.LOCKER_REQUIRED);
		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).get(SubwayColumns.LOCKER_LOCATION)).isEqualTo("대합실");
	}

	@Test
	void extraEmptyValuesAreIgnored() {
		List<CsvRow> rows = read("연번,호선,보관함명,상세위치\n1,1,시청,대합실,,\n", MS949, SubwayColumns.LOCKER_REQUIRED);
		assertThat(rows).hasSize(1);
	}

	@Test
	void missingRequiredColumnFails() {
		assertThatThrownBy(() -> read("연번,호선,보관함명\n1,1,시청\n", MS949, SubwayColumns.LOCKER_REQUIRED))
			.isInstanceOf(SubwayFileException.class)
			.hasMessage("test 1行目 상세위치: 見出しにない");
	}

	@Test
	void tooFewOrTooManyValuesFail() {
		assertThatThrownBy(() -> read("연번,호선,보관함명,상세위치\n1,1,시청\n", MS949, SubwayColumns.LOCKER_REQUIRED))
			.isInstanceOf(SubwayFileException.class)
			.hasMessageContaining("test 2行目")
			.hasMessageContaining("少ない");
		assertThatThrownBy(() -> read("연번,호선,보관함명,상세위치\n1,1,시청,대합실,여분\n", MS949, SubwayColumns.LOCKER_REQUIRED))
			.isInstanceOf(SubwayFileException.class)
			.hasMessageContaining("多い");
	}

	@Test
	void unclosedQuoteFailsWithStartLine() {
		// ファイルの終わりまで閉じない " は、その行が始まる物理的な行番号で失敗（06 の 5.4）
		assertThatThrownBy(() -> read("연번,호선,보관함명,상세위치\n1,1,시청,대합실\n2,1,종각,\"대합실\n옆\n", MS949,
				SubwayColumns.LOCKER_REQUIRED))
			.isInstanceOf(SubwayFileException.class)
			.hasMessage("test 3行目: 閉じていない \"");
	}

	@Test
	void newlinesInsideQuotesAreKeptAndLineNumbersArePhysical() {
		// 多言語表記の8列目の注記の形（改行1つ・2つ）。名前のない列の値は捨てる
		List<CsvRow> rows = read("연번,호선,한글,한자,영문,중국어,일본어,\n"
				+ "96,4호선,당고개,,Danggogae,堂?,タンゴゲ,\"※진접선 제외\n남양주도시공사 관할\"\n"
				+ "97,4호선,상계,上溪,Sanggye,上溪,サンゲ,\n"
				+ "259,8호선,암사역사공원,-,Amsa,-,アムサ,\"※별내선 5개역\n(장자호수공원) 제외\n구리도시공사관할\"\n"
				+ "260,8호선,천호,千戶,Cheonho,千戶,チョノ,\n", MS949, SubwayColumns.NAME_REQUIRED);

		assertThat(rows).extracting(r -> r.get(SubwayColumns.NAME_KO)).containsExactly("당고개", "상계", "암사역사공원", "천호");
		// その行が始まる物理的な行の番号
		assertThat(rows).extracting(CsvRow::lineNumber).containsExactly(2, 4, 5, 8);
		assertThat(rows.get(0).values()).containsOnlyKeys(SubwayColumns.NAME_REQUIRED);
	}

	@Test
	void crlfNewlineInsideQuotesIsPartOfValue() {
		List<CsvRow> rows = read("연번,호선,보관함명,상세위치\r\n1,1,시청,\"대합실\r\n옆\"\r\n2,1,종각,대합실\r\n", MS949,
				SubwayColumns.LOCKER_REQUIRED);

		assertThat(rows).hasSize(2);
		assertThat(rows.get(0).get(SubwayColumns.LOCKER_LOCATION)).isEqualTo("대합실\r\n옆");
		assertThat(rows).extracting(CsvRow::lineNumber).containsExactly(2, 4);
	}

	@Test
	void errorAfterMultilineRowUsesPhysicalLine() {
		assertThatThrownBy(() -> read("연번,호선,보관함명,상세위치\n1,1,시청,\"대합실\n옆\"\n2,1,종각\n", MS949,
				SubwayColumns.LOCKER_REQUIRED))
			.isInstanceOf(SubwayFileException.class)
			.hasMessageStartingWith("test 4行目: 値の数が見出しより少ない");
	}

	@Test
	void readsRealMultilingualFile() {
		// 置いてある版（05 の 4.2）: 290 行。8列目に改行を含む行（연번 96・259）がある
		List<CsvRow> rows = reader.read(new ClassPathResource("data/subway/서울교통공사_역명다국어표기_20241101.csv"), MS949,
				"station-name-ja", SubwayColumns.NAME_REQUIRED);
		assertThat(rows).hasSize(290);
		assertThat(rows).filteredOn(r -> r.get(SubwayColumns.SEQ).equals("97"))
			.extracting(CsvRow::lineNumber)
			.containsExactly(99);
	}

	@Test
	void crOnlyLineEndingsEndRows() {
		List<CsvRow> rows = read("연번,호선,보관함명,상세위치\r1,1,시청,대합실\r2,1,종각,대합실", MS949,
				SubwayColumns.LOCKER_REQUIRED);
		assertThat(rows).extracting(CsvRow::lineNumber).containsExactly(2, 3);
	}

	@Test
	void missingOrEmptyFileFails() {
		assertThatThrownBy(() -> reader.read(new ClassPathResource("data/subway-sample/no-such-file.csv"), MS949,
				"locker", SubwayColumns.LOCKER_REQUIRED))
			.isInstanceOf(SubwayFileException.class)
			.hasMessageStartingWith("locker: ファイルがない");
		assertThatThrownBy(() -> read("", MS949, SubwayColumns.LOCKER_REQUIRED)).isInstanceOf(SubwayFileException.class)
			.hasMessage("test: 空のファイル");
	}

	@Test
	void readsCp949SampleFile() {
		List<CsvRow> rows = reader.read(new ClassPathResource("data/subway-sample/locker.csv"), MS949, "locker",
				SubwayColumns.LOCKER_REQUIRED);

		assertThat(rows).hasSize(9);
		assertThat(rows.get(0).get(SubwayColumns.LOCKER_NAME)).isEqualTo("서울역1~22");
		assertThat(rows.get(1).get(SubwayColumns.LOCKER_LOCATION)).isEqualTo("2,3번 출입구 복도 사이");
	}

}
