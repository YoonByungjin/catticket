package com.catticket.common.validation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * ページングのクエリ（03 の 7.1）。省略は page = 0、size = 20。
 */
public record PageQuery(
		@Min(value = 0, message = "RANGE") @Max(value = 10000, message = "RANGE") Integer page,
		@Min(value = 1, message = "RANGE") @Max(value = 100, message = "RANGE") Integer size) {

	public static final int DEFAULT_PAGE = 0;

	public static final int DEFAULT_SIZE = 20;

	@Override
	public Integer page() {
		return page == null ? DEFAULT_PAGE : page;
	}

	@Override
	public Integer size() {
		return size == null ? DEFAULT_SIZE : size;
	}

	public int offset() {
		return page() * size();
	}

}
