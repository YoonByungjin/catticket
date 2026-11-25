package com.catticket.collect.client;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.catticket.common.batch.Retry;
import com.catticket.common.batch.Sleeper;
import com.catticket.common.config.CatticketProperties;
import com.catticket.common.config.CatticketProperties.Kopis.CallLimit;

/**
 * KOPIS の呼び出しの方針（05 の 3.5、06 の 4.9）。KopisClient のすべての呼び出しがここを通る。
 * 送信ごとの間隔、再試行、遮断の印・続けての失敗で実行の全体を止めること、1回の実行の上限を持つ。
 * <p>
 * 状態は実行（ジョブ）ごと。KOPIS を呼ぶのは B-01 だけで、JobLock で同時に1つしか動かず、ステップは順に動くので singleton に持つ。
 * 止めた印・続けての失敗の数は {@link #startJob()} だけで戻す（ステップをまたいで数える）。
 */
@Component
public class KopisCallPolicy {

	private static final Logger log = LoggerFactory.getLogger(KopisCallPolicy.class);

	/** 再試行の前の待ち（最大 2 回）。どちらも間隔の 7 秒より長いので、再試行の前に間隔を足さない */
	static final List<Duration> RETRY_WAITS = List.of(Duration.ofSeconds(30), Duration.ofSeconds(60));

	/** 失敗した呼び出しが実行の中でこの回数続いたら、その実行の呼び出しをすべて止める */
	static final int MAX_CONSECUTIVE_FAILURES = 5;

	/** KOPIS のステップの順（割り当ては後ろのステップの上限を先に取り置く） */
	static final List<String> STEPS = List.of("S1", "S2", "S3", "S4", "S6");

	private final Retry retry;

	private final Sleeper sleeper;

	private final Duration interval;

	private final int perRun;

	private final CallLimit limit;

	// アプリの中で前の送信があったか（起動して最初の送信は待たない）
	private boolean sentBefore;

	private KopisStopReason stopReason;

	private String stopMessage;

	private int runCalls;

	private int consecutiveFailures;

	private String step;

	private int stepCalls;

	private int stepAllowance;

	public KopisCallPolicy(Retry retry, Sleeper sleeper, CatticketProperties props) {
		this.retry = retry;
		this.sleeper = sleeper;
		CatticketProperties.Kopis kopis = props.kopis();
		this.interval = kopis.interval();
		this.limit = kopis.callLimit();
		// 設定の誤りのまま大きく呼ばないよう、起動を失敗させる
		if (interval == null || interval.isNegative()) {
			throw new IllegalStateException("catticket.kopis.interval must be set and not negative: " + interval);
		}
		if (limit == null) {
			throw new IllegalStateException("catticket.kopis.call-limit is not set");
		}
		if (limit.perRun() <= 0) {
			throw new IllegalStateException("catticket.kopis.call-limit.per-run must be positive: " + limit.perRun());
		}
		if (limit.s2() < 0 || limit.s3() < 0 || limit.s4() < 0 || limit.s6() < 0) {
			throw new IllegalStateException("catticket.kopis.call-limit must not be negative: " + limit);
		}
		this.perRun = limit.perRun();
		long reserved = (long) limit.s2() + limit.s3() + limit.s4() + limit.s6();
		if (reserved >= perRun) {
			log.warn("KOPIS call limits of S2-S6 ({}) leave nothing for S1 (per-run {})", reserved, perRun);
		}
		log.info("KOPIS calls: interval={}, per-run={}, S2={}, S3={}, S4={}, S6={} (S1 up to {})", interval, perRun,
				limit.s2(), limit.s3(), limit.s4(), limit.s6(), Math.max(0, perRun - reserved));
	}

	/** 実行の初めに1回（DailyCollectJob）。止めた印・送信の数・続けての失敗の数・今のステップを戻す */
	public synchronized void startJob() {
		stopReason = null;
		stopMessage = null;
		runCalls = 0;
		consecutiveFailures = 0;
		step = null;
		stepCalls = 0;
		stepAllowance = 0;
	}

