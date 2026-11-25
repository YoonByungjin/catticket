package com.catticket.common.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * BatchRunMapper（実際の MySQL のテスト用 DB）。各テストはロールバックされる。
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BatchRunMapperTest {

	private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 19, 6, 0, 0);

	@Autowired
	BatchRunMapper mapper;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@BeforeEach
	void clear() {
		// ロールバックされるので、ほかの実行の記録は残る
		jdbcTemplate.update("DELETE FROM batch_job_run");
	}

	private long insertJob(String name, LocalDateTime startedAt) {
		JobRunInsert row = new JobRunInsert(name, startedAt);
		mapper.insertJobRun(row);
		return row.getId();
	}

	private long insertStep(long jobRunId, String step, LocalDateTime startedAt) {
		StepRunInsert row = new StepRunInsert(jobRunId, step, startedAt);
		mapper.insertStepRun(row);
		return row.getId();
	}

	@Test
	void jobRunIsInsertedAndFinished() {
		long id = insertJob("import-subway", T0);
		assertThat(id).isPositive();

		Map<String, Object> started = jdbcTemplate.queryForMap("SELECT * FROM batch_job_run WHERE id = ?", id);
		assertThat(started.get("job_name")).isEqualTo("import-subway");
		assertThat(started.get("started_at")).isEqualTo(T0);
		assertThat(started.get("finished_at")).isNull();
		assertThat(started.get("result")).isNull();

		mapper.updateJobRun(id, T0.plusMinutes(1), "FAILURE", "SubwayFileException: locker 12行目 보관함명: 空");

		Map<String, Object> finished = jdbcTemplate.queryForMap("SELECT * FROM batch_job_run WHERE id = ?", id);
		assertThat(finished.get("finished_at")).isEqualTo(T0.plusMinutes(1));
		assertThat(finished.get("result")).isEqualTo("FAILURE");
		assertThat(finished.get("error_message")).isEqualTo("SubwayFileException: locker 12行目 보관함명: 空");
		assertThat(finished.get("updated_at")).isEqualTo(T0.plusMinutes(1));
	}

	@Test
	void stepRunIsInsertedAndFinished() {
		long jobId = insertJob("import-subway", T0);
		long stepId = insertStep(jobId, "import-subway", T0);

		mapper.updateStepRun(stepId, T0.plusSeconds(5), "SUCCESS", 574L, "{\"stationsSaved\":261}", null, null, null);

		Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM batch_step_run WHERE id = ?", stepId);
		assertThat(((Number) row.get("job_run_id")).longValue()).isEqualTo(jobId);
		assertThat(row.get("step_name")).isEqualTo("import-subway");
		assertThat(row.get("result")).isEqualTo("SUCCESS");
		assertThat(((Number) row.get("processed_count")).longValue()).isEqualTo(574L);
		assertThat(row.get("summary")).isEqualTo("{\"stationsSaved\":261}");
		assertThat(row.get("error_message")).isNull();
		assertThat(row.get("window_end_date")).isNull();
		assertThat(row.get("finished_at")).isEqualTo(T0.plusSeconds(5));
	}

	@Test
	void successfulS1WritesWindowEndDate() {
		// 06 の 11 の U-11: 成功した S1 の窓の末日を updateStepRun で書く
		long jobId = insertJob("daily-collect", T0);
		long s1 = insertStep(jobId, "S1", T0);

		mapper.updateStepRun(s1, T0.plusMinutes(3), "SUCCESS", 120L, "{\"saved\":120}", null,
				LocalDate.of(2027, 5, 18), null);

		assertThat(jdbcTemplate.queryForObject("SELECT window_end_date FROM batch_step_run WHERE id = ?",
				LocalDate.class, s1))
			.isEqualTo(LocalDate.of(2027, 5, 18));
		assertThat(mapper.findLastSuccessfulS1()).contains(new LastS1(LocalDate.of(2026, 11, 19), LocalDate.of(2027, 5, 18)));
	}

	@Test
	void findLastSuccessfulS1IsEmptyWithoutSuccess() {
		long jobId = insertJob("daily-collect", T0);
		long s1 = insertStep(jobId, "S1", T0);
		mapper.updateStepRun(s1, T0.plusMinutes(1), "FAILURE", 0L, null, "boom", null, null);

		assertThat(mapper.findLastSuccessfulS1()).isEmpty();
	}

	@Test
	void findLastSuccessfulS1ReturnsLatestSuccessfulS1() {
		// 古い成功
		long job1 = insertJob("daily-collect", T0.minusDays(2));
		long old = insertStep(job1, "S1", T0.minusDays(2));
		mapper.updateStepRun(old, T0.minusDays(2).plusMinutes(1), "SUCCESS", 10L, null, null, null, null);
		jdbcTemplate.update("UPDATE batch_step_run SET window_end_date = ? WHERE id = ?",
				LocalDate.of(2027, 5, 16), old);
		// 新しい成功（ここを返す）
		long job2 = insertJob("daily-collect", T0.minusDays(1));
		long latest = insertStep(job2, "S1", T0.minusDays(1));
		mapper.updateStepRun(latest, T0.minusDays(1).plusMinutes(1), "SUCCESS", 10L, null, null, null, null);
		jdbcTemplate.update("UPDATE batch_step_run SET window_end_date = ? WHERE id = ?",
				LocalDate.of(2027, 5, 17), latest);
		// もっと新しいが失敗の S1 と、成功した S1 以外のステップは対象外
		long job3 = insertJob("daily-collect", T0);
		long failed = insertStep(job3, "S1", T0);
		mapper.updateStepRun(failed, T0.plusMinutes(1), "FAILURE", 0L, null, "boom", null, null);
		long s2 = insertStep(job3, "S2", T0);
		mapper.updateStepRun(s2, T0.plusMinutes(2), "SUCCESS", 60L, null, null, null, null);

		assertThat(mapper.findLastSuccessfulS1())
			.contains(new LastS1(LocalDate.of(2026, 11, 18), LocalDate.of(2027, 5, 17)));
	}

	@Test
	void findFirstFailedStepReturnsSmallestIdFailure() {
		long jobId = insertJob("daily-collect", T0);
		// started_at はジョブの開始時刻と同じ値にする（id の順で決まること）
		long s1 = insertStep(jobId, "S1", T0);
		mapper.updateStepRun(s1, T0.plusMinutes(1), "SUCCESS", 10L, null, null, null, null);
		long s2 = insertStep(jobId, "S2", T0);
		mapper.updateStepRun(s2, T0.plusMinutes(2), "FAILURE", 0L, null, "KopisFatalException: 02", null, null);
		long s3 = insertStep(jobId, "S3", T0);
		mapper.updateStepRun(s3, T0.plusMinutes(3), "FAILURE", 0L, null, "X: y", null, null);

		assertThat(mapper.findFirstFailedStep(jobId)).contains(new FailedStep("S2", "KopisFatalException: 02"));
	}

	@Test
	void findFirstFailedStepIgnoresSuccessUnfinishedAndOtherJobs() {
		long other = insertJob("daily-collect", T0.minusDays(1));
		long otherStep = insertStep(other, "S1", T0.minusDays(1));
		mapper.updateStepRun(otherStep, T0.minusDays(1).plusMinutes(1), "FAILURE", 0L, null, "other job", null, null);

		long jobId = insertJob("daily-collect", T0);
		long s1 = insertStep(jobId, "S1", T0);
		mapper.updateStepRun(s1, T0.plusMinutes(1), "SUCCESS", 10L, null, null, null, null);
		// 始めたが終わりを記録できなかった（result が NULL）
		insertStep(jobId, "S2", T0);

		assertThat(mapper.findFirstFailedStep(jobId)).isEmpty();
	}

	@Test
	void findFirstFailedStepKeepsNullMessage() {
		long jobId = insertJob("daily-collect", T0);
		long s7 = insertStep(jobId, "S7", T0);
		mapper.updateStepRun(s7, T0.plusMinutes(1), "FAILURE", 0L, null, null, null, null);

		assertThat(mapper.findFirstFailedStep(jobId)).isEqualTo(Optional.of(new FailedStep("S7", null)));
	}

	@Test
	void updateStepRunWritesCollectBaseDate() {
		long jobId = insertJob("daily-collect", T0);
		long s1 = insertStep(jobId, "S1", T0);
		long s2 = insertStep(jobId, "S2", T0);

		mapper.updateStepRun(s1, T0.plusMinutes(3), "FAILURE", 5L, "{\"saved\":5}", "KopisCallLimitException: x", null,
				LocalDate.of(2026, 11, 17));
		mapper.updateStepRun(s2, T0.plusMinutes(4), "SUCCESS", 60L, null, null, null, null);

		assertThat(jdbcTemplate.queryForObject("SELECT collect_base_date FROM batch_step_run WHERE id = ?",
				LocalDate.class, s1))
			.isEqualTo(LocalDate.of(2026, 11, 17));
		// null なら NULL（S1 以外は NULL。CHECK chk_batch_step_run_collect_base_date）
		assertThat(jdbcTemplate.queryForObject("SELECT collect_base_date FROM batch_step_run WHERE id = ?",
				LocalDate.class, s2))
			.isNull();
	}

	@Test
	void findLastSuccessfulS1UsesTheBaseDate() {
		// 続きで終わった S1: 開始は 11-19 だが基準日は 11-17（05 の 3.3 の 12.）
		long jobId = insertJob("daily-collect", T0);
		long s1 = insertStep(jobId, "S1", T0);
		mapper.updateStepRun(s1, T0.plusHours(3), "SUCCESS", 900L, null, null, LocalDate.of(2027, 5, 18),
				LocalDate.of(2026, 11, 17));

		assertThat(mapper.findLastSuccessfulS1())
			.contains(new LastS1(LocalDate.of(2026, 11, 17), LocalDate.of(2027, 5, 18)));
	}

	@Test
	void findLastSuccessfulS1WithoutBaseDateUsesTheStartDate() {
		// V3 より前の行（基準日 NULL）は started_at の日付
		long jobId = insertJob("daily-collect", T0);
		long s1 = insertStep(jobId, "S1", T0);
		mapper.updateStepRun(s1, T0.plusMinutes(3), "SUCCESS", 10L, null, null, LocalDate.of(2027, 5, 18), null);

		assertThat(mapper.findLastSuccessfulS1())
			.contains(new LastS1(LocalDate.of(2026, 11, 19), LocalDate.of(2027, 5, 18)));
	}

	@Test
	void findLatestS1BeforeReturnsTheNewestS1OfEarlierRuns() {
		long job1 = insertJob("daily-collect", T0.minusDays(3));
		long s1a = insertStep(job1, "S1", T0.minusDays(3));
		mapper.updateStepRun(s1a, T0.minusDays(3).plusHours(1), "SUCCESS", 1L, null, null,
				LocalDate.of(2027, 5, 15), LocalDate.of(2026, 11, 16));
		long job2 = insertJob("daily-collect", T0.minusDays(2));
		long s1b = insertStep(job2, "S1", T0.minusDays(2));
		mapper.updateStepRun(s1b, T0.minusDays(2).plusHours(3), "FAILURE", 5L, null, "limit", null,
				LocalDate.of(2026, 11, 17));
		// ほかのステップの行は対象外
		long s2b = insertStep(job2, "S2", T0.minusDays(2));
		mapper.updateStepRun(s2b, T0.minusDays(2).plusHours(4), "FAILURE", 0L, null, "x", null, null);
		long job3 = insertJob("daily-collect", T0);
		// 同じ実行の行は対象外
		insertStep(job3, "S1", T0);

		assertThat(mapper.findLatestS1Before(job3))
			.contains(new PreviousS1("FAILURE", LocalDate.of(2026, 11, 17)));
		assertThat(mapper.findLatestS1Before(job2))
			.contains(new PreviousS1("SUCCESS", LocalDate.of(2026, 11, 16)));
		assertThat(mapper.findLatestS1Before(job1)).isEmpty();
	}

	@Test
	void findLatestS1BeforeReturnsAnUnfinishedRow() {
		long job1 = insertJob("daily-collect", T0.minusDays(1));
		// 終わりが記録されていない（result が NULL）
		long s1 = insertStep(job1, "S1", T0.minusDays(1));
		jdbcTemplate.update("UPDATE batch_step_run SET collect_base_date = ? WHERE id = ?", LocalDate.of(2026, 11, 15),
				s1);
		long job2 = insertJob("daily-collect", T0);

		assertThat(mapper.findLatestS1Before(job2)).contains(new PreviousS1(null, LocalDate.of(2026, 11, 15)));
	}

	@Test
	void findLatestS1BeforeIsEmptyWithoutS1() {
		long job1 = insertJob("daily-collect", T0.minusDays(1));
		long s2 = insertStep(job1, "S2", T0.minusDays(1));
		mapper.updateStepRun(s2, T0.minusDays(1).plusMinutes(1), "SUCCESS", 1L, null, null, null, null);
		long job2 = insertJob("daily-collect", T0);

		assertThat(mapper.findLatestS1Before(job2)).isEmpty();
	}

}
