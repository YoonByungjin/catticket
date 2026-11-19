package com.catticket.common.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Retry（Sleeper は待たずに記録だけする）。
 */
class RetryTest {

	private static final List<Duration> WAITS = List.of(Duration.ofSeconds(2), Duration.ofSeconds(4),
			Duration.ofSeconds(8));

	private final List<Duration> slept = new ArrayList<>();

	private final Retry retry = new Retry(slept::add);

	@Test
	void returnsImmediatelyOnSuccess() {
		assertThat(retry.call(() -> "ok", WAITS, e -> true)).isEqualTo("ok");
		assertThat(slept).isEmpty();
	}

	@Test
	void retriesWithWaitsUntilSuccess() {
		AtomicInteger calls = new AtomicInteger();

		String result = retry.call(() -> {
			if (calls.incrementAndGet() < 3) {
				throw new IllegalStateException("temporary");
			}
			return "ok";
		}, WAITS, e -> true);

		assertThat(result).isEqualTo("ok");
		assertThat(calls).hasValue(3);
		assertThat(slept).containsExactly(Duration.ofSeconds(2), Duration.ofSeconds(4));
	}

	@Test
	void givesUpAfterAllWaits() {
		AtomicInteger calls = new AtomicInteger();

		assertThatThrownBy(() -> retry.call(() -> {
			calls.incrementAndGet();
			throw new IllegalStateException("always");
		}, WAITS, e -> true)).isInstanceOf(IllegalStateException.class).hasMessage("always");

		// 最初の1回 + 再試行 3 回
		assertThat(calls).hasValue(4);
		assertThat(slept).containsExactlyElementsOf(WAITS);
	}

	@Test
	void doesNotRetryNonRetryable() {
		AtomicInteger calls = new AtomicInteger();

		assertThatThrownBy(() -> retry.call(() -> {
			calls.incrementAndGet();
			throw new IllegalArgumentException("bad request");
		}, WAITS, e -> !(e instanceof IllegalArgumentException))).isInstanceOf(IllegalArgumentException.class);

		assertThat(calls).hasValue(1);
		assertThat(slept).isEmpty();
	}

}
