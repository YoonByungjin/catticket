package com.catticket.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.catticket.review.row.RatingStatRow;

/**
 * ReviewService.findRatingStats（06 の 4.3）。
 */
class ReviewServiceTest {

	private final ReviewMapper reviewMapper = mock(ReviewMapper.class);

	private final ReviewService service = new ReviewService(reviewMapper);

	@Test
	void returnsStatsByInternalId() {
		when(reviewMapper.sumAndCount(any()))
			.thenReturn(List.of(new RatingStatRow(1, 13, 3), new RatingStatRow(2, 1, 1)));

		Map<Long, RatingStat> stats = service.findRatingStats(Arrays.asList(1L, 2L, 3L, 1L, null));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Collection<Long>> ids = ArgumentCaptor.forClass(Collection.class);
		verify(reviewMapper).sumAndCount(ids.capture());
		// 同じ ID は1つ、null は渡さない
		assertThat(ids.getValue()).containsExactly(1L, 2L, 3L);
		// レビューのない公演（3）は入らない
		assertThat(stats).containsExactlyInAnyOrderEntriesOf(Map.of(1L, new RatingStat(13, 3), 2L, new RatingStat(1, 1)));
	}

	@Test
	void emptyArgumentDoesNotCallSql() {
		assertThat(service.findRatingStats(List.of())).isEmpty();
		assertThat(service.findRatingStats(null)).isEmpty();
		assertThat(service.findRatingStats(Arrays.asList((Long) null))).isEmpty();
		verifyNoInteractions(reviewMapper);
	}

}
