package com.catticket.common.batch;

import java.util.Arrays;
import java.util.Optional;

/**
 * 手で実行する処理の名前（05 の 1.1、06 の 5.1）。値は {@code --catticket.task=} とロック名・記録の job_name に使う。
 */
public enum TaskName {

	DAILY_COLLECT("daily-collect"),

	IMPORT_SUBWAY("import-subway"),

	CREATE_ADMIN("create-admin"),

	PURGE_EXPIRED_SIGNUPS("purge-expired-signups");

	private final String value;

	TaskName(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	/** 知らない名前・null は空 */
	public static Optional<TaskName> fromValue(String value) {
		return Arrays.stream(values()).filter(t -> t.value.equals(value)).findFirst();
	}

}
