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

	/**
	 * @param interval  送信ごとの間隔（05 の 3.5。7 秒）
	 * @param callLimit 1回の実行の送信の上限（05 の 3.5）。application.yml は開発の値、application-prod.yml は本番の値
	 */
	public record Kopis(String baseUrl, String apiKey, Duration interval, CallLimit callLimit) {

		// toString でキーをログに出さない
		@Override
		public String toString() {
			return "Kopis[baseUrl=" + baseUrl + ", interval=" + interval + ", callLimit=" + callLimit + "]";
		}

		/** 1回の実行の上限（perRun）と、S1 より後ろのステップごとの上限（05 の 3.5。S1 は残り） */
		public record CallLimit(int perRun, int s2, int s3, int s4, int s6) {
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

	/** B-02 の4つのファイル（Spring の Resource の場所）と文字コード（06 の 8.1） */
	public record Subway(String stationLocation, String stationAddress, String locker, String stationNameJa,
			String charset) {
	}

}
