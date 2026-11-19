package com.catticket.common.batch;

/**
 * バッチのジョブ（06 の 5.2）。TaskRunner が名前で選んで実行する。
 */
public interface BatchJob {

	TaskName name();

	JobResult run();

}
