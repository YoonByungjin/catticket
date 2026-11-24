package com.catticket.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定期実行を有効にする（06 の 5.1）。何を動かすかは common.batch.ScheduledTasks が決め、
 * catticket.scheduler.enabled が true のときだけ登録される（テスト・手での実行では動かない）。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

}
