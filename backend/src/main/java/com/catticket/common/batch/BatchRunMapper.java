package com.catticket.common.batch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;

/**
 * 実行の記録（mapper/common/BatchRunMapper.xml。04 の batch_job_run・batch_step_run）。
 */
@Mapper
public interface BatchRunMapper {

	/** 生成された id を row に書き戻す */
	int insertJobRun(JobRunInsert row);

	int updateJobRun(long id, LocalDateTime finishedAt, String result, String errorMessage);

	/** 生成された id を row に書き戻す */
	int insertStepRun(StepRunInsert row);

	/**
	 * windowEndDate は成功した S1 だけ、collectBaseDate は S1 だけ（ほかは null で NULL を書く。04 の DB-10・11.11）
	 */
	int updateStepRun(long id, LocalDateTime finishedAt, String result, Long processedCount, String summary,
			String errorMessage, LocalDate windowEndDate, LocalDate collectBaseDate);

	/** step_name = 'S1' AND result = 'SUCCESS' の最も新しい行。成功した日は基準日（04 の DB-10） */
	Optional<LastS1> findLastSuccessfulS1();

	/** その実行より前の実行の S1 の最も新しい行（id の降順。06 の 5.2。05 の 3.3 の 12.） */
	Optional<PreviousS1> findLatestS1Before(long jobRunId);

	/** result = 'FAILURE' のステップのうち id が最も小さい行（始めた順。06 の 5.2） */
	Optional<FailedStep> findFirstFailedStep(long jobRunId);

}
