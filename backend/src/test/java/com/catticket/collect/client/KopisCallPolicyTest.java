package com.catticket.collect.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.catticket.common.batch.Retry;
import com.catticket.common.config.CatticketProperties;

/**
 * KopisCallPolicy（05 の 3.5）。Sleeper は待たずに記録だけする。
 */
class KopisCallPolicyTest {

	private static final Duration INTERVAL = Duration.ofMillis(500);

	private static final List<Duration> BACKOFF = List.of(Duration.ofSeconds(2), Duration.ofSeconds(4),
			Duration.ofSeconds(8));

	private final List<Duration> slept = new ArrayList<>();

	private final KopisCallPolicy policy = newPolicy(slept);

	static KopisCallPolicy newPolicy(List<Duration> slept) {
		CatticketProperties props = new CatticketProperties(null, null,
				new CatticketProperties.Kopis("http://kopis.test", "k", INTERVAL), null, null, null, null, null, null);
		return new KopisCallPolicy(new Retry(slept::add), slept::add, props);
	}

	@Test
	void waitsIntervalBetweenCalls() {
		assertThat(policy.call(() -> "a")).isEqualTo("a");
		// 最初の呼び出しの前は待たない
		assertThat(slept).isEmpty();

		policy.call(() -> "b");
		policy.call(() -> "c");

		assertThat(slept).containsExactly(INTERVAL, INTERVAL);
	}

	@Test
	void retriesRetryableWith2_4_8Seconds() {
		AtomicInteger attempts = new AtomicInteger();

		String result = policy.call(() -> {
			if (attempts.incrementAndGet() < 4) {
				throw new KopisRetryableException("timeout");
			}
			return "ok";
		});

		// 最初 + 再試行 3 回
		assertThat(result).isEqualTo("ok");
		assertThat(attempts).hasValue(4);
		assertThat(slept).containsExactlyElementsOf(BACKOFF);
	}

	@Test
	void givesUpAfterThreeRetries() {
		AtomicInteger attempts = new AtomicInteger();

		assertThatThrownBy(() -> policy.call(failing(attempts, new KopisRetryableException("HTTP 503"))))
			.isInstanceOf(KopisRetryableException.class)
			.hasMessage("HTTP 503");

		assertThat(attempts).hasValue(4);
		assertThat(slept).containsExactlyElementsOf(BACKOFF);
	}

	@Test
	void doesNotRetryFatal() {
		AtomicInteger attempts = new AtomicInteger();

		assertThatThrownBy(() -> policy.call(failing(attempts, new KopisFatalException("returncode=02"))))
			.isInstanceOf(KopisFatalException.class);

		assertThat(attempts).hasValue(1);
		assertThat(slept).isEmpty();
	}

	@Test
	void abortsOnTenthConsecutiveFailedCall() {
		AtomicInteger attempts = new AtomicInteger();
		for (int i = 0; i < 9; i++) {
			assertThatThrownBy(() -> policy.call(failing(attempts, new KopisRetryableException("timeout"))))
				.isInstanceOf(KopisRetryableException.class);
		}

		assertThatThrownBy(() -> policy.call(failing(attempts, new KopisRetryableException("timeout"))))
			.isInstanceOf(StepAbortedException.class)
			.hasMessageContaining("10 consecutive failures")
			.hasMessageContaining("timeout")
			.hasCauseInstanceOf(KopisRetryableException.class);
		// 10 回の呼び出し × (最初 + 再試行 3 回)
		assertThat(attempts).hasValue(40);

		// 止めた後は呼ばずに投げる
		assertThatThrownBy(() -> policy.call(failing(attempts, new KopisRetryableException("timeout"))))
			.isInstanceOf(StepAbortedException.class);
		assertThat(attempts).hasValue(40);
	}

	@Test
	void fatalFailuresAlsoCountAsFailedCalls() {
		AtomicInteger attempts = new AtomicInteger();
		for (int i = 0; i < 9; i++) {
			assertThatThrownBy(() -> policy.call(failing(attempts, new KopisFatalException("returncode=99"))))
				.isInstanceOf(KopisFatalException.class);
		}

		assertThatThrownBy(() -> policy.call(failing(attempts, new KopisFatalException("returncode=99"))))
			.isInstanceOf(StepAbortedException.class);
	}

	@Test
	void successResetsTheCount() {
		AtomicInteger attempts = new AtomicInteger();
		for (int i = 0; i < 9; i++) {
			assertThatThrownBy(() -> policy.call(failing(attempts, new KopisRetryableException("timeout"))))
				.isInstanceOf(KopisRetryableException.class);
		}
		policy.call(() -> "ok");

		// 続けて失敗した数は 0 から数え直す
		for (int i = 0; i < 9; i++) {
			assertThatThrownBy(() -> policy.call(failing(attempts, new KopisRetryableException("timeout"))))
				.isInstanceOf(KopisRetryableException.class);
		}
	}

	@Test
	void startStepResetsTheCount() {
		AtomicInteger attempts = new AtomicInteger();
		for (int i = 0; i < 10; i++) {
			try {
				policy.call(failing(attempts, new KopisRetryableException("timeout")));
			}
			catch (RuntimeException e) {
				// 10 回目は StepAbortedException
			}
		}
		assertThatThrownBy(() -> policy.call(() -> "x")).isInstanceOf(StepAbortedException.class);

		policy.startStep();

		assertThat(policy.call(() -> "next step")).isEqualTo("next step");
	}

	@Test
	void sleepsOnlyBeforeEachCallNotBetweenRetryAttempts() {
		policy.call(() -> "first");
		AtomicInteger attempts = new AtomicInteger();
		policy.call(() -> {
			if (attempts.incrementAndGet() == 1) {
				throw new KopisRetryableException("once");
			}
			return "ok";
		});

		// 2回目の呼び出しの前の間隔 → 再試行の 2 秒
		assertThat(slept).containsExactly(INTERVAL, Duration.ofSeconds(2));
		assertThat(Collections.frequency(slept, INTERVAL)).isEqualTo(1);
	}

	private static Supplier<String> failing(AtomicInteger attempts, RuntimeException e) {
		return () -> {
			attempts.incrementAndGet();
			throw e;
		};
	}

}
