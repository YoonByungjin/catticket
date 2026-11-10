package com.catticket.common.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.catticket.common.time.DateTimes;

/**
 * 「今日」・今の時刻の時計（03 の 7.1）。テストで Clock.fixed に替えられるよう Bean にする。
 */
@Configuration
public class ClockConfig {

	@Bean
	Clock clock() {
		return Clock.system(DateTimes.SEOUL);
	}

}
