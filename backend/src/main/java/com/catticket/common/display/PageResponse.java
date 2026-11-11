package com.catticket.common.display;

import java.util.List;

import com.catticket.common.validation.PageQuery;

/**
 * 一覧の応答（03 の 7.2 の Page）。
 */
public record PageResponse<T>(List<T> items, int page, int size, long totalElements, long totalPages) {

	public PageResponse {
		items = items == null ? List.of() : List.copyOf(items);
	}

	public static <T> PageResponse<T> of(List<T> items, PageQuery query, long total) {
		int size = query.size();
		// 0 件ならページ数も 0（03 の 7.2）
		long totalPages = (total + size - 1) / size;
		return new PageResponse<>(items, query.page(), size, total, totalPages);
	}

}
