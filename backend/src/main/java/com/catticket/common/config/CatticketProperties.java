package com.catticket.common.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code catticket.*} の設定（06 の 8.1）。起動時に必須を確かめない（テストで API キーが空でも起動できるように）。
 * 値が要る部品が、使う時点で空なら失敗する。
 *
 * @param task 手での実行のときだけコマンドの引数で渡すバッチの名前（06 の 5.1）。ふだんは null
 */
@ConfigurationProperties("catticket")
public record CatticketProperties(String publicBaseUrl, Scheduler scheduler, Kopis kopis, Azure azure,
		Recaptcha recaptcha, Mail mail, Admin admin, Subway subway, String task) {

	public record Scheduler(boolean enabled) {
	}

	public record Kopis(String baseUrl, String apiKey, Duration interval) {

		// toString でキーをログに出さない
		@Override
		public String toString() {
			return "Kopis[baseUrl=" + baseUrl + ", interval=" + interval + "]";
		}

	}

	public record Azure(String translatorEndpoint, String translatorKey, String translatorRegion,
			int requestMaxChars, int charsPerMinute, long monthlyLimitChars) {

		@Override
		public String toString() {
			return "Azure[translatorEndpoint=" + translatorEndpoint + ", translatorRegion=" + translatorRegion
					+ ", requestMaxChars=" + requestMaxChars + ", charsPerMinute=" + charsPerMinute
					+ ", monthlyLimitChars=" + monthlyLimitChars + "]";
		}

	}

	public record Recaptcha(String projectId, String apiKey, String siteKey) {

		@Override
		public String toString() {
			return "Recaptcha[projectId=" + projectId + ", siteKey=" + siteKey + "]";
		}

	}

	public record Mail(String from, int dailyLimit) {
	}

	public record Admin(String email, String password) {

		@Override
		public String toString() {
			return "Admin[email=***]";
		}

	}

	/** ファイルの名前・文字コードは U-03 で決める（06 の 8.1） */
	public record Subway(String stationLocation, String stationAddress, String locker, String stationNameJa,
			String charset) {
	}

}
