package com.catticket.collect.row;

/**
 * DB にある会場（ソウルの公演の判定に住所を使う。05 の 3.3 の 6.）。
 */
public record CollectVenueRow(long id, String address) {
}
