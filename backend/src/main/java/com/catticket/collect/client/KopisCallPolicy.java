package com.catticket.collect.client;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.catticket.common.batch.Retry;
import com.catticket.common.batch.Sleeper;
import com.catticket.common.config.CatticketProperties;

/**
 * KOPIS の呼び出しの制限への対応（05 の 3.5）。呼び出しの間隔・再試行・続けて失敗したときの停止。
 * KopisClient のすべての呼び出しがここを通る。ステップは始めに {@link #startStep()} を呼ぶ。
 */
@Component
public class KopisCallPolicy {

	private static final Logger log = LoggerFactory.getLogger(KopisCallPolicy.class);

	/** 再試行の前の待ち（最大 3 回） */
	static final List<Duration> RETRY_WAITS = List.of(Duration.ofSeconds(2), Duration.ofSeconds(4),
			Duration.ofSeconds(8));

	/** 同じステップで、再試行の後も失敗した呼び出しがこの回数続いたら止める */
	static final int MAX_CONSECUTIVE_FAILURES = 10;

	private final Retry retry;

	private final Sleeper sleeper;

	private final Duration interval;

	// バッチは1つずつ順に動くが、同時に複数を呼ばないことを確かにするため synchronized で守る
	private boolean called;

	private int consecutiveFailures;

	public KopisCallPolicy(Retry retry, Sleeper sleeper, CatticketProperties props) {
		this.retry = retry;
		this.sleeper = sleeper;
		this.interval = Objects.requireNonNull(props.kopis().interval(), "catticket.kopis.interval");
	}

	/** ステップの始めに、続けて失敗した数を 0 に戻す（数えるのはステップごと） */
	public synchronized void startStep() {
		consecutiveFailures = 0;
	}

	/**
	 * 1回の呼び出し。前の呼び出しとの間を {@code interval} あけ、{@link KopisRetryableException} は 2・4・8 秒あけて最大 3 回やり直す。
	 *
	 * @throws StepAbortedException 再試行の後も失敗した呼び出しが、このステップで続けて 10 回になった（それ以降も呼ばずに投げる）
	 */
	public synchronized <T> T call(Supplier<T> attempt) {
		if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
			throw new StepAbortedException(
					"KOPIS calls aborted: " + consecutiveFailures + " consecutive failures in this step");
		}
		if (called) {
			sleeper.sleep(interval);
		}
		called = true;
		try {
			T result = retry.call(attempt, RETRY_WAITS, e -> e instanceof KopisRetryableException);
			consecutiveFailures = 0;
			return result;
		}
		catch (RuntimeException e) {
			consecutiveFailures++;
			if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
				log.error("KOPIS: {} consecutive failed calls, aborting the step", consecutiveFailures);
				throw new StepAbortedException("KOPIS calls aborted after " + consecutiveFailures
						+ " consecutive failures; last: " + e.getMessage(), e);
			}
			throw e;
		}
	}

}
