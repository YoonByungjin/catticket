package com.catticket.collect.client;

/**
 * KOPIS の結果コードの応答（{@code dbs/db/returncode}・{@code errmsg}。06 の 7.1）。値は strip 済み。
 */
public record KopisResult(String code, String errmsg) {
}
