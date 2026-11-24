package com.catticket.collect;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.catticket.common.batch.LastS1;
import com.catticket.common.display.Period;

/**
 * S1・S3 の収集の窓（05 の 3.3）。Spring に依存しない。
 * 窓は今日から 180 日後まで（両端を含む）で、KOPIS の1回の照会の上限 31 日ずつに分ける（6 区間）。
 */
public final class CollectWindows {

	/** 窓の末日 = 今日 + この日数（ADR 0008 2.） */
	static final int WINDOW_DAYS = 180;

	/** 1回の照会の日数（両端を含む。KOPIS の上限） */
	static final int SEGMENT_DAYS = 31;

	private CollectWindows() {
	}

	/** 今日から 180 日後までの 6 区間（S3 も同じ窓を使う。05 の 3.2） */
	public static List<Period> window(LocalDate today) {
		return segments(today, windowEnd(today));
	}

	public static LocalDate windowEnd(LocalDate today) {
		return today.plusDays(WINDOW_DAYS);
	}

	/**
	 * S1 の計画（05 の 3.3 の 2.・3.）。
	 * <ul>
	 * <li>前回の成功がない（または窓の末日の記録がない）: すべての区間を afterdate なし</li>
	 * <li>2回目以降: (a) すべての区間を afterdate = 前回の成功の日の前日、(b) 前回の窓の末日の翌日〜今日 + 180 日を afterdate なし</li>
	 * </ul>
	 */
	public static CollectPlan plan(LocalDate today, Optional<LastS1> lastSuccess) {
		LocalDate end = windowEnd(today);
		List<CollectRange> ranges = new ArrayList<>();
		if (lastSuccess.isEmpty() || lastSuccess.get().successDate() == null
				|| lastSuccess.get().windowEndDate() == null) {
			// 窓の末日がわからないと (b) の範囲を決められないので、初回と同じく全部を取り直す
			window(today).forEach(p -> ranges.add(new CollectRange(p, null)));
			return new CollectPlan(ranges, end);
		}
		LastS1 last = lastSuccess.get();
		// (a) 前回の実行中に変わった分を取りこぼさないよう 1 日ずらす
		LocalDate afterDate = last.successDate().minusDays(1);
		window(today).forEach(p -> ranges.add(new CollectRange(p, afterDate)));
		// (b) 新しく窓に入った日。窓は今日から始まるので、今日より前は照会しない
		LocalDate from = last.windowEndDate().plusDays(1);
		if (from.isBefore(today)) {
			from = today;
		}
		segments(from, end).forEach(p -> ranges.add(new CollectRange(p, null)));
		return new CollectPlan(ranges, end);
	}

	/** from〜to（両端を含む）を 31 日ずつに分ける。from > to なら空 */
	static List<Period> segments(LocalDate from, LocalDate to) {
		List<Period> result = new ArrayList<>();
		for (LocalDate start = from; !start.isAfter(to); start = start.plusDays(SEGMENT_DAYS)) {
			LocalDate segmentEnd = start.plusDays(SEGMENT_DAYS - 1);
			result.add(new Period(start, segmentEnd.isAfter(to) ? to : segmentEnd));
		}
		return result;
	}

}
