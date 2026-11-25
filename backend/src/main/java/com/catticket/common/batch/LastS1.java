package com.catticket.common.batch;

import java.time.LocalDate;

/**
 * 前回成功した S1（06 の 5.2、04 の DB-10）。successDate はその S1 の収集の基準日
 * （基準日のない V3 より前の行は started_at の日付。Asia/Seoul）。
 */
public record LastS1(LocalDate successDate, LocalDate windowEndDate) {
}
