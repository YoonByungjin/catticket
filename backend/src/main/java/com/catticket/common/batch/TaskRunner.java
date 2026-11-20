package com.catticket.common.batch;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.catticket.common.config.CatticketProperties;

/**
 * 手での実行（05 の 1.1、06 の 5.1）。{@code --catticket.task=} の名前のジョブを実行し、終了コードを返す。
 */
@Component
@ConditionalOnProperty("catticket.task")
public class TaskRunner implements ApplicationRunner, ExitCodeGenerator {

	public static final int EXIT_SUCCESS = 0;

	public static final int EXIT_FAILURE = 1;

	public static final int EXIT_PARTIAL_FAILURE = 2;

	public static final int EXIT_LOCKED = 3;

	/** 知らない名前（まだ作っていないバッチの名前も） */
	public static final int EXIT_UNKNOWN_TASK = 64;

	private static final Logger log = LoggerFactory.getLogger(TaskRunner.class);

	private final CatticketProperties props;

	private final List<BatchJob> jobs;

	private int exitCode = EXIT_FAILURE;

	public TaskRunner(CatticketProperties props, List<BatchJob> jobs) {
		this.props = props;
		this.jobs = jobs;
	}

	@Override
	public void run(ApplicationArguments args) {
		exitCode = runTask(props.task());
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

	int runTask(String name) {
		Optional<TaskName> task = TaskName.fromValue(name == null ? null : name.strip());
		Optional<BatchJob> job = task.flatMap(t -> jobs.stream().filter(j -> j.name() == t).findFirst());
		if (job.isEmpty()) {
			log.error("Unknown task: '{}'", name);
			return EXIT_UNKNOWN_TASK;
		}
		try {
			int code = exitCode(job.get().run());
			log.info("Task {} exit code {}", name, code);
			return code;
		}
		catch (RuntimeException e) {
			// ロック・記録の DB に届かないときなど
			log.error("Task {} failed: {}", name, BatchRunRecorder.describe(e), e);
			return EXIT_FAILURE;
		}
	}

	static int exitCode(JobResult result) {
		return switch (result) {
			case SUCCESS -> EXIT_SUCCESS;
			case FAILURE -> EXIT_FAILURE;
			case PARTIAL_FAILURE -> EXIT_PARTIAL_FAILURE;
			case LOCKED -> EXIT_LOCKED;
		};
	}

}