	/**
	 * KOPIS のステップの初めに（DailyCollectJob が呼ぶ）。ステップの送信の数を 0 にし、割り当てを決める。
	 * 止めた印・続けての失敗の数は戻さない。
	 */
	public synchronized void startStep(String stepName) {
		int index = STEPS.indexOf(stepName);
		if (index < 0) {
			throw new IllegalArgumentException("not a KOPIS step: " + stepName);
		}
		long laterCaps = 0;
		for (String later : STEPS.subList(index + 1, STEPS.size())) {
			laterCaps += cap(later);
		}
		// 使い残しは後ろへ回さない（1日の回数を小さく保つ）
		long allowance = Math.min(cap(stepName), (long) perRun - runCalls - laterCaps);
		step = stepName;
		stepCalls = 0;
		stepAllowance = (int) Math.max(0, allowance);
	}

	public synchronized boolean stopped() {
		return stopReason != null;
	}

	/** 止めた理由（{@code {止めたステップ}: {メッセージ}}）。止めていなければ null */
	public synchronized String stopMessage() {
		return stopMessage;
	}

	/** 今のステップの残りの回数 */
	public synchronized int allowance() {
		return Math.max(0, stepAllowance - stepCalls);
	}

	public synchronized int stepCalls() {
		return stepCalls;
	}

	public synchronized int runCalls() {
		return runCalls;
	}

	public synchronized int perRun() {
		return perRun;
	}

	/**
	 * 1回の呼び出し。前の送信との間を {@code interval} あけ、{@link KopisRetryableException} は 30・60 秒あけて最大 2 回やり直す。
	 *
	 * @throws KopisStoppedException   遮断の印を受けた・続けて 5 回失敗した（止めた後は送らずに投げる）
	 * @throws KopisCallLimitException ステップの割り当てを使い切った（送らずに投げる）
	 */
	public synchronized <T> T call(Supplier<T> attempt) {
		if (stopReason != null) {
			throw new KopisStoppedException(stopReason, "KOPIS calls stopped in this run: " + stopMessage, null);
		}
		// 上限に当たる呼び出しで間隔を待たないよう、待つ前にも確かめる
		checkLimit();
		if (sentBefore) {
			sleeper.sleep(interval);
		}
		Supplier<T> sending = () -> {
			// 再試行の送信も1回と数えるので、送るたびに確かめる
			checkLimit();
			stepCalls++;
			runCalls++;
			sentBefore = true;
			return attempt.get();
		};
		try {
			T result = retry.call(sending, RETRY_WAITS, e -> e instanceof KopisRetryableException);
			consecutiveFailures = 0;
			return result;
		}
		catch (KopisBlockedException e) {
			// 遮断の後に呼び続けると長くなるおそれがあるので、再試行せず実行の全体を止める
			String message = "KOPIS blocked: " + e.getMessage();
			stop(KopisStopReason.BLOCKED, message);
			log.error("KOPIS blocked in {}; stopping all KOPIS calls in this run: {}", step, e.getMessage());
			throw new KopisStoppedException(KopisStopReason.BLOCKED, message, e);
		}
		catch (KopisRetryableException | KopisFatalException e) {
			consecutiveFailures++;
			if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
				String message = "KOPIS calls stopped after " + consecutiveFailures + " consecutive failures; last: "
						+ e.getMessage();
				stop(KopisStopReason.CONSECUTIVE_FAILURES, message);
				log.error("KOPIS: {} consecutive failed calls in {}; stopping all KOPIS calls in this run",
						consecutiveFailures, step);
				throw new KopisStoppedException(KopisStopReason.CONSECUTIVE_FAILURES, message, e);
			}
			throw e;
		}
	}

	private void checkLimit() {
		if (stepCalls >= stepAllowance) {
			// 送っていないので続けての失敗には数えない
			throw new KopisCallLimitException("KOPIS call limit reached in " + step + ": " + stepCalls + "/"
					+ stepAllowance + " (run " + runCalls + "/" + perRun + ")");
		}
	}

	private void stop(KopisStopReason reason, String message) {
		stopReason = reason;
		stopMessage = step + ": " + message;
	}

	// S1 は上限を持たず、1回の上限まで（残りは割り当ての式で決まる）
	private long cap(String stepName) {
		return switch (stepName) {
			case "S1" -> perRun;
			case "S2" -> limit.s2();
			case "S3" -> limit.s3();
			case "S4" -> limit.s4();
			case "S6" -> limit.s6();
			default -> throw new IllegalArgumentException("not a KOPIS step: " + stepName);
		};
	}

}
