package com.catticket.common.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import com.catticket.common.config.CatticketProperties;

/**
 * TaskRunner の終了コード（06 の 5.1: 0 成功、1 失敗、2 一部失敗、3 ロック、64 知らない名前）。
 */
class TaskRunnerTest {

	private static BatchJob job(TaskName name, JobResult result) {
		return new BatchJob() {
			@Override
			public TaskName name() {
				return name;
			}

			@Override
			public JobResult run() {
				return result;
			}
		};
	}

	private static TaskRunner runner(String task, BatchJob... jobs) {
		CatticketProperties props = new CatticketProperties(null, null, null, null, null, null, null, null, task);
		return new TaskRunner(props, List.of(jobs));
	}

	@Test
	void exitCodesFollowJobResult() {
		assertThat(runner(null).runTask("import-subway")).isEqualTo(64);
		assertThat(runner(null, job(TaskName.IMPORT_SUBWAY, JobResult.SUCCESS)).runTask("import-subway"))
			.isZero();
		assertThat(runner(null, job(TaskName.IMPORT_SUBWAY, JobResult.FAILURE)).runTask("import-subway"))
			.isEqualTo(1);
		assertThat(runner(null, job(TaskName.IMPORT_SUBWAY, JobResult.PARTIAL_FAILURE)).runTask("import-subway"))
			.isEqualTo(2);
		assertThat(runner(null, job(TaskName.IMPORT_SUBWAY, JobResult.LOCKED)).runTask("import-subway"))
			.isEqualTo(3);
	}

	@Test
	void unknownNameIs64() {
		TaskRunner runner = runner(null, job(TaskName.IMPORT_SUBWAY, JobResult.SUCCESS));
		assertThat(runner.runTask("no-such-task")).isEqualTo(64);
		assertThat(runner.runTask("")).isEqualTo(64);
		assertThat(runner.runTask(null)).isEqualTo(64);
		// 大文字・enum の定数名では受け付けない
		assertThat(runner.runTask("IMPORT_SUBWAY")).isEqualTo(64);
	}

	@Test
	void knownButNotImplementedTaskIs64() {
		// daily-collect（B-01）などはまだジョブがない
		TaskRunner runner = runner(null, job(TaskName.IMPORT_SUBWAY, JobResult.SUCCESS));
		assertThat(runner.runTask("daily-collect")).isEqualTo(64);
		assertThat(runner.runTask("create-admin")).isEqualTo(64);
		assertThat(runner.runTask("purge-expired-signups")).isEqualTo(64);
	}

	@Test
	void exceptionFromJobIs1() {
		BatchJob failing = new BatchJob() {
			@Override
			public TaskName name() {
				return TaskName.IMPORT_SUBWAY;
			}

			@Override
			public JobResult run() {
				throw new IllegalStateException("db down");
			}
		};
		assertThat(runner(null, failing).runTask("import-subway")).isEqualTo(1);
	}

	@Test
	void runUsesCatticketTaskPropertyAndExposesExitCode() {
		TaskRunner runner = runner("import-subway", job(TaskName.IMPORT_SUBWAY, JobResult.PARTIAL_FAILURE));
		runner.run(new DefaultApplicationArguments("--catticket.task=import-subway"));
		assertThat(runner.getExitCode()).isEqualTo(2);
	}

}
