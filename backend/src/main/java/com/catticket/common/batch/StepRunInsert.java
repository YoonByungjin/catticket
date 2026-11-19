package com.catticket.common.batch;

import java.time.LocalDateTime;

/**
 * batch_step_run の INSERT の引数。生成された id を MyBatis が書き戻すので record にできない（可変）。
 */
public final class StepRunInsert {

	private final long jobRunId;

	private final String stepName;

	private final LocalDateTime startedAt;

	private Long id;

	public StepRunInsert(long jobRunId, String stepName, LocalDateTime startedAt) {
		this.jobRunId = jobRunId;
		this.stepName = stepName;
		this.startedAt = startedAt;
	}

	public long getJobRunId() {
		return jobRunId;
	}

	public String getStepName() {
		return stepName;
	}

	public LocalDateTime getStartedAt() {
		return startedAt;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

}
