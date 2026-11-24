package com.catticket.collect;

import java.time.LocalDate;
import java.util.List;

/**
 * S1 の照会の計画（05 の 3.3）。windowEnd は今回の窓の末日（S1 がすべて成功したときに記録する）。
 */
public record CollectPlan(List<CollectRange> ranges, LocalDate windowEnd) {

	public CollectPlan {
		ranges = List.copyOf(ranges);
	}

}
