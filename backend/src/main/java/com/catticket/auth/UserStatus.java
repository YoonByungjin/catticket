package com.catticket.auth;

/**
 * アカウントの状態（04 の app_user.status。06 の 4.5）。認証前の申込は PENDING、登録の完了で ACTIVE。
 */
public enum UserStatus {

	PENDING, ACTIVE

}
