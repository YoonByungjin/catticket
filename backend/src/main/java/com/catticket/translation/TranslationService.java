package com.catticket.translation;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.catticket.common.batch.BatchRunRecorder;
import com.catticket.common.batch.Sleeper;
import com.catticket.common.batch.StepResult;
import com.catticket.common.batch.StepSummary;
import com.catticket.common.config.CatticketProperties;
import com.catticket.translation.client.AzureTranslatorClient;
import com.catticket.translation.client.TranslatorException;
import com.catticket.translation.client.TranslatorQuotaExceededException;
import com.catticket.translation.client.TranslatorThrottledException;

/**
 * B-01 の S7 翻訳（05 の 3.7、06 の 4.10）。
 * メソッドにはトランザクションを付けない（Azure を待つ間に DB の接続を持たない。6.1）。束ごとに TranslationBatchWriter で保存する。
 */
@Service
public class TranslationService {

	private static final Logger log = LoggerFactory.getLogger(TranslationService.class);

	/** 1回の要求の要素の上限（Azure の公式の制限。06 の 4.10 の 2.） */
	static final int MAX_ELEMENTS = 1000;

	/** 種類ごとに1回で読む原文の数。保存した分は次の読みに出ないので、読み直して続ける */
	static final int READ_LIMIT = 1000;

	/** 429 の待ちと再試行の回数（05 の 3.7） */
	static final Duration THROTTLED_WAIT = Duration.ofSeconds(60);

	static final int THROTTLED_RETRIES = 3;

	/** そのほかのエラーの再試行の待ち（05 の 3.7） */
	static final List<Duration> RETRY_WAITS = List.of(Duration.ofSeconds(2), Duration.ofSeconds(4),
			Duration.ofSeconds(8));

	private final TranslationMapper mapper;

	private final TranslationUsageMapper usageMapper;

	private final TranslationBatchWriter writer;

	private final AzureTranslatorClient client;

	private final Sleeper sleeper;

	private final int requestMaxChars;

	private final int charsPerMinute;

	private final long monthlyLimitChars;

	public TranslationService(TranslationMapper mapper, TranslationUsageMapper usageMapper,
			TranslationBatchWriter writer, AzureTranslatorClient client, Sleeper sleeper, CatticketProperties props) {
		this.mapper = mapper;
		this.usageMapper = usageMapper;
		this.writer = writer;
		this.client = client;
		this.sleeper = sleeper;
		this.requestMaxChars = props.azure().requestMaxChars();
		this.charsPerMinute = props.azure().charsPerMinute();
		this.monthlyLimitChars = props.azure().monthlyLimitChars();
	}

	/**
	 * 翻訳がまだない原文を、種類の順（公演名 → 会場名 → 駅名 → それ以外）に翻訳して保存する。
	 * 月の上限で止めたときは成功（内訳に monthlyLimit）。403001・再試行の後の失敗は失敗（それまでの保存は残る）。
	 */
	public StepResult translatePending(LocalDateTime now) {
		StepSummary summary = new StepSummary();
		LocalDate month = now.toLocalDate().withDayOfMonth(1);
		// この実行で送った原文。保存したのに読みに出続ける原文があっても、同じものを送り続けない
		Set<String> sent = new HashSet<>();
		long translated = 0;
		long previousChars = 0;
		for (Source source : sources()) {
			while (true) {
				List<String> texts = new ArrayList<>();
				for (String text : source.reader().apply(READ_LIMIT)) {
					if (sent.add(text)) {
						texts.add(text);
					}
				}
				if (texts.isEmpty()) {
					break;
				}
				for (List<String> batch : batches(texts, requestMaxChars, MAX_ELEMENTS)) {
					long chars = charCount(batch);
					long used = usageMapper.findChars(month);
					if (used + chars > monthlyLimitChars) {
						summary.increment("monthlyLimit");
						summary.addDetail("monthlyLimit", month + "|used=" + used + "|request=" + chars);
						log.warn("S7 stopped at the monthly limit: used={} request={} limit={}", used, chars,
								monthlyLimitChars);
						return finish(StepResult.success(translated, summary));
					}
					if (previousChars > 0) {
						// 1分あたりの文字数を超えないよう、前の要求の文字数の分あける
						sleeper.sleep(rateWait(previousChars));
					}
					List<String> results;
					try {
						results = send(batch);
					}
					catch (TranslatorException e) {
						String message = BatchRunRecorder.describe(e);
						log.error("S7 stopped: {}", message);
						return finish(StepResult.failure(translated, summary, message));
					}
					List<TranslationPair> pairs = new ArrayList<>(batch.size());
					for (int i = 0; i < batch.size(); i++) {
						pairs.add(new TranslationPair(batch.get(i), results.get(i)));
					}
					writer.save(pairs, chars, month, now);
					translated += batch.size();
					summary.add(source.name(), batch.size());
					summary.add("chars", chars);
					summary.increment("requests");
					previousChars = chars;
				}
			}
		}
		return finish(StepResult.success(translated, summary));
	}

