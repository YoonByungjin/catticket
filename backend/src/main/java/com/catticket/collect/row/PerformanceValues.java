package com.catticket.collect.row;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 公演詳細から作った自動収集の値（ProviderValues が A-02 の長さの規則を通したもの）。
 * upsertPerformance の引数。areaSeoul は公演の地域が「서울특별시」か（ソウルの判定の半分。05 の 3.3 の 6.）。
 */
public record PerformanceValues(String kopisPerformanceId, String kopisVenueId, String title, LocalDate startDate,
		LocalDate endDate, String genreCode, String castText, String ticketPrice, String scheduleText, String runtime,
		String ageLimit, String posterUrl, boolean openRun, boolean areaSeoul, LocalDateTime kopisUpdatedAt,
		List<BookingSiteValue> bookingSites) {

	public PerformanceValues {
		bookingSites = List.copyOf(bookingSites);
	}

}
