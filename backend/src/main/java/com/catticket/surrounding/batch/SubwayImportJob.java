package com.catticket.surrounding.batch;

import org.springframework.stereotype.Component;

import com.catticket.common.batch.BatchJob;
import com.catticket.common.batch.BatchJobRunner;
import com.catticket.common.batch.JobResult;
import com.catticket.common.batch.TaskName;

/**
 * B-02 {@code import-subway}（05 の 4、06 の 5.4）。運営者が手で実行する。結果は SUCCESS か FAILURE。
 */
@Component
public class SubwayImportJob implements BatchJob {

	private final BatchJobRunner runner;

	private final SubwayImportService subwayImportService;

	public SubwayImportJob(BatchJobRunner runner, SubwayImportService subwayImportService) {
		this.runner = runner;
		this.subwayImportService = subwayImportService;
	}

	@Override
	public TaskName name() {
		return TaskName.IMPORT_SUBWAY;
	}

	@Override
	public JobResult run() {
		return runner.execute(TaskName.IMPORT_SUBWAY, subwayImportService::importAll);
	}

}
