package com.catticket.review;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.catticket.review.row.RatingStatRow;

/**
 * レビュー（06 の 4.3）。今は評価の読み取りだけ。レビューの一覧・書き込みは W2-4 で足す。
 */
@Service
public class ReviewService {

	private final ReviewMapper reviewMapper;

	public ReviewService(ReviewMapper reviewMapper) {
		this.reviewMapper = reviewMapper;
	}

	/**
	 * 公演ごとの星の合計と件数。引数・キーは内部の ID（performance.id）。
	 *
	 * @return レビューのない公演は入らない（呼ぶ側が 0 件として扱う）
	 */
	@Transactional(readOnly = true)
	public Map<Long, RatingStat> findRatingStats(Collection<Long> performanceIds) {
		Set<Long> ids = new LinkedHashSet<>();
		if (performanceIds != null) {
			for (Long id : performanceIds) {
				if (id != null) {
					ids.add(id);
				}
			}
		}
		// 空の IN は SQL の誤りになるので、SQL を呼ばない
		if (ids.isEmpty()) {
			return Map.of();
		}
		Map<Long, RatingStat> result = new HashMap<>();
		for (RatingStatRow row : reviewMapper.sumAndCount(ids)) {
			result.put(row.performanceId(), new RatingStat(row.sum(), row.count()));
		}
		return result;
	}

}
