package com.catticket.collect.row;

import java.math.BigDecimal;

/**
 * 施設詳細から作った会場の値（upsertVenue の引数）。district は区の韓国語の名前（決められなければ null）。
 * 緯度・経度は両方あるか両方 null。
 */
public record VenueValues(String kopisVenueId, String name, String address, String district, BigDecimal latitude,
		BigDecimal longitude) {
}
