package com.catticket.common.db;

import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PessimisticLockingFailureException;

/**
 * MySQL のエラーの見分け（06 の 3.2）。
 */
public final class DbErrors {

	private static final int ER_LOCK_WAIT_TIMEOUT = 1205;

	private static final int ER_LOCK_DEADLOCK = 1213;

	private DbErrors() {
	}

	/**
	 * 一意制約 keyName（例: uk_app_user_email）の違反か。
	 * MySQL 8 のメッセージは「for key 'app_user.uk_app_user_email'」の形なので、制約名の前後で見分ける。
	 */
	public static boolean isDuplicateKey(DataAccessException ex, String keyName) {
		if (!(ex instanceof DuplicateKeyException)) {
			return false;
		}
		String message = ex.getMostSpecificCause().getMessage();
		if (message == null) {
			return false;
		}
		return message.contains("'" + keyName + "'") || message.contains("." + keyName + "'");
	}

	/** デッドロック（1213）・ロック待ちのタイムアウト（1205）か */
	public static boolean isLockFailure(DataAccessException ex) {
		if (ex instanceof PessimisticLockingFailureException) {
			return true;
		}
		for (Throwable t = ex; t != null; t = t.getCause()) {
			if (t instanceof SQLException sql
					&& (sql.getErrorCode() == ER_LOCK_DEADLOCK || sql.getErrorCode() == ER_LOCK_WAIT_TIMEOUT)) {
				return true;
			}
		}
		return false;
	}

}
