package com.catticket.collect.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.catticket.common.batch.Retry;
import com.catticket.common.config.CatticketProperties;
import com.catticket.common.config.CatticketProperties.Kopis.CallLimit;

/**
 * KopisCallPolicy（05 の 3.5、06 の 4.9 の「呼び出しの方針のテスト」）。Retry は本物、Sleeper は待たずに記録だけする。
 */
class KopisCallPolicyTest {

	private static final Duration INTERVAL = Duration.ofSeconds(7);

	private static final Duration WAIT_30 = Duration.ofSeconds(30);

	private static final Duration WAIT_60 = Duration.ofSeconds(60);

	/** 本番の値（05 の 3.5） */
	static final CallLimit PROD = new CallLimit(300, 70, 10, 30, 5);

	/** 開発の値（05 の 3.5） */
	static final CallLimit DEV = new CallLimit(20, 4, 2, 2, 1);

	private final List<Duration> slept = new ArrayList<>();

	private KopisCallPolicy policy;

	static KopisCallPolicy newPolicy(List<Duration> slept, Duration interval, CallLimit limit) {
		CatticketProperties props = new CatticketProperties(null, null,
				new CatticketProperties.Kopis("http://kopis.test", "k", interval, limit), null, null, null, null,
				null, null);
		return new KopisCallPolicy(new Retry(slept::add), slept::add, props);
	}

	@BeforeEach
	void setUp() {
		policy = newPolicy(slept, INTERVAL, PROD);
		policy.startJob();
		policy.startStep("S1");
	}

	private static Supplier<String> failing(AtomicInteger attempts, RuntimeException e) {
		return () -> {
			attempts.incrementAndGet();
			throw e;
		};
	}

	private void failOnce(AtomicInteger attempts) {
		assertThatThrownBy(() -> policy.call(failing(attempts, new KopisFatalException("returncode=01"))))
			.isInstanceOf(KopisFatalException.class);
	}

	@Nested
	class Interval {

		@Test
		void firstSendDoesNotWaitAndEachLaterSendWaits7Seconds() {
			assertThat(policy.call(() -> "a")).isEqualTo("a");
			// 起動して最初の送信は待たない
			assertThat(slept).isEmpty();

			policy.call(() -> "b");
			policy.call(() -> "c");

			assertThat(slept).containsExactly(INTERVAL, INTERVAL);
		}

		@Test
		void intervalIsKeptAcrossJobs() {
			policy.call(() -> "a");
			policy.startJob();
			policy.startStep("S1");

			policy.call(() -> "b");

			// 間隔はアプリの中の前の送信からなので、実行が変わっても待つ
			assertThat(slept).containsExactly(INTERVAL);
		}

	}

	@Nested
	class Retries {

		@Test
		void retryableIsSentAtMostThreeTimesWaiting30And60Seconds() {
			AtomicInteger attempts = new AtomicInteger();

			assertThatThrownBy(() -> policy.call(failing(attempts, new KopisRetryableException("HTTP 503"))))
				.isInstanceOf(KopisRetryableException.class)
				.hasMessage("HTTP 503");

			assertThat(attempts).hasValue(3);
			assertThat(policy.stepCalls()).isEqualTo(3);
			assertThat(policy.runCalls()).isEqualTo(3);
			assertThat(slept).containsExactly(WAIT_30, WAIT_60);
		}

		@Test
		void retryDoesNotAddTheIntervalBeforeRetrySends() {
			policy.call(() -> "first");
			AtomicInteger attempts = new AtomicInteger();

			String result = policy.call(() -> {
				if (attempts.incrementAndGet() == 1) {
					throw new KopisRetryableException("once");
				}
				return "ok";
			});

			assertThat(result).isEqualTo("ok");
			// 2回目の呼び出しの前の間隔 → 再試行の 30 秒（7 秒は足さない）
			assertThat(slept).containsExactly(INTERVAL, WAIT_30);
		}

