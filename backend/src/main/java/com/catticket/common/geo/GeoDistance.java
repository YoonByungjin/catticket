package com.catticket.common.geo;

/**
 * 球面上の距離（ハバーサイン。03 の 7.2）。
 */
public final class GeoDistance {

	/** 地球の半径（m。03 の 7.2） */
	public static final double EARTH_RADIUS_METERS = 6_371_000d;

	private GeoDistance() {
	}

	public static double meters(double lat1, double lng1, double lat2, double lng2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
		return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1d, Math.sqrt(a)));
	}

}
