package com.catticket.common.batch;

/**
 * バッチの結果（06 の 5.2）。LOCKED は記録を作らないので batch_job_run.result には入らない。
 */
public enum JobResult {

	SUCCESS, PARTIAL_FAILURE, FAILURE, LOCKED

}
