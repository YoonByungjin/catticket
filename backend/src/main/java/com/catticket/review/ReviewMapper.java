package com.catticket.review;

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.catticket.review.row.RatingStatRow;

/**
 * レビューの Mapper（mapper/review/ReviewMapper.xml。06 の 4.3）。書き込みのメソッドは W2-4 で足す。
 */
@Mapper
public interface ReviewMapper {

	/** performanceIds は内部の ID。空なら呼ばない（空の IN は SQL の誤り）。レビューのない公演は行が返らない */
	List<RatingStatRow> sumAndCount(Collection<Long> performanceIds);

}
