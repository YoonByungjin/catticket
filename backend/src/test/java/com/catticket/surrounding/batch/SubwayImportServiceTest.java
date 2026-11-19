package com.catticket.surrounding.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.dao.DataIntegrityViolationException;

import com.catticket.common.batch.BatchContext;
import com.catticket.common.batch.BatchRunRecorder;
import com.catticket.common.batch.JobResult;
import com.catticket.common.batch.StepResult;
import com.catticket.common.batch.StepRun;
import com.catticket.common.batch.StepSummary;
import com.catticket.common.config.CatticketProperties;
import com.catticket.common.time.DateTimes;

/**
 * SubwayImportService（src/test/resources/data/subway-sample/ の CP949 の小さなファイル。SubwayImportWriter・記録はモック）。
 */
class SubwayImportServiceTest {

	private static final String SAMPLE = "classpath:data/subway-sample/";

	private static final LocalDateTime START = LocalDateTime.of(2026, 11, 19, 10, 0, 0);

	// 2026-11-19 10:00:05 Asia/Seoul
	private final Clock clock = Clock.fixed(Instant.parse("2026-11-19T01:00:05Z"), DateTimes.SEOUL);

	private final BatchContext ctx = new BatchContext(5L, START, LocalDate.of(2026, 11, 19));

	private final StepRun step = new StepRun(9L, 5L, "import-subway");

	private BatchRunRecorder recorder;

	private SubwayImportWriter writer;

	@BeforeEach
	void setUp() {
		recorder = mock(BatchRunRecorder.class);
		writer = mock(SubwayImportWriter.class);
		when(recorder.startStep(5L, "import-subway", START)).thenReturn(step);
	}

	private SubwayImportService service(String location, String charset) {
		CatticketProperties.Subway subway = new CatticketProperties.Subway(SAMPLE + location,
				SAMPLE + "station-address.csv", SAMPLE + "locker.csv", SAMPLE + "station-name-ja.csv", charset);
		CatticketProperties props = new CatticketProperties(null, null, null, null, null, null, null, subway, null);
		return new SubwayImportService(recorder, writer, new DefaultResourceLoader(), props, clock);
	}

	private StepResult finishedStep() {
		ArgumentCaptor<StepResult> result = ArgumentCaptor.forClass(StepResult.class);
		verify(recorder).finishStep(eq(step), result.capture(), eq(LocalDateTime.of(2026, 11, 19, 10, 0, 5)));
		return result.getValue();
	}

	@Test
	void importsSampleFiles() {
		JobResult result = service("station-location.csv", "MS949").importAll(ctx);

		assertThat(result).isEqualTo(JobResult.SUCCESS);
		ArgumentCaptor<SubwayImportPlan> planCaptor = ArgumentCaptor.forClass(SubwayImportPlan.class);
		verify(writer).replaceAll(planCaptor.capture(), eq(START));
		SubwayImportPlan plan = planCaptor.getValue();

		// ソウルの駅: 서울・청량리・까치산（名前で結ぶ）・강남・복정・총신대입구・충정로
		assertThat(plan.stations()).extracting(StationInsert::nameKo)
			.containsExactly("서울", "청량리", "까치산", "강남", "복정", "총신대입구", "충정로");
		assertThat(plan.stations()).extracting(StationInsert::nameJa)
			.containsExactly("ソウルヨク", "チョンニャンニ", "カチサン", null, null, null, "チュンジョンノ");
		assertThat(plan.stations().get(0)).isEqualTo(new StationInsert("150", 1, "서울", "ソウルヨク",
				new BigDecimal("37.55315"), new BigDecimal("126.972533")));
		assertThat(plan.lockers()).containsExactly(new LockerInsert("150", "서울역1~22", "1호선 1번 출입구 주변"),
				new LockerInsert("150", "서울역23~44", "2,3번 출입구 복도 사이"), new LockerInsert("243", "충정로(1~15)", "대합실 고객안내센터 옆"),
				new LockerInsert("243", "충정로B", "\"B\" 구역 계단 옆"), new LockerInsert("2828", "복정", "대합실 화장실 옆"));

		StepSummary s = plan.summary();
		assertThat(s.counts()).containsExactly(java.util.Map.entry("stationRows", 10L),
				java.util.Map.entry("stationsSaved", 7L), java.util.Map.entry("stationSkippedLine", 1L),
				java.util.Map.entry("stationOutsideSeoul", 1L), java.util.Map.entry("stationNoAddress", 1L),
				java.util.Map.entry("stationAddressByName", 1L), java.util.Map.entry("jaNameMissing", 2L),
				java.util.Map.entry("jaNameGarbled", 1L), java.util.Map.entry("lockerRows", 9L),
				java.util.Map.entry("lockersSaved", 5L), java.util.Map.entry("lockerSkippedLine", 2L),
				java.util.Map.entry("lockerOutsideSeoul", 1L), java.util.Map.entry("lockerUnmatched", 1L));
		assertThat(s.details("lockerUnmatched")).containsExactly("7|이수|7호선 대합실");
		assertThat(s.details("stationNoAddress")).containsExactly("1|미지|999");

		StepResult stepResult = finishedStep();
		assertThat(stepResult.success()).isTrue();
		assertThat(stepResult.processedCount()).isEqualTo(12);
		assertThat(stepResult.summary()).isSameAs(s);
	}

	@Test
	void missingFileFailsWithoutChangingDb() {
		JobResult result = service("no-such-file.csv", "MS949").importAll(ctx);

		assertThat(result).isEqualTo(JobResult.FAILURE);
		verify(writer, never()).replaceAll(any(), any());
		StepResult stepResult = finishedStep();
		assertThat(stepResult.success()).isFalse();
		assertThat(stepResult.errorMessage()).startsWith("SubwayFileException: station-location: ファイルがない");
	}

	@Test
	void zeroStationsFailsWithoutChangingDb() {
		JobResult result = service("station-location-header-only.csv", "MS949").importAll(ctx);

		assertThat(result).isEqualTo(JobResult.FAILURE);
		verify(writer, never()).replaceAll(any(), any());
		assertThat(finishedStep().errorMessage()).isEqualTo("SubwayFileException: station-location: 入れる駅が 0 件");
	}

	@Test
	void unknownCharsetFails() {
		JobResult result = service("station-location.csv", "NO-SUCH-CHARSET").importAll(ctx);

		assertThat(result).isEqualTo(JobResult.FAILURE);
		verify(writer, never()).replaceAll(any(), any());
		assertThat(finishedStep().errorMessage()).contains("catticket.subway.charset");
	}

	@Test
	void writerFailureIsRecordedAsFailure() {
		doThrow(new DataIntegrityViolationException("Duplicate entry")).when(writer).replaceAll(any(), any());

		JobResult result = service("station-location.csv", "MS949").importAll(ctx);

		assertThat(result).isEqualTo(JobResult.FAILURE);
		StepResult stepResult = finishedStep();
		assertThat(stepResult.success()).isFalse();
		assertThat(stepResult.errorMessage()).isEqualTo("DataIntegrityViolationException: Duplicate entry");
	}

}
