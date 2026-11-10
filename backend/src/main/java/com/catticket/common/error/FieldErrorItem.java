package com.catticket.common.error;

/**
 * 項目ごとの入力の誤り（03 の 7.3 の {@code errors[]}）。
 */
public record FieldErrorItem(String field, String reason) {
}
