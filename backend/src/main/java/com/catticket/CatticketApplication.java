package com.catticket;

import java.util.Arrays;
import java.util.stream.Stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import com.catticket.common.batch.TaskRunner;

// CatticketProperties（catticket.*）を読み込むため
@SpringBootApplication
@ConfigurationPropertiesScan
public class CatticketApplication {

	static final String TASK_ARG_PREFIX = "--catticket.task=";

	/**
	 * 手での実行でスケジューラーを止める引数。既定のプロパティ（setDefaultProperties）は application.yml の
	 * {@code catticket.scheduler.enabled: true} より優先度が低く効かないので、コマンドの引数で渡す
	 */
	static final String SCHEDULER_OFF_ARG = "--catticket.scheduler.enabled=false";

	public static void main(String[] args) {
		if (isTaskRun(args)) {
			System.exit(runTask(args));
		}
		SpringApplication.run(CatticketApplication.class, args);
	}

	/** 手での実行（06 の 5.1）: Web サーバーを起動せず、ジョブの終了コードを返す */
	static int runTask(String[] args) {
		// bootRun の devtools は main を別のスレッドで呼び直し、起動の失敗でも終了コード 0 で終わるので使わない
		System.setProperty("spring.devtools.restart.enabled", "false");
		SpringApplication app = new SpringApplication(CatticketApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE);
		try {
			return SpringApplication.exit(app.run(taskArgs(args)));
		}
		catch (RuntimeException e) {
			// 起動の失敗（設定の誤りなど）。内容は Spring Boot がログに出している
			return TaskRunner.EXIT_FAILURE;
		}
	}

	static boolean isTaskRun(String[] args) {
		return Arrays.stream(args).anyMatch(a -> a.startsWith(TASK_ARG_PREFIX));
	}

	static String[] taskArgs(String[] args) {
		if (Arrays.asList(args).contains(SCHEDULER_OFF_ARG)) {
			return args.clone();
		}
		return Stream.concat(Arrays.stream(args), Stream.of(SCHEDULER_OFF_ARG)).toArray(String[]::new);
	}

}
