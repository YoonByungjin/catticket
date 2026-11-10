package com.catticket.auth.row;

import com.catticket.common.security.Role;

/**
 * セッションの確認（06 の 3.4 の SessionUserFilter）で読む app_user の行。status は PENDING / ACTIVE。
 */
public record SessionUserRow(long id, Role role, String status, String nickname, String passwordHash) {

	public static final String STATUS_ACTIVE = "ACTIVE";

	public boolean isActive() {
		return STATUS_ACTIVE.equals(status);
	}

	// toString でハッシュをログに出さない
	@Override
	public String toString() {
		return "SessionUserRow[id=" + id + ", role=" + role + ", status=" + status + "]";
	}

}
