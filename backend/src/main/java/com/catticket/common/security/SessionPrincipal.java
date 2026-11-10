package com.catticket.common.security;

import java.io.Serializable;

/**
 * セッションに保存するもの（03 の 7.4）。ニックネーム・役割は持たず、リクエストのたびに DB から読む。
 * passwordHash はほかの端末でのパスワードの変更を見つけるために持つ。
 */
public record SessionPrincipal(long userId, String passwordHash) implements Serializable {

	// toString でハッシュをログに出さない
	@Override
	public String toString() {
		return "SessionPrincipal[userId=" + userId + "]";
	}

}
