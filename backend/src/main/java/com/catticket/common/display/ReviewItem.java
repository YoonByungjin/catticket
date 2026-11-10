package com.catticket.common.display;

import java.time.OffsetDateTime;

/**
 * レビュー（03 の 7.2）。未ログインへの応答では body・nickname・createdAt が null。
 */
public record ReviewItem(long reviewId, int rating, String body, String nickname, OffsetDateTime createdAt) {
}
