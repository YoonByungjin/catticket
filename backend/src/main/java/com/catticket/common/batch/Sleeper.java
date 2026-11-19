package com.catticket.common.batch;

import java.time.Duration;

/**
 * 待ち時間（06 の 5.2）。テストでは待たずに記録だけする実装に替える。
 */
public interface Sleeper {

	void sleep(Duration duration);

}
