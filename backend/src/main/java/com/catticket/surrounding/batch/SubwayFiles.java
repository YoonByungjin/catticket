package com.catticket.surrounding.batch;

import java.util.List;

/**
 * 読んだ4つのファイル（06 の 5.4）。
 */
public record SubwayFiles(List<CsvRow> locations, List<CsvRow> addresses, List<CsvRow> lockers,
		List<CsvRow> names) {
}
