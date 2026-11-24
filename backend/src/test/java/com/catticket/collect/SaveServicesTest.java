package com.catticket.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.annotation.Transactional;

import com.catticket.collect.client.xml.KopisPerformanceDetail;
import com.catticket.collect.client.xml.KopisRelate;
import com.catticket.collect.client.xml.KopisVenueDetail;
import com.catticket.collect.row.BookingSiteValue;
import com.catticket.collect.row.CollectVenueRow;
import com.catticket.collect.row.GenreTrendRow;
import com.catticket.collect.row.PerformanceValues;
import com.catticket.collect.row.RankingCollectionKey;
import com.catticket.collect.row.RankingEntryInsert;
import com.catticket.collect.row.VenueValues;
import com.catticket.common.code.Genre;
import com.catticket.common.code.PeriodType;
import com.catticket.common.display.Period;

/**
 * 保存の Service（PerformanceSaveService・RankingSaveService・GenreTrendSaveService）。CollectMapper はモック。
 */
class SaveServicesTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 6, 0, 0);

	private final CollectMapper mapper = mock(CollectMapper.class);

	@Nested
	class PerformanceSave {

		private final PerformanceSaveService service = new PerformanceSaveService(mapper);

		private final KopisPerformanceDetail detail = ProviderValuesTest.detail();

		@Test
		void isOneTransaction() throws Exception {
			Method m = PerformanceSaveService.class.getMethod("saveFromKopis", KopisPerformanceDetail.class,
					KopisVenueDetail.class, boolean.class, LocalDateTime.class);
			assertThat(m.getAnnotation(Transactional.class)).isNotNull();
		}

		@Test
		void savesNewVenueThenPerformanceThenReplacesCollectedBookingSites() {
			when(mapper.findVenueByKopisId("FC000011")).thenReturn(Optional.of(new CollectVenueRow(7L, "서울특별시 종로구")));
			when(mapper.findPerformanceIdByKopisId("PF303043")).thenReturn(Optional.of(42L));
			KopisVenueDetail venue = new KopisVenueDetail("FC000011", "샤롯데씨어터", "서울특별시 송파구 올림픽로 240",
					"37.5", "127.1");

			SaveOutcome outcome = service.saveFromKopis(detail, venue, false, NOW);

			assertThat(outcome.saved()).isTrue();
			assertThat(outcome.notes()).extracting(CollectNote::name).containsExactly("venueSaved", "saved");
			InOrder order = inOrder(mapper);
			ArgumentCaptor<VenueValues> v = ArgumentCaptor.forClass(VenueValues.class);
			order.verify(mapper).upsertVenue(v.capture(), eq(NOW));
			order.verify(mapper).findVenueByKopisId("FC000011");
			order.verify(mapper).upsertPerformance(any(PerformanceValues.class), eq(7L), eq(true), eq(NOW));
			order.verify(mapper).deleteBookingSites(42L, "COLLECTED");
			order.verify(mapper)
				.insertBookingSites(42L, "COLLECTED",
						List.of(new BookingSiteValue("NHN티켓링크", "https://www.ticketlink.co.kr/product/66145", 1)), NOW);
			assertThat(v.getValue().district()).isEqualTo("송파구");
			assertThat(v.getValue().kopisVenueId()).isEqualTo("FC000011");
		}

		@Test
		void existingVenueIsNotWritten() {
			when(mapper.findVenueByKopisId("FC000011")).thenReturn(Optional.of(new CollectVenueRow(7L, "서울특별시 종로구")));
			when(mapper.findPerformanceIdByKopisId("PF303043")).thenReturn(Optional.of(42L));

			assertThat(service.saveFromKopis(detail, null, false, NOW).saved()).isTrue();

			verify(mapper, never()).upsertVenue(any(), any());
			// 受賞作でなければ受賞歴に触れない
			verify(mapper, never()).markAwarded(any(), any());
		}

		@Test
		void awardedMarksTheAwardAfterTheUpsert() {
			// S4 が保存した S3 の受賞作（06 の 11 の U-13。同じトランザクションの中）
			when(mapper.findVenueByKopisId("FC000011")).thenReturn(Optional.of(new CollectVenueRow(7L, "서울특별시 종로구")));
			when(mapper.findPerformanceIdByKopisId("PF303043")).thenReturn(Optional.of(42L));
			when(mapper.markAwarded(List.of("PF303043"), NOW)).thenReturn(1);

			SaveOutcome outcome = service.saveFromKopis(detail, null, true, NOW);

			assertThat(outcome.saved()).isTrue();
			assertThat(outcome.notes()).extracting(CollectNote::name).containsExactly("saved", "awardMarked");
			InOrder order = inOrder(mapper);
			order.verify(mapper).upsertPerformance(any(PerformanceValues.class), eq(7L), eq(true), eq(NOW));
			order.verify(mapper).markAwarded(List.of("PF303043"), NOW);
		}

		@Test
		void awardedAlreadyMarkedIsNotCounted() {
			when(mapper.findVenueByKopisId("FC000011")).thenReturn(Optional.of(new CollectVenueRow(7L, "서울특별시 종로구")));
			when(mapper.findPerformanceIdByKopisId("PF303043")).thenReturn(Optional.of(42L));
			when(mapper.markAwarded(List.of("PF303043"), NOW)).thenReturn(0);

			SaveOutcome outcome = service.saveFromKopis(detail, null, true, NOW);

			assertThat(outcome.notes()).extracting(CollectNote::name).containsExactly("saved");
		}

		@Test
		void skippedAwardedPerformanceDoesNotMarkTheAward() {
			KopisPerformanceDetail unknownGenre = ProviderValuesTest.detail("PF1", "FC1", "오페라", "공연", null,
					List.of());
			assertThat(service.saveFromKopis(unknownGenre, null, true, NOW).saved()).isFalse();

			// 会場がなくて保存しないときも立てない
			when(mapper.findVenueByKopisId("FC000011")).thenReturn(Optional.empty());
			assertThat(service.saveFromKopis(detail, null, true, NOW).saved()).isFalse();

			verify(mapper, never()).markAwarded(any(), any());
		}

		@Test
		void noBookingSitesOnlyDeletesCollectedRows() {
			KopisPerformanceDetail noSites = ProviderValuesTest.detail("PF1", "FC1", "연극", "공연", null, List.of());
			when(mapper.findVenueByKopisId("FC1")).thenReturn(Optional.of(new CollectVenueRow(7L, null)));
			when(mapper.findPerformanceIdByKopisId("PF1")).thenReturn(Optional.of(42L));

			service.saveFromKopis(noSites, null, false, NOW);

			verify(mapper).deleteBookingSites(42L, "COLLECTED");
			verify(mapper, never()).insertBookingSites(anyLong(), any(), any(), any());
		}

		@Test
		void seoulIsDecidedByAreaOrVenueAddress() {
			// 地域が서울특별시でなくても、会場の住所がソウルならソウルの公演
			KopisPerformanceDetail d = new KopisPerformanceDetail("PF1", "공연", "2026.12.04", "2027.05.02", null, null,
					null, null, null, null, null, "연극", null, null, "FC1", List.<KopisRelate>of());
			when(mapper.findVenueByKopisId("FC1")).thenReturn(Optional.of(new CollectVenueRow(7L, "서울특별시 중구")));
			when(mapper.findPerformanceIdByKopisId("PF1")).thenReturn(Optional.of(1L));
			service.saveFromKopis(d, null, false, NOW);
			verify(mapper).upsertPerformance(any(), eq(7L), eq(true), eq(NOW));

			when(mapper.findVenueByKopisId("FC1")).thenReturn(Optional.of(new CollectVenueRow(7L, "경기도 고양시")));
			service.saveFromKopis(d, null, false, NOW);
			verify(mapper).upsertPerformance(any(), eq(7L), eq(false), eq(NOW));
		}

		@Test
		void skippedPerformanceWritesNothing() {
			KopisPerformanceDetail unknownGenre = ProviderValuesTest.detail("PF1", "FC1", "오페라", "공연", null,
					List.of());

			SaveOutcome outcome = service.saveFromKopis(unknownGenre, null, false, NOW);

			assertThat(outcome.saved()).isFalse();
			assertThat(outcome.notes()).containsExactly(CollectNote.of("skippedGenre", "PF1|오페라"));
			verifyNoInteractions(mapper);
		}

		@Test
		void venueWithoutNameSkipsThePerformance() {
			SaveOutcome outcome = service.saveFromKopis(detail,
					new KopisVenueDetail("FC000011", null, null, null, null), false, NOW);

			assertThat(outcome.saved()).isFalse();
			assertThat(outcome.notes()).containsExactly(CollectNote.of("skippedVenueInvalid", "FC000011|fcltynm"));
			verifyNoInteractions(mapper);
		}

		@Test
		void missingVenueSkipsThePerformance() {
			when(mapper.findVenueByKopisId("FC000011")).thenReturn(Optional.empty());

			SaveOutcome outcome = service.saveFromKopis(detail, null, false, NOW);

			assertThat(outcome.saved()).isFalse();
			assertThat(outcome.notes()).containsExactly(CollectNote.of("venueNotFound", "PF303043|FC000011"));
			verify(mapper, never()).upsertPerformance(any(), anyLong(), anyBoolean(), any());
		}

	}

	@Nested
	class RankingSave {

		private final RankingSaveService service = new RankingSaveService(mapper);

		private final RankingCollectionKey key = new RankingCollectionKey(PeriodType.DAILY, "ALL",
				new Period(LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 23)));

		@Test
		void recordsCompletionAndReplacesRowsInOneTransaction() throws Exception {
			assertThat(RankingSaveService.class
				.getMethod("replace", RankingCollectionKey.class, List.class, LocalDateTime.class)
				.getAnnotation(Transactional.class)).isNotNull();
			when(mapper.findCollectionId(key)).thenReturn(Optional.of(5L));
			List<RankingEntryInsert> entries = List.of(new RankingEntryInsert(1, "PF1"));

			service.replace(key, entries, NOW);

			InOrder order = inOrder(mapper);
			order.verify(mapper).upsertRankingCollection(key, 1, NOW);
			order.verify(mapper).deleteRankingEntries(5L);
			order.verify(mapper).insertRankingEntries(5L, entries, NOW);
		}

		@Test
		void zeroRowsRecordsCompletionWithoutInsert() {
			when(mapper.findCollectionId(key)).thenReturn(Optional.of(5L));

			service.replace(key, List.of(), NOW);

			verify(mapper).upsertRankingCollection(key, 0, NOW);
			verify(mapper).deleteRankingEntries(5L);
			verify(mapper, never()).insertRankingEntries(anyLong(), any(), any());
		}

	}

	@Nested
	class GenreTrendSave {

		private final GenreTrendSaveService service = new GenreTrendSaveService(mapper);

		private final Period period = new Period(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 11, 23));

		@Test
		void replacesThePeriod() throws Exception {
			assertThat(GenreTrendSaveService.class
				.getMethod("replace", Period.class, List.class, GenreTrendBasis.class, LocalDateTime.class)
				.getAnnotation(Transactional.class)).isNotNull();
			List<GenreTrendRow> rows = List.of(new GenreTrendRow(Genre.AAAA, 1, new BigDecimal("100.000000")));

			service.replace(period, rows, GenreTrendBasis.NATIONAL, NOW);

			InOrder order = inOrder(mapper);
			order.verify(mapper).deleteGenreTrends(period);
			order.verify(mapper).insertGenreTrends(period, rows, GenreTrendBasis.NATIONAL, NOW);
		}

		@Test
		void emptyRowsAreRejected() {
			assertThatThrownBy(() -> service.replace(period, List.of(), GenreTrendBasis.NATIONAL, NOW))
				.isInstanceOf(IllegalArgumentException.class);
			verifyNoInteractions(mapper);
		}

	}

}
