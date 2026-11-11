package com.catticket.common.display;

import java.time.LocalDate;

/**
 * 集計期間（03 の 7.2）。日次は start = end。
 */
public record Period(LocalDate start, LocalDate end) {
}
