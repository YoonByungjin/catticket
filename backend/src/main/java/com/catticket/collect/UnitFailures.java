package com.catticket.collect;

import org.slf4j.Logger;

import com.catticket.common.batch.BatchRunRecorder;
import com.catticket.common.batch.StepSummary;

/**
 * ステップの中で失敗した単位（公演1件・目録の1区間・順位の1組など）を数える（06 の 4.9 の「ステップ側の扱い」）。
 * 1つでもあればステップは失敗。KopisStoppedException・KopisCallLimitException は単位で捕まえないので、ここには来ない。
 */
final class UnitFailures {

	static final String FAILED = "failed";

	private final StepSummary summary;

	private final Logger log;

	private int count;

	private String first;

	UnitFailures(StepSummary summary, Logger log) {
		this.summary = summary;
		this.log = log;
	}

	void record(String unit, RuntimeException e) {
		count++;
		String reason = BatchRunRecorder.describe(e);
		if (first == null) {
			first = unit + ": " + reason;
		}
		summary.increment(FAILED);
		summary.addDetail(FAILED, unit);
		log.warn("{} failed: {}", unit, reason);
	}

	boolean any() {
		return count > 0;
	}

	/** StepResult の errorMessage（件数と最初の失敗） */
	String message() {
		return count + " failed; first: " + first;
	}

}
