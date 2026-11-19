package com.catticket.common.batch;

import java.time.LocalDateTime;

/**
 * batch_job_run の INSERT の引数。生成された id を MyBatis が書き戻すので record にできない（可変）。
 */
public final class JobRunInsert {

	private final String jobName;

	private final LocalDateTime startedAt;

	private Long id;

	public JobRunInsert(String jobName, LocalDateTime startedAt) {
		this.jobName = jobName;
		this.startedAt = startedAt;
	}

	public String getJobName() {
		return jobName;
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
