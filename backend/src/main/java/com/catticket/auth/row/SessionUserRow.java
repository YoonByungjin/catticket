package com.catticket.auth.row;

import com.catticket.auth.UserStatus;
import com.catticket.common.security.Role;

/**
 * セッションの確認（06 の 3.4 の SessionUserFilter）で読む app_user の行。
 */
public record SessionUserRow(long id, Role role, UserStatus status, String nickname, String passwordHash) {

	public boolean isActive() {
		return status == UserStatus.ACTIVE;
	}

	// toString でハッシュをログに出さない
	@Override
	public String toString() {
		return "SessionUserRow[id=" + id + ", role=" + role + ", status=" + status + "]";
	}

}
