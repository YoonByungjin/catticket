package com.catticket.common.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class GeoTest {

	@Test
	void metersByHaversine() {
		assertThat(GeoDistance.meters(37.5, 127.0, 37.5, 127.0)).isZero();
		// 緯度 0.001 度 ≒ 6,371,000 × π / 180 × 0.001 ≒ 111.19 m
		assertThat(GeoDistance.meters(37.5, 127.0, 37.501, 127.0)).isCloseTo(111.19, within(0.01));
	}

	@Test
	void boundsContainEveryPointWithinDistance() {
		BigDecimal lat = new BigDecimal("37.566500");
		BigDecimal lng = new BigDecimal("126.978000");
		GeoBounds bounds = GeoBounds.around(lat, lng, 600);

		assertThat(bounds.minLatitude().scale()).isEqualTo(6);
		// 四角の辺の中点はちょうど 600 m 以上離れている（取りこぼしがない）
		assertThat(GeoDistance.meters(37.5665, 126.978, bounds.maxLatitude().doubleValue(), 126.978))
			.isGreaterThanOrEqualTo(600);
		assertThat(GeoDistance.meters(37.5665, 126.978, bounds.minLatitude().doubleValue(), 126.978))
			.isGreaterThanOrEqualTo(600);
		assertThat(GeoDistance.meters(37.5665, 126.978, 37.5665, bounds.maxLongitude().doubleValue()))
			.isGreaterThanOrEqualTo(600);
		assertThat(GeoDistance.meters(37.5665, 126.978, 37.5665, bounds.minLongitude().doubleValue()))
			.isGreaterThanOrEqualTo(600);
	}

}
