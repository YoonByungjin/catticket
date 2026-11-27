package com.catticket.collect;

/**
 * StepSummary に数える1件（name を1足し、detail があれば内容に残す）。
 */
public record CollectNote(String name, String detail) {

	static CollectNote of(String name) {
		return new CollectNote(name, null);
	}

	static CollectNote of(String name, String detail) {
		return new CollectNote(name, detail);
	}

}