		@Test
		void fatalIsNotRetried() {
			AtomicInteger attempts = new AtomicInteger();

			failOnce(attempts);

			assertThat(attempts).hasValue(1);
			assertThat(slept).isEmpty();
		}

	}

	@Nested
	class Blocked {

		@Test
		void blockedIsSentOnceAndStopsTheRun() {
			AtomicInteger attempts = new AtomicInteger();
			KopisBlockedException blocked = new KopisBlockedException(
					"KOPIS pblprfr blocked: HTTP 400 (Request Blocked)");

			KopisStoppedException e = catchThrowableOfType(KopisStoppedException.class,
					() -> policy.call(failing(attempts, blocked)));

			assertThat(e.reason()).isEqualTo(KopisStopReason.BLOCKED);
			assertThat(e).hasMessage("KOPIS blocked: KOPIS pblprfr blocked: HTTP 400 (Request Blocked)");
			assertThat(e.getCause()).isSameAs(blocked);
			// 再試行しない
			assertThat(attempts).hasValue(1);
			assertThat(slept).isEmpty();
			assertThat(policy.stopped()).isTrue();
			assertThat(policy.stopMessage())
				.isEqualTo("S1: KOPIS blocked: KOPIS pblprfr blocked: HTTP 400 (Request Blocked)");
		}

		@Test
		void afterBlockedNoCallIsSentEvenInLaterStepsUntilStartJob() {
			AtomicInteger attempts = new AtomicInteger();
			assertThatThrownBy(() -> policy.call(failing(attempts, new KopisBlockedException("blocked"))))
				.isInstanceOf(KopisStoppedException.class);
			slept.clear();

			AtomicInteger later = new AtomicInteger();
			KopisStoppedException e = catchThrowableOfType(KopisStoppedException.class,
					() -> policy.call(failing(later, new IllegalStateException("must not be called"))));
			assertThat(e.reason()).isEqualTo(KopisStopReason.BLOCKED);
			assertThat(e).hasMessage("KOPIS calls stopped in this run: S1: KOPIS blocked: blocked");

			// ステップの初めには戻さない
			policy.startStep("S3");
			assertThatThrownBy(() -> policy.call(failing(later, new IllegalStateException("must not be called"))))
				.isInstanceOf(KopisStoppedException.class);
			assertThat(later).hasValue(0);
			// 送らないので待たない
			assertThat(slept).isEmpty();
			assertThat(policy.stopped()).isTrue();

			// 次の実行は通常どおり呼ぶ
			policy.startJob();
			policy.startStep("S1");
			assertThat(policy.stopped()).isFalse();
			assertThat(policy.stopMessage()).isNull();
			assertThat(policy.call(() -> "next run")).isEqualTo("next run");
		}

	}

	@Nested
	class ConsecutiveFailures {

		@Test
		void successAfterFourFailuresResetsTheCount() {
			AtomicInteger attempts = new AtomicInteger();
			for (int i = 0; i < 4; i++) {
				failOnce(attempts);
			}
			policy.call(() -> "ok");

			for (int i = 0; i < 4; i++) {
				failOnce(attempts);
			}
			assertThat(policy.stopped()).isFalse();
		}

		@Test
		void fifthFailureAcrossStepsStopsTheRun() {
			policy.startStep("S2");
			AtomicInteger attempts = new AtomicInteger();
			for (int i = 0; i < 4; i++) {
				// 4 回目までは元の例外がそのまま出る
				failOnce(attempts);
			}
			policy.startStep("S3");
			KopisFatalException last = new KopisFatalException("returncode=02");

			KopisStoppedException e = catchThrowableOfType(KopisStoppedException.class,
					() -> policy.call(failing(attempts, last)));

			assertThat(e.reason()).isEqualTo(KopisStopReason.CONSECUTIVE_FAILURES);
			assertThat(e).hasMessage("KOPIS calls stopped after 5 consecutive failures; last: returncode=02");
			assertThat(e.getCause()).isSameAs(last);
			assertThat(policy.stopped()).isTrue();
			assertThat(policy.stopMessage()).startsWith("S3: KOPIS calls stopped after 5 consecutive failures");
			assertThatThrownBy(() -> policy.call(() -> "x")).isInstanceOf(KopisStoppedException.class);
		}

