package com.catticket.collect.client;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import com.catticket.collect.client.xml.AwardListResponse;
import com.catticket.collect.client.xml.BoxOfficeResponse;
import com.catticket.collect.client.xml.GenreStatsResponse;
import com.catticket.collect.client.xml.KopisAwardItem;
import com.catticket.collect.client.xml.KopisBoxOfficeItem;
import com.catticket.collect.client.xml.KopisEnvelope;
import com.catticket.collect.client.xml.KopisGenreStat;
import com.catticket.collect.client.xml.KopisPerformanceDetail;
import com.catticket.collect.client.xml.KopisPerformanceListItem;
import com.catticket.collect.client.xml.KopisVenueDetail;
import com.catticket.collect.client.xml.PerformanceDetailResponse;
import com.catticket.collect.client.xml.PerformanceListResponse;
import com.catticket.collect.client.xml.VenueDetailResponse;
import com.catticket.common.code.Genre;
import com.catticket.common.config.CatticketProperties;
import com.catticket.common.display.Period;

/**
 * KOPIS の API（ADR 0008、06 の 4.9・7.1）。呼び出しはすべて {@link KopisCallPolicy} を通す。
 * API キーはクエリの {@code service} に入るので、ログ・例外のメッセージに URL を出すときはキーを伏せる。
 */
@Component
public class KopisClient {

	private static final Logger log = LoggerFactory.getLogger(KopisClient.class);

	/** ソウル（{@code signgucode}・{@code area}） */
	static final String SEOUL = "11";

	private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private static final Pattern SERVICE_PARAM = Pattern.compile("([?&]service=)[^&]*");

	private final RestClient restClient;

	private final String apiKey;

	private final KopisCallPolicy policy;

	private final KopisXmlReader reader = new KopisXmlReader();

	public KopisClient(@Qualifier("kopisRestClient") RestClient restClient, CatticketProperties props,
			KopisCallPolicy policy) {
		this.restClient = restClient;
		this.apiKey = props.kopis().apiKey();
		this.policy = policy;
	}

	/** 公演の目録（{@code pblprfr}）。afterDate が null なら {@code afterdate} を送らない */
	public KopisPage<KopisPerformanceListItem> listPerformances(LocalDate from, LocalDate to, LocalDate afterDate,
			int page) {
		List<KopisPerformanceListItem> rows = fetch("pblprfr", b -> {
			b.path("/pblprfr")
				.queryParam("stdate", DATE.format(from))
				.queryParam("eddate", DATE.format(to))
				.queryParam("cpage", page)
				.queryParam("rows", KopisPage.ROWS)
				.queryParam("signgucode", SEOUL);
			if (afterDate != null) {
				b.queryParam("afterdate", DATE.format(afterDate));
			}
			return b;
		}, Map.of(), PerformanceListResponse.class);
		return new KopisPage<>(rows);
	}

	/** 公演詳細（{@code pblprfr/{mt20id}}）。結果コード 04 は空 */
	public Optional<KopisPerformanceDetail> getPerformance(String mt20id) {
		return first(fetch("pblprfr/{id}", b -> b.path("/pblprfr/{id}"), Map.of("id", mt20id),
				PerformanceDetailResponse.class));
	}

	/** 施設詳細（{@code prfplc/{mt10id}}）。結果コード 04 は空 */
	public Optional<KopisVenueDetail> getVenue(String mt10id) {
		return first(fetch("prfplc/{id}", b -> b.path("/prfplc/{id}"), Map.of("id", mt10id),
				VenueDetailResponse.class));
	}

	/** 順位（{@code boxoffice}）。最大 50 行・ページなし。ジャンルが空なら全体（{@code catecode} を送らない） */
	public List<KopisBoxOfficeItem> boxOffice(Period period, Optional<Genre> genre) {
		return fetch("boxoffice", b -> {
			b.path("/boxoffice")
				.queryParam("stdate", DATE.format(period.start()))
				.queryParam("eddate", DATE.format(period.end()))
				.queryParam("area", SEOUL);
			genre.ifPresent(g -> b.queryParam("catecode", g.code()));
			return b;
		}, Map.of(), BoxOfficeResponse.class);
	}

