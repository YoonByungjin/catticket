package com.catticket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 起動の切り替え（06 の 5.1）。--catticket.task= があれば手での実行で、スケジューラーを止める引数を足す。
 */
class CatticketApplicationMainTest {

	@Test
	void taskArgumentSwitchesToTaskRun() {
		assertThat(CatticketApplication.isTaskRun(new String[] { "--catticket.task=import-subway" })).isTrue();
		assertThat(CatticketApplication.isTaskRun(new String[] { "--server.port=8081" })).isFalse();
		assertThat(CatticketApplication.isTaskRun(new String[0])).isFalse();
	}

	@Test
	void taskArgsDisableSchedulerOnce() {
		String[] once = CatticketApplication.taskArgs(new String[] { "--catticket.task=import-subway" });
		assertThat(once).containsExactly("--catticket.task=import-subway", "--catticket.scheduler.enabled=false");
		// 2回通しても重ならない（値が "false,false" にならない）
		assertThat(CatticketApplication.taskArgs(once)).containsExactly(once);
	}

	@Test
	void unknownTaskStartsWithoutWebServerAndReturns64() {
		// 実際に起動する（テスト用 DB に接続）。知らない名前なのでジョブは動かない
		assertThat(CatticketApplication.runTask(new String[] { "--catticket.task=no-such-task" })).isEqualTo(64);
	}

}
