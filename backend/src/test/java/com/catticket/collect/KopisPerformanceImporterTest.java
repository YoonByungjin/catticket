package com.catticket.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;

import com.catticket.collect.client.KopisClient;
import com.catticket.collect.client.KopisFatalException;
import com.catticket.collect.client.KopisRetryableException;
import com.catticket.collect.client.StepAbortedException;
import com.catticket.collect.client.xml.KopisPerformanceDetail;
import com.catticket.collect.client.xml.KopisVenueDetail;
import com.catticket.collect.row.CollectVenueRow;
import com.catticket.common.batch.StepSummary;

/**
 * KopisPerformanceImporter（S1・S4 の公演1件。05 の 3.3 の 4.・5.、06 の 6.1）。クライアント・Mapper・保存はモック。
 */
class KopisPerformanceImporterTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 6, 0, 0);

	private final KopisClient client = mock(KopisClient.class);

	private final CollectMapper mapper = mock(CollectMapper.class);

	private final PerformanceSaveService saveService = mock(PerformanceSaveService.class);

	private final KopisPerformanceImporter importer = new KopisPerformanceImporter(client, mapper, saveService);

	private final StepSummary summary = new StepSummary();

	private final UnitFailures failures = new UnitFailures(summary, LoggerFactory.getLogger(getClass()));

	private final KopisPerformanceDetail detail = ProviderValuesTest.detail();

	private final KopisVenueDetail venue = new KopisVenueDetail("FC000011", "샤롯데씨어터", "서울특별시 송파구", null, null);

	private static SaveOutcome saved() {
		return new SaveOutcome(true, List.of(CollectNote.of("venueSaved"), CollectNote.of("saved")));
	}

	@Test
	void newVenueIsFetchedBeforeTheTransaction() {
		when(client.getPerformance("PF303043")).thenReturn(Optional.of(detail));
		when(mapper.findVenueByKopisId("FC000011")).thenReturn(Optional.empty());
		when(client.getVenue("FC000011")).thenReturn(Optional.of(venue));
		when(saveService.saveFromKopis(detail, venue, NOW)).thenReturn(saved());

		importer.importOne("PF303043", NOW, summary, failures);

		// 外部の呼び出しをすべて終えてから保存のトランザクションに入る（06 の 6.1）
		InOrder order = inOrder(client, saveService);
		order.verify(client).getPerformance("PF303043");
		order.verify(client).getVenue("FC000011");
		order.verify(saveService).saveFromKopis(detail, venue, NOW);
		assertThat(summary.get("saved")).isEqualTo(1);
		assertThat(summary.get("venueSaved")).isEqualTo(1);
		assertThat(failures.any()).isFalse();
	}

	@Test
	void existingVenueIsNotFetched() {
		when(client.getPerformance("PF303043")).thenReturn(Optional.of(detail));
		when(mapper.findVenueByKopisId("FC000011")).thenReturn(Optional.of(new CollectVenueRow(7L, "서울특별시")));
		when(saveService.saveFromKopis(detail, null, NOW))
			.thenReturn(new SaveOutcome(true, List.of(CollectNote.of("saved"))));

		importer.importOne("PF303043", NOW, summary, failures);

		verify(client, never()).getVenue(anyString());
		verify(saveService).saveFromKopis(eq(detail), isNull(), eq(NOW));
		assertThat(summary.get("saved")).isEqualTo(1);
	}

	@Test
	void detailNotFoundIsCountedNotFailed() {
		when(client.getPerformance("PF1")).thenReturn(Optional.empty());

		importer.importOne("PF1", NOW, summary, failures);

		assertThat(summary.get("detailNotFound")).isEqualTo(1);
		assertThat(summary.details("detailNotFound")).containsExactly("PF1");
		assertThat(failures.any()).isFalse();
		verify(saveService, never()).saveFromKopis(any(), any(), any());
	}

	@Test
	void skippedPerformanceDoesNotQueryTheVenue() {
		KopisPerformanceDetail unknownGenre = ProviderValuesTest.detail("PF1", "FC1", "오페라", "공연", null, List.of());
		when(client.getPerformance("PF1")).thenReturn(Optional.of(unknownGenre));

		importer.importOne("PF1", NOW, summary, failures);

		assertThat(summary.get("skippedGenre")).isEqualTo(1);
		assertThat(summary.details("skippedGenre")).containsExactly("PF1|오페라");
		verify(client, never()).getVenue(anyString());
		verify(saveService, never()).saveFromKopis(any(), any(), any());
	}

	@Test
	void venueNotFoundSkipsThePerformance() {
		when(client.getPerformance("PF303043")).thenReturn(Optional.of(detail));
		when(mapper.findVenueByKopisId("FC000011")).thenReturn(Optional.empty());
		when(client.getVenue("FC000011")).thenReturn(Optional.empty());

		importer.importOne("PF303043", NOW, summary, failures);

		assertThat(summary.get("venueNotFound")).isEqualTo(1);
		assertThat(failures.any()).isFalse();
		verify(saveService, never()).saveFromKopis(any(), any(), any());
	}

	@Test
	void kopisFailuresAreCountedAsFailedUnits() {
		when(client.getPerformance("PF1")).thenThrow(new KopisRetryableException("KOPIS pblprfr/{id} HTTP 503"));
		when(client.getPerformance("PF303043")).thenReturn(Optional.of(detail));
		when(mapper.findVenueByKopisId("FC000011")).thenReturn(Optional.empty());
		when(client.getVenue("FC000011")).thenThrow(new KopisFatalException("KOPIS prfplc/{id} HTTP 404"));

		importer.importOne("PF1", NOW, summary, failures);
		importer.importOne("PF303043", NOW, summary, failures);

		assertThat(summary.get("failed")).isEqualTo(2);
		assertThat(summary.details("failed")).containsExactly("performance PF1", "venue FC000011 of PF303043");
		assertThat(failures.message())
			.isEqualTo("2 failed; first: performance PF1: KopisRetryableException: KOPIS pblprfr/{id} HTTP 503");
		verify(saveService, never()).saveFromKopis(any(), any(), any());
	}

	@Test
	void stepAbortedIsNotCaught() {
		when(client.getPerformance("PF1")).thenThrow(new StepAbortedException("aborted"));

		assertThatThrownBy(() -> importer.importOne("PF1", NOW, summary, failures))
			.isInstanceOf(StepAbortedException.class);
		assertThat(failures.any()).isFalse();
	}

}
