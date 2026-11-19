package com.catticket.common.batch;

import java.time.Duration;

import org.springframework.stereotype.Component;

/**
 * 本番の待ち時間（Thread.sleep）。
 */
@Component
public class ThreadSleeper implements Sleeper {

	@Override
	public void sleep(Duration duration) {
		try {
			Thread.sleep(duration);
		}
		catch (InterruptedException e) {
			// 割り込みの状態を戻し、呼び出し元に止めたことを伝える
			Thread.currentThread().interrupt();
			throw new IllegalStateException("interrupted while sleeping", e);
		}
	}

}
