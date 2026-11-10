package com.catticket.common.geo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * SQL の前の絞り込み用の緯度経度の四角。正確な距離の判定は Java（{@link GeoDistance}）で行う。
 */
public record GeoBounds(BigDecimal minLatitude, BigDecimal maxLatitude, BigDecimal minLongitude,
		BigDecimal maxLongitude) {

	/** DB の DECIMAL(9,6) と同じ桁 */
	private static final int SCALE = 6;

	public static GeoBounds around(BigDecimal latitude, BigDecimal longitude, int meters) {
		double lat = latitude.doubleValue();
		double lng = longitude.doubleValue();
		double dLat = Math.toDegrees(meters / GeoDistance.EARTH_RADIUS_METERS);
		// 極の近くで cos が 0 になっても範囲が無限にならないようにする（ソウルでは起きない）
		double cos = Math.max(Math.cos(Math.toRadians(lat)), 0.01);
		double dLng = dLat / cos;
		// 境目の取りこぼしがないよう、外側に丸める
		return new GeoBounds(BigDecimal.valueOf(lat - dLat).setScale(SCALE, RoundingMode.FLOOR),
				BigDecimal.valueOf(lat + dLat).setScale(SCALE, RoundingMode.CEILING),
				BigDecimal.valueOf(lng - dLng).setScale(SCALE, RoundingMode.FLOOR),
				BigDecimal.valueOf(lng + dLng).setScale(SCALE, RoundingMode.CEILING));
	}

}
