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

	/** windowEndDate は成功した S1 だけ（ほかは null で NULL を書く。04 の DB-10） */
	int updateStepRun(long id, LocalDateTime finishedAt, String result, Long processedCount, String summary,
			String errorMessage, LocalDate windowEndDate);

	/** step_name = 'S1' AND result = 'SUCCESS' の最も新しい行（04 の DB-10） */
	Optional<LastS1> findLastSuccessfulS1();

	/** result = 'FAILURE' のステップのうち id が最も小さい行（始めた順。06 の 5.2） */
	Optional<FailedStep> findFirstFailedStep(long jobRunId);

}