		@Test
		void retryableFailuresCountAfterTheirRetries() {
			AtomicInteger attempts = new AtomicInteger();
			for (int i = 0; i < 4; i++) {
				assertThatThrownBy(() -> policy.call(failing(attempts, new KopisRetryableException("timeout"))))
					.isInstanceOf(KopisRetryableException.class);
			}
			assertThatThrownBy(() -> policy.call(failing(attempts, new KopisRetryableException("timeout"))))
				.isInstanceOf(KopisStoppedException.class)
				.hasCauseInstanceOf(KopisRetryableException.class);

			// 5 回 × 最大 3 回の送信
			assertThat(attempts).hasValue(15);
		}

		@Test
		void startJobResetsTheCount() {
			AtomicInteger attempts = new AtomicInteger();
			for (int i = 0; i < 4; i++) {
				failOnce(attempts);
			}
			policy.startJob();
			policy.startStep("S1");

			failOnce(attempts);
			assertThat(policy.stopped()).isFalse();
		}

	}

	@Nested
	class Allowance {

		@Test
		void productionValuesReserveTheLaterSteps() {
			assertThat(policy.allowance()).isEqualTo(185);
			for (int i = 0; i < 185; i++) {
				policy.call(() -> "x");
			}

			policy.startStep("S2");
			assertThat(policy.allowance()).isEqualTo(70);
			for (int i = 0; i < 70; i++) {
				policy.call(() -> "x");
			}
			policy.startStep("S3");
			assertThat(policy.allowance()).isEqualTo(10);
			for (int i = 0; i < 10; i++) {
				policy.call(() -> "x");
			}
			policy.startStep("S4");
			assertThat(policy.allowance()).isEqualTo(30);
			for (int i = 0; i < 30; i++) {
				policy.call(() -> "x");
			}
			policy.startStep("S6");
			assertThat(policy.allowance()).isEqualTo(5);
		}

		@Test
		void unusedCallsAreNotPassedOn() {
			for (int i = 0; i < 100; i++) {
				policy.call(() -> "x");
			}

			policy.startStep("S2");

			assertThat(policy.allowance()).isEqualTo(70);
		}

		@Test
		void developmentValuesGiveS1Eleven() {
			KopisCallPolicy dev = newPolicy(slept, INTERVAL, DEV);
			dev.startJob();
			dev.startStep("S1");

			assertThat(dev.allowance()).isEqualTo(11);
			assertThat(dev.perRun()).isEqualTo(20);
		}

	}

	@Nested
	class Limit {

		private KopisCallPolicy dev;

		@BeforeEach
		void setUp() {
			dev = newPolicy(slept, INTERVAL, DEV);
			dev.startJob();
			dev.startStep("S2");
		}

		@Test
		void callAfterTheAllowanceIsNotSent() {
			AtomicInteger attempts = new AtomicInteger();
			for (int i = 0; i < 3; i++) {
				assertThatThrownBy(() -> dev.call(failing(attempts, new KopisFatalException("x"))))
					.isInstanceOf(KopisFatalException.class);
			}
			dev.call(() -> "4th");
			slept.clear();

			assertThatThrownBy(() -> dev.call(failing(attempts, new IllegalStateException("must not be sent"))))
				.isInstanceOf(KopisCallLimitException.class)
				.hasMessage("KOPIS call limit reached in S2: 4/4 (run 4/20)");
			assertThat(attempts).hasValue(3);
			// 送らないので間隔も待たない
			assertThat(slept).isEmpty();
			assertThat(dev.allowance()).isZero();
			assertThat(dev.stopped()).isFalse();
		}

