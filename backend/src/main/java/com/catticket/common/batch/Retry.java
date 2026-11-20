package com.catticket.common.batch;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

/**
 * 再試行（06 の 5.2。05 の 3.5・3.7）。waits の数だけ再試行する（最大の呼び出し回数 = waits の数 + 1）。
 */
@Component
public class Retry {

	private final Sleeper sleeper;

	public Retry(Sleeper sleeper) {
		this.sleeper = sleeper;
	}

	/**
	 * @param waits     再試行の前に待つ時間（例: 2・4・8 秒）
	 * @param retryable 再試行してよい例外か。偽ならすぐ投げ直す
	 */
	public <T> T call(Supplier<T> action, List<Duration> waits, Predicate<Exception> retryable) {
		int attempt = 0;
		while (true) {
			try {
				return action.get();
			}
			catch (RuntimeException e) {
				if (attempt >= waits.size() || !retryable.test(e)) {
					throw e;
				}
				sleeper.sleep(waits.get(attempt));
				attempt++;
			}
		}
	}

}
