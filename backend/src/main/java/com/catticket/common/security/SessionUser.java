package com.catticket.common.security;

/**
 * リクエストごとの利用者。Controller は {@code @AuthenticationPrincipal SessionUser user} で受ける（未ログインは null）。
 * nickname は管理者では null。
 */
public record SessionUser(long userId, Role role, String nickname) {
}