		@Test
		void limitDoesNotCountAsFailure() {
			AtomicInteger attempts = new AtomicInteger();
			dev.call(() -> "1");
			for (int i = 0; i < 3; i++) {
				assertThatThrownBy(() -> dev.call(failing(attempts, new KopisFatalException("x"))))
					.isInstanceOf(KopisFatalException.class);
			}
			for (int i = 0; i < 3; i++) {
				assertThatThrownBy(() -> dev.call(() -> "x")).isInstanceOf(KopisCallLimitException.class);
			}

			dev.startStep("S3");
			// 続けての失敗は 3 のまま。4 回目は元の例外、5 回目で止まる
			assertThatThrownBy(() -> dev.call(failing(attempts, new KopisFatalException("x"))))
				.isInstanceOf(KopisFatalException.class);
			assertThatThrownBy(() -> dev.call(failing(attempts, new KopisFatalException("x"))))
				.isInstanceOf(KopisStoppedException.class);
		}

		@Test
		void retryHittingTheAllowanceIsNotSent() {
			dev.call(() -> "1");
			dev.call(() -> "2");
			AtomicInteger attempts = new AtomicInteger();

			// 3 回目の送信は割り当ての 4 回目、再試行の 1 回目が最後、2 回目は送らない
			assertThatThrownBy(() -> dev.call(failing(attempts, new KopisRetryableException("timeout"))))
				.isInstanceOf(KopisCallLimitException.class)
				.hasMessage("KOPIS call limit reached in S2: 4/4 (run 4/20)");
			assertThat(attempts).hasValue(2);
			assertThat(dev.stepCalls()).isEqualTo(4);
		}

		@Test
		void nextStepCallsWithItsOwnAllowance() {
			for (int i = 0; i < 4; i++) {
				dev.call(() -> "x");
			}
			assertThatThrownBy(() -> dev.call(() -> "x")).isInstanceOf(KopisCallLimitException.class);

			dev.startStep("S3");

			assertThat(dev.allowance()).isEqualTo(2);
			assertThat(dev.call(() -> "S3")).isEqualTo("S3");
			assertThat(dev.stepCalls()).isEqualTo(1);
			assertThat(dev.runCalls()).isEqualTo(5);
		}

	}

	@Test
	void runTotalNeverExceedsPerRun() {
		for (CallLimit limit : List.of(PROD, DEV)) {
			KopisCallPolicy p = newPolicy(slept, INTERVAL, limit);
			p.startJob();
			for (String step : KopisCallPolicy.STEPS) {
				p.startStep(step);
				// 割り当てまで使い、さらに呼ぼうとする
				while (true) {
					try {
						p.call(() -> "x");
					}
					catch (KopisCallLimitException e) {
						break;
					}
				}
			}
			assertThat(p.runCalls()).isEqualTo(limit.perRun());
		}
	}

	@Test
	void misconfigurationFailsAtStartup() {
		assertThatThrownBy(() -> newPolicy(slept, INTERVAL, new CallLimit(0, 4, 2, 2, 1)))
			.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> newPolicy(slept, INTERVAL, new CallLimit(20, -1, 2, 2, 1)))
			.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> newPolicy(slept, INTERVAL, new CallLimit(20, 4, 2, 2, -1)))
			.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> newPolicy(slept, null, DEV)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> newPolicy(slept, Duration.ofSeconds(-1), DEV))
			.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> newPolicy(slept, INTERVAL, null)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void unknownStepNameIsRejected() {
		assertThatThrownBy(() -> policy.startStep("S5")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> policy.startStep("S7")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void callOutsideAStepIsNotSent() {
		KopisCallPolicy fresh = newPolicy(slept, INTERVAL, DEV);
		AtomicInteger attempts = new AtomicInteger();

		// 割り当てが決まる前（startStep の前）は 0 回
		assertThatThrownBy(() -> fresh.call(failing(attempts, new IllegalStateException("must not be sent"))))
			.isInstanceOf(KopisCallLimitException.class);
		assertThat(attempts).hasValue(0);
	}

}
