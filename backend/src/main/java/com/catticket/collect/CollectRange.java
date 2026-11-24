package com.catticket.collect;

import java.time.LocalDate;

import com.catticket.common.display.Period;

/**
 * S1 の目録の1区間（05 の 3.3）。afterDate が null なら {@code afterdate} を送らない。
 */
public record CollectRange(Period period, LocalDate afterDate) {

	@Override
	public String toString() {
		return period.start() + "~" + period.end() + (afterDate == null ? "" : " afterdate=" + afterDate);
	}

}