	/** 受賞作（{@code prfawad}）。{@code afterdate} は送らない（05 の 3.2 の S3） */
	public KopisPage<KopisAwardItem> awards(LocalDate from, LocalDate to, int page) {
		List<KopisAwardItem> rows = fetch("prfawad",
				b -> b.path("/prfawad")
					.queryParam("stdate", DATE.format(from))
					.queryParam("eddate", DATE.format(to))
					.queryParam("cpage", page)
					.queryParam("rows", KopisPage.ROWS)
					.queryParam("signgucode", SEOUL),
				Map.of(), AwardListResponse.class);
		return new KopisPage<>(rows);
	}

	/** ジャンル別統計（{@code boxStatsCate}）。地域の変数は効かないので送らない。{@code 합계} の行も含む */
	public List<KopisGenreStat> genreStats(Period period) {
		return fetch("boxStatsCate",
				b -> b.path("/boxStatsCate")
					.queryParam("stdate", DATE.format(period.start()))
					.queryParam("eddate", DATE.format(period.end())),
				Map.of(), GenreStatsResponse.class);
	}

	/** URL の {@code service}（API キー）を伏せる */
	static String maskKey(String url) {
		if (url == null) {
			return null;
		}
		Matcher m = SERVICE_PARAM.matcher(url);
		return m.replaceAll("$1***");
	}

	// URL の形でない所にキーがそのまま出ても伏せる
	private String mask(String text) {
		String masked = maskKey(text);
		return masked == null || apiKey == null || apiKey.isBlank() ? masked : masked.replace(apiKey, "***");
	}

	private <T> List<T> fetch(String service, Function<UriBuilder, UriBuilder> path, Map<String, ?> pathVars,
			Class<? extends KopisEnvelope<T>> envelopeType) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new KopisFatalException("KOPIS api key is not set (catticket.kopis.api-key)");
		}
		return policy.call(() -> {
			byte[] body = get(service, path, pathVars);
			return reader.readRows(service, body, envelopeType);
		});
	}

	private byte[] get(String service, Function<UriBuilder, UriBuilder> path, Map<String, ?> pathVars) {
		AtomicReference<String> maskedUrl = new AtomicReference<>();
		try {
			return restClient.get().uri(b -> {
				// キーは変数で渡してエンコードさせる
				Map<String, Object> vars = new HashMap<>(pathVars);
				vars.put("service", apiKey);
				URI uri = path.apply(b).queryParam("service", "{service}").build(vars);
				maskedUrl.set(mask(uri.toString()));
				log.debug("KOPIS GET {}", maskedUrl.get());
				return uri;
			}).exchange((request, response) -> {
				HttpStatusCode status = response.getStatusCode();
				if (status.is5xxServerError()) {
					throw new KopisRetryableException("KOPIS " + service + " HTTP " + status.value());
				}
				if (!status.is2xxSuccessful()) {
					// 05 の 3.5 にない失敗は意味が分からないので呼び続けない
					throw new KopisFatalException("KOPIS " + service + " HTTP " + status.value());
				}
				return response.getBody().readAllBytes();
			});
		}
		catch (KopisRetryableException | KopisFatalException e) {
			log.warn("KOPIS GET {} failed: {}", maskedUrl.get(), e.getMessage());
			throw e;
		}
		catch (RestClientException e) {
			// RestClient の例外のメッセージには URL（キー）が入るので使わない。原因の I/O の例外だけを付ける
			Throwable cause = e.getMostSpecificCause();
			String reason = cause == e ? e.getClass().getSimpleName()
					: cause.getClass().getSimpleName() + ": " + mask(cause.getMessage());
			log.warn("KOPIS GET {} failed: {}", maskedUrl.get(), reason);
			throw new KopisRetryableException("KOPIS " + service + " I/O error: " + reason,
					cause == e ? null : cause);
		}
	}

	private static <T> Optional<T> first(List<T> rows) {
		return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
	}

}
