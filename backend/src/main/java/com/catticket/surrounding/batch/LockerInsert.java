package com.catticket.surrounding.batch;

/**
 * 入れるコインロッカー（04 の coin_locker）。属する駅は外部の駅コードで指す（INSERT の副問い合わせで station_id にする）。
 */
public record LockerInsert(String stationExternalCode, String name, String locationText) {
}
