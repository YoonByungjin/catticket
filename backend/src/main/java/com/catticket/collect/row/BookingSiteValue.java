package com.catticket.collect.row;

/**
 * 自動収集の予約サイトの1行（05 の 3.3 の 9.）。name は名前がなければ null。sortOrder は 1 から。
 */
public record BookingSiteValue(String name, String url, int sortOrder) {
}