	private StepResult finish(StepResult result) {
		log.info("S7 finished: success={} translated={} {}", result.success(), result.processedCount(),
				result.summary());
		return result;
	}

	/** 05 の 3.7 の順。上限に近いときに大事なものから訳される */
	private List<Source> sources() {
		return List.of(new Source("titles", mapper::findUntranslatedTitles),
				new Source("venueNames", mapper::findUntranslatedVenueNames),
				new Source("stationNames", mapper::findUntranslatedStationNames),
				new Source("others", mapper::findUntranslatedOthers));
	}

	/** 403001 はすぐ止め、429 は 60 秒・そのほかは 2・4・8 秒で最大 3 回やり直す（05 の 3.7） */
	private List<String> send(List<String> batch) {
		int throttled = 0;
		int failed = 0;
		while (true) {
			try {
				return client.translate(batch);
			}
			catch (TranslatorQuotaExceededException e) {
				throw e;
			}
			catch (TranslatorThrottledException e) {
				if (throttled >= THROTTLED_RETRIES) {
					throw e;
				}
				log.warn("S7 throttled, retrying in {}: {}", THROTTLED_WAIT, e.getMessage());
				sleeper.sleep(THROTTLED_WAIT);
				throttled++;
			}
			catch (TranslatorException e) {
				if (failed >= RETRY_WAITS.size()) {
					throw e;
				}
				log.warn("S7 request failed, retrying in {}: {}", RETRY_WAITS.get(failed), e.getMessage());
				sleeper.sleep(RETRY_WAITS.get(failed));
				failed++;
			}
		}
	}

	/** n 文字のあとに n ÷ (1分の文字数) 分あける（06 の 4.10 の 6.）。切り上げて速くならないようにする */
	Duration rateWait(long chars) {
		long millis = (chars * 60_000L + charsPerMinute - 1) / charsPerMinute;
		return Duration.ofMillis(millis);
	}

	/**
	 * 順を保って束ねる: 1回 maxChars 文字（コードポイント）以内・maxElements 個以内。
	 * 1つで maxChars を超える原文は、その原文だけで1回にする（06 の 4.10 の 2.）。
	 */
	static List<List<String>> batches(List<String> texts, int maxChars, int maxElements) {
		List<List<String>> batches = new ArrayList<>();
		List<String> current = new ArrayList<>();
		long currentChars = 0;
		for (String text : texts) {
			long chars = charCount(text);
			if (!current.isEmpty() && (currentChars + chars > maxChars || current.size() >= maxElements)) {
				batches.add(current);
				current = new ArrayList<>();
				currentChars = 0;
			}
			current.add(text);
			currentChars += chars;
		}
		if (!current.isEmpty()) {
			batches.add(current);
		}
		return batches;
	}

	/** 文字数はコードポイントで数える（06 の 4.10 の 2.。束の大きさ・月の数・速さで同じ数え方にする） */
	static long charCount(String text) {
		return text.codePointCount(0, text.length());
	}

	static long charCount(List<String> texts) {
		long total = 0;
		for (String text : texts) {
			total += charCount(text);
		}
		return total;
	}

	private record Source(String name, IntFunction<List<String>> reader) {
	}

}
