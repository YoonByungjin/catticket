package com.catticket.common.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

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

		mapper.updateStepRun(stepId, T0.plusSeconds(5), "SUCCESS", 574L, "{\"stationsSaved\":261}", null);

		Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM batch_step_run WHERE id = ?", stepId);
		assertThat(((Number) row.get("job_run_id")).longValue()).isEqualTo(jobId);
		assertThat(row.get("step_name")).isEqualTo("import-subway");
		assertThat(row.get("result")).isEqualTo("SUCCESS");
		assertThat(((Number) row.get("processed_count")).longValue()).isEqualTo(574L);
		assertThat(row.get("summary")).isEqualTo("{\"stationsSaved\":261}");
		assertThat(row.get("error_message")).isNull();
		assertThat(row.get("finished_at")).isEqualTo(T0.plusSeconds(5));
	}

	@Test
	void findLastSuccessfulS1IsEmptyWithoutSuccess() {
		long jobId = insertJob("daily-collect", T0);
		long s1 = insertStep(jobId, "S1", T0);
		mapper.updateStepRun(s1, T0.plusMinutes(1), "FAILURE", 0L, null, "boom");

		assertThat(mapper.findLastSuccessfulS1()).isEmpty();
	}

	@Test
	void findLastSuccessfulS1ReturnsLatestSuccessfulS1() {
		// 古い成功
		long job1 = insertJob("daily-collect", T0.minusDays(2));
		long old = insertStep(job1, "S1", T0.minusDays(2));
		mapper.updateStepRun(old, T0.minusDays(2).plusMinutes(1), "SUCCESS", 10L, null, null);
		jdbcTemplate.update("UPDATE batch_step_run SET window_end_date = ? WHERE id = ?",
				LocalDate.of(2027, 5, 16), old);
		// 新しい成功（ここを返す）
		long job2 = insertJob("daily-collect", T0.minusDays(1));
		long latest = insertStep(job2, "S1", T0.minusDays(1));
		mapper.updateStepRun(latest, T0.minusDays(1).plusMinutes(1), "SUCCESS", 10L, null, null);
		jdbcTemplate.update("UPDATE batch_step_run SET window_end_date = ? WHERE id = ?",
				LocalDate.of(2027, 5, 17), latest);
		// もっと新しいが失敗の S1 と、成功した S1 以外のステップは対象外
		long job3 = insertJob("daily-collect", T0);
		long failed = insertStep(job3, "S1", T0);
		mapper.updateStepRun(failed, T0.plusMinutes(1), "FAILURE", 0L, null, "boom");
		long s2 = insertStep(job3, "S2", T0);
		mapper.updateStepRun(s2, T0.plusMinutes(2), "SUCCESS", 60L, null, null);

		assertThat(mapper.findLastSuccessfulS1())
			.contains(new LastS1(LocalDate.of(2026, 11, 18), LocalDate.of(2027, 5, 17)));
	}

}
