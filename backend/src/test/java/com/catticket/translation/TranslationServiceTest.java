package com.catticket.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.catticket.common.batch.Sleeper;
import com.catticket.common.batch.StepResult;
import com.catticket.common.config.CatticketProperties;
import com.catticket.common.text.Hashes;
import com.catticket.translation.client.AzureTranslatorClient;
import com.catticket.translation.client.TranslatorException;
import com.catticket.translation.client.TranslatorFatalException;
import com.catticket.translation.client.TranslatorQuotaExceededException;
import com.catticket.translation.client.TranslatorThrottledException;

/**
 * TranslationService（S7。05 の 3.7、06 の 4.10）。Mapper・保存・クライアントはモック、待ちは記録だけ。
 */
class TranslationServiceTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 6, 0, 0);

	private static final LocalDate MONTH = LocalDate.of(2026, 11, 1);

	private static final CatticketProperties PROPS = props("test-azure-key");

	private final TranslationMapper mapper = mock(TranslationMapper.class);

	private final TranslationUsageMapper usageMapper = mock(TranslationUsageMapper.class);

	private final TranslationBatchWriter writer = mock(TranslationBatchWriter.class);

	private final AzureTranslatorClient client = mock(AzureTranslatorClient.class);

	private final List<Duration> sleeps = new ArrayList<>();

	private final Sleeper sleeper = sleeps::add;

	private final TranslationService service = new TranslationService(mapper, usageMapper, writer, client, sleeper,
			PROPS);

	private static CatticketProperties props(String key) {
		return new CatticketProperties(null, null, null,
				new CatticketProperties.Azure("https://translator.test", key, "", 10000, 30000, 1800000), null, null,
				null, null, null);
	}

	/** 各種類の読みが、1回目に texts を返し、2回目からは空（保存した分は出ない） */
	private void pending(List<String> titles, List<String> venues, List<String> stations, List<String> others) {
		when(mapper.findUntranslatedTitles(anyInt())).thenReturn(titles, List.of());
		when(mapper.findUntranslatedVenueNames(anyInt())).thenReturn(venues, List.of());
		when(mapper.findUntranslatedStationNames(anyInt())).thenReturn(stations, List.of());
		when(mapper.findUntranslatedOthers(anyInt())).thenReturn(others, List.of());
	}

	/** 訳文は原文の前に「ja:」を付けたもの */
	private void translatesAll() {
		when(client.translate(anyList())).thenAnswer(inv -> {
			List<String> texts = inv.getArgument(0);
			return texts.stream().map(t -> "ja:" + t).toList();
		});
	}

	private static String repeat(char c, int n) {
		return String.valueOf(c).repeat(n);
	}

	@Nested
	class Batching {

		@Test
		void keepsOrderAndSplitsAtMaxChars() {
			List<List<String>> batches = TranslationService.batches(List.of("가나", "다라마", "바", "사아자차"), 5, 1000);

			assertThat(batches).containsExactly(List.of("가나", "다라마"), List.of("바", "사아자차"));
		}

		@Test
		void splitsAtMaxElements() {
			List<String> texts = new ArrayList<>();
			for (int i = 0; i < 2001; i++) {
				texts.add("a");
			}

			List<List<String>> batches = TranslationService.batches(texts, 10000, 1000);

			assertThat(batches).extracting(List::size).containsExactly(1000, 1000, 1);
		}

		@Test
		void exactlyMaxCharsFitsInOneBatch() {
			List<List<String>> batches = TranslationService.batches(List.of(repeat('a', 4000), repeat('b', 6000)),
					10000, 1000);

			assertThat(batches).hasSize(1);
		}

		@Test
		void countsCodePointsNotUtf16Units() {
			// サロゲートペアの文字（例: 𠮷）は1文字
			assertThat(TranslationService.charCount("𠮷가")).isEqualTo(2);
			assertThat(TranslationService.batches(List.of("𠮷𠮷", "𠮷"), 3, 1000)).hasSize(1);
		}

		@Test
		void requestsAreSplitByThe10000CharLimit() {
			pending(List.of(repeat('가', 6000), repeat('나', 6000)), List.of(), List.of(), List.of());
			translatesAll();

			StepResult result = service.translatePending(NOW);

			assertThat(result.success()).isTrue();
			verify(client, times(2)).translate(anyList());
			assertThat(result.summary().get("requests")).isEqualTo(2);
		}

	}

	@Nested
	class Targets {

		@Test
		void readsKindsInOrderAndSavesEachBatchWithUsage() {
			pending(List.of("공연"), List.of("극장"), List.of("역"), List.of("주소"));
			translatesAll();

			StepResult result = service.translatePending(NOW);

			InOrder order = inOrder(mapper, client, writer);
			order.verify(mapper).findUntranslatedTitles(TranslationService.READ_LIMIT);
			order.verify(client).translate(List.of("공연"));
			order.verify(writer).save(List.of(new TranslationPair("공연", "ja:공연")), 2, MONTH, NOW);
			order.verify(mapper).findUntranslatedVenueNames(TranslationService.READ_LIMIT);
			order.verify(client).translate(List.of("극장"));
			order.verify(mapper).findUntranslatedStationNames(TranslationService.READ_LIMIT);
			order.verify(client).translate(List.of("역"));
			order.verify(mapper).findUntranslatedOthers(TranslationService.READ_LIMIT);
			order.verify(client).translate(List.of("주소"));
			assertThat(result.success()).isTrue();
			assertThat(result.processedCount()).isEqualTo(4);
			assertThat(result.summary().get("titles")).isEqualTo(1);
			assertThat(result.summary().get("venueNames")).isEqualTo(1);
			assertThat(result.summary().get("stationNames")).isEqualTo(1);
			assertThat(result.summary().get("others")).isEqualTo(1);
			assertThat(result.summary().get("chars")).isEqualTo(7);
		}

		@Test
		void sameOriginalIsTranslatedOnceAcrossKinds() {
			// 会場名と住所の読みに同じ原文が出ても、1回だけ送る
			pending(List.of(), List.of("예술의전당"), List.of(), List.of("예술의전당", "서울 서초구"));
			translatesAll();

			service.translatePending(NOW);

			verify(client).translate(List.of("예술의전당"));
			verify(client).translate(List.of("서울 서초구"));
			verify(client, times(2)).translate(anyList());
		}

		@Test
		void keepsReadingUntilNothingIsLeft() {
			when(mapper.findUntranslatedTitles(anyInt())).thenReturn(List.of("가"), List.of("나"), List.of());
			when(mapper.findUntranslatedVenueNames(anyInt())).thenReturn(List.of());
			when(mapper.findUntranslatedStationNames(anyInt())).thenReturn(List.of());
			when(mapper.findUntranslatedOthers(anyInt())).thenReturn(List.of());
			translatesAll();

			StepResult result = service.translatePending(NOW);

			verify(mapper, times(3)).findUntranslatedTitles(anyInt());
			verify(client).translate(List.of("가"));
			verify(client).translate(List.of("나"));
			assertThat(result.processedCount()).isEqualTo(2);
		}

		@Test
		void nothingPendingSendsNothing() {
			pending(List.of(), List.of(), List.of(), List.of());

			StepResult result = service.translatePending(NOW);

			assertThat(result.success()).isTrue();
			assertThat(result.processedCount()).isZero();
			verifyNoInteractions(client, writer);
			assertThat(sleeps).isEmpty();
		}

		@Test
		void originalStillReadAfterSavingIsNotSentAgain() {
			// 保存したのに読みに出続けても（ハッシュが合わないなど）、同じ原文を送り続けない
			when(mapper.findUntranslatedTitles(anyInt())).thenReturn(List.of("가"));
			when(mapper.findUntranslatedVenueNames(anyInt())).thenReturn(List.of());
			when(mapper.findUntranslatedStationNames(anyInt())).thenReturn(List.of());
			when(mapper.findUntranslatedOthers(anyInt())).thenReturn(List.of());
			translatesAll();

			StepResult result = service.translatePending(NOW);

			verify(client, times(1)).translate(anyList());
			verify(mapper, times(2)).findUntranslatedTitles(anyInt());
			assertThat(result.success()).isTrue();
		}

	}

	/** 1つで 1 万文字を超える原文は送らない（06 の 4.10 の 2.。U-15 ②） */
	@Nested
	class TooLong {

		@Test
		void textOver10000CharsIsNotSentAndOthersAre() {
			String big = repeat('가', 10001);
			pending(List.of("공연", big, "나"), List.of(), List.of(), List.of());
			translatesAll();

			StepResult result = service.translatePending(NOW);

			assertThat(result.success()).isTrue();
			verify(client).translate(List.of("공연", "나"));
			verify(client, times(1)).translate(anyList());
			assertThat(result.processedCount()).isEqualTo(2);
			assertThat(result.summary().get("tooLong")).isEqualTo(1);
			assertThat(result.summary().details("tooLong")).containsExactly("titles|10001|" + Hashes.sha256Hex(big));
		}

		@Test
		void exactly10000CharsIsSent() {
			String max = repeat('가', 10000);
			pending(List.of(), List.of(), List.of(), List.of(max));
			translatesAll();

			StepResult result = service.translatePending(NOW);

			assertThat(result.success()).isTrue();
			verify(client).translate(List.of(max));
			assertThat(result.summary().get("tooLong")).isZero();
		}

		@Test
		void codePointsAreCounted() {
			// サロゲートペアの文字は1文字。10,000 文字（UTF-16 では 20,000 単位）は送る
			String max = "𠮷".repeat(10000);
			String big = "𠮷".repeat(10001);
			pending(List.of(max, big), List.of(), List.of(), List.of());
			translatesAll();

			StepResult result = service.translatePending(NOW);

			verify(client).translate(List.of(max));
			assertThat(result.summary().details("tooLong")).containsExactly("titles|10001|" + Hashes.sha256Hex(big));
		}

		@Test
		void textStillReadIsCountedOnceAndReadingMovesOn() {
			// 送らない原文は翻訳がないので読みに出続けるが、数えるのは1回で、次の種類へ進む
			String big = repeat('a', 20000);
			when(mapper.findUntranslatedTitles(anyInt())).thenReturn(List.of(big));
			when(mapper.findUntranslatedVenueNames(anyInt())).thenReturn(List.of("극장"), List.of());
			when(mapper.findUntranslatedStationNames(anyInt())).thenReturn(List.of());
			when(mapper.findUntranslatedOthers(anyInt())).thenReturn(List.of(big));
			translatesAll();

			StepResult result = service.translatePending(NOW);

			assertThat(result.success()).isTrue();
			verify(mapper, times(2)).findUntranslatedTitles(anyInt());
			verify(client).translate(List.of("극장"));
			verify(client, times(1)).translate(anyList());
			assertThat(result.summary().get("tooLong")).isEqualTo(1);
			assertThat(result.summary().details("tooLong")).containsExactly("titles|20000|" + Hashes.sha256Hex(big));
		}

		@Test
		void onlyTooLongTextsSendNothing() {
			pending(List.of(repeat('a', 10001)), List.of(), List.of(), List.of());

			StepResult result = service.translatePending(NOW);

			assertThat(result.success()).isTrue();
			assertThat(result.processedCount()).isZero();
			assertThat(result.summary().get("tooLong")).isEqualTo(1);
			verifyNoInteractions(client, writer);
			assertThat(sleeps).isEmpty();
		}

	}

	@Nested
	class MonthlyLimit {

		@Test
		void stopsBeforeSendingWhenUsagePlusRequestExceedsTheLimit() {
			pending(List.of("가나다"), List.of("라"), List.of(), List.of());
			when(usageMapper.findChars(MONTH)).thenReturn(1_799_998L);

			StepResult result = service.translatePending(NOW);

			assertThat(result.success()).isTrue();
			assertThat(result.summary().get("monthlyLimit")).isEqualTo(1);
			assertThat(result.summary().details("monthlyLimit")).containsExactly("2026-11-01|used=1799998|request=3");
			verifyNoInteractions(client, writer);
			// 止めた後はほかの種類も読まない
			verify(mapper, never()).findUntranslatedVenueNames(anyInt());
		}

		@Test
		void exactlyTheLimitIsAllowed() {
			pending(List.of("가나"), List.of(), List.of(), List.of());
			when(usageMapper.findChars(MONTH)).thenReturn(1_799_998L);
			translatesAll();

			StepResult result = service.translatePending(NOW);

			assertThat(result.summary().get("monthlyLimit")).isZero();
			verify(writer).save(anyList(), eq(2L), eq(MONTH), eq(NOW));
		}

		@Test
		void checksUsageBeforeEveryRequestAndKeepsEarlierTranslations() {
			pending(List.of(repeat('가', 6000), repeat('나', 6000)), List.of(), List.of(), List.of());
			// 1回目の後に今月の数が増えて、2回目は上限を超える
			when(usageMapper.findChars(MONTH)).thenReturn(1_790_000L, 1_796_000L);
			translatesAll();

			StepResult result = service.translatePending(NOW);

			assertThat(result.success()).isTrue();
			assertThat(result.processedCount()).isEqualTo(1);
			assertThat(result.summary().get("monthlyLimit")).isEqualTo(1);
			verify(writer, times(1)).save(anyList(), eq(6000L), eq(MONTH), eq(NOW));
			verify(client, times(1)).translate(anyList());
		}

		@Test
		void monthIsTheSeoulCalendarMonthOfNow() {
			pending(List.of("가"), List.of(), List.of(), List.of());
			translatesAll();

			service.translatePending(LocalDateTime.of(2026, 12, 1, 0, 0, 0));

			verify(usageMapper).findChars(LocalDate.of(2026, 12, 1));
			verify(writer).save(anyList(), anyLong(), eq(LocalDate.of(2026, 12, 1)), any());
		}

	}

	@Nested
	class Rate {

		@Test
		void waitsForThePreviousRequestBetweenRequests() {
			pending(List.of(repeat('가', 10000), repeat('나', 3000)), List.of(), List.of("역"), List.of());
			translatesAll();

			service.translatePending(NOW);

			// 1万文字のあとに 20 秒、3千文字のあとに 6 秒。最後の要求のあとは待たない
			assertThat(sleeps).containsExactly(Duration.ofSeconds(20), Duration.ofSeconds(6));
		}

		@Test
		void waitIsRoundedUp() {
			assertThat(service.rateWait(1)).isEqualTo(Duration.ofMillis(2));
			assertThat(service.rateWait(30000)).isEqualTo(Duration.ofMinutes(1));
		}

	}

	@Nested
	class Errors {

		@Test
		void quotaExceededStopsWithoutRetryAndKeepsEarlierBatches() {
			pending(List.of(repeat('가', 6000), repeat('나', 6000)), List.of(), List.of(), List.of());
			when(client.translate(anyList())).thenReturn(List.of("ja1"))
				.thenThrow(new TranslatorQuotaExceededException("Azure Translator HTTP 403 code=403001 message=x"));

			StepResult result = service.translatePending(NOW);

			assertThat(result.success()).isFalse();
			assertThat(result.processedCount()).isEqualTo(1);
			assertThat(result.errorMessage())
				.isEqualTo("TranslatorQuotaExceededException: Azure Translator HTTP 403 code=403001 message=x");
			verify(client, times(2)).translate(anyList());
			verify(writer, times(1)).save(anyList(), anyLong(), any(), any());
			// 速さの待ち（6000 文字 → 12 秒）だけで、再試行の待ちはない
			assertThat(sleeps).containsExactly(Duration.ofSeconds(12));
			verify(mapper, never()).findUntranslatedVenueNames(anyInt());
		}

		@Test
		void throttledRetriesAfter60SecondsUpTo3Times() {
			pending(List.of("가"), List.of(), List.of(), List.of());
			TranslatorThrottledException throttled = new TranslatorThrottledException("429");
			when(client.translate(anyList())).thenThrow(throttled, throttled, throttled).thenReturn(List.of("ja"));

			StepResult result = service.translatePending(NOW);

			assertThat(result.success()).isTrue();
			assertThat(sleeps).containsExactly(Duration.ofSeconds(60), Duration.ofSeconds(60), Duration.ofSeconds(60));
			verify(writer).save(List.of(new TranslationPair("가", "ja")), 1, MONTH, NOW);
		}

		@Test
		void throttledFourTimesStops() {
			pending(List.of("가"), List.of(), List.of(), List.of());
			when(client.translate(anyList())).thenThrow(new TranslatorThrottledException("Azure Translator HTTP 429"));

			StepResult result = service.translatePending(NOW);

			assertThat(result.success()).isFalse();
			assertThat(result.errorMessage()).isEqualTo("TranslatorThrottledException: Azure Translator HTTP 429");
			verify(client, times(4)).translate(anyList());
			assertThat(sleeps).hasSize(3).allMatch(Duration.ofSeconds(60)::equals);
			verifyNoInteractions(writer);
		}

		@Test
		void otherErrorsRetryAfter2_4_8Seconds() {
			pending(List.of("가"), List.of(), List.of(), List.of());
			TranslatorException failure = new TranslatorException("Azure Translator HTTP 500");
			when(client.translate(anyList())).thenThrow(failure, failure).thenReturn(List.of("ja"));

			StepResult result = service.translatePending(NOW);

			assertThat(result.success()).isTrue();
			assertThat(sleeps).containsExactly(Duration.ofSeconds(2), Duration.ofSeconds(4));
		}

		@Test
		void fatalErrorStopsAfterOneRequestWithoutWaiting() {
			pending(List.of("가"), List.of("극장"), List.of(), List.of());
			when(client.translate(anyList()))
				.thenThrow(new TranslatorFatalException("Azure Translator HTTP 400 code=400000 message=x"));

			StepResult result = service.translatePending(NOW);

			assertThat(result.success()).isFalse();
			assertThat(result.errorMessage())
				.isEqualTo("TranslatorFatalException: Azure Translator HTTP 400 code=400000 message=x");
			verify(client, times(1)).translate(anyList());
			assertThat(sleeps).isEmpty();
			verifyNoInteractions(writer);
			verify(mapper, never()).findUntranslatedVenueNames(anyInt());
		}

		@Test
		void otherErrorsFourTimesStop() {
			pending(List.of("가"), List.of("극장"), List.of(), List.of());
			when(client.translate(anyList())).thenThrow(new TranslatorException("Azure Translator HTTP 503"));

			StepResult result = service.translatePending(NOW);

			assertThat(result.success()).isFalse();
			assertThat(result.errorMessage()).isEqualTo("TranslatorException: Azure Translator HTTP 503");
			assertThat(sleeps).containsExactly(Duration.ofSeconds(2), Duration.ofSeconds(4), Duration.ofSeconds(8));
			verify(client, times(4)).translate(anyList());
			verifyNoInteractions(writer);
			verify(mapper, never()).findUntranslatedVenueNames(anyInt());
		}

	}

	/** 実際のクライアント（応答は external/azure/ のファイル、またはキーなし）と組み合わせる */
	@Nested
	class WithRealClient {

		private static byte[] json(String name) {
			try (InputStream in = TranslationServiceTest.class.getResourceAsStream("/external/azure/" + name)) {
				return in.readAllBytes();
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Test
		void missingKeyFailsTheStep() {
			// キーがないときは送らずに失敗し、S7 は失敗として記録される（06 の 7.2: 使う時点で空なら失敗）
			CatticketProperties noKey = props("");
			AzureTranslatorClient realClient = new AzureTranslatorClient(RestClient.builder().build(), noKey);
			TranslationService s = new TranslationService(mapper, usageMapper, writer, realClient, sleeper, noKey);
			pending(List.of("가"), List.of(), List.of(), List.of());

			StepResult result = s.translatePending(NOW);

			assertThat(result.success()).isFalse();
			assertThat(result.errorMessage()).isEqualTo(
					"TranslatorFatalException: Azure Translator key is not set (catticket.azure.translator-key)");
			// 再試行しない（U-15 ③）
			assertThat(sleeps).isEmpty();
			verifyNoInteractions(writer);
		}

		@Test
		void quotaResponseFileStopsTheStep() {
			RestClient.Builder builder = RestClient.builder().baseUrl("https://translator.test");
			MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
			server.expect(ExpectedCount.once(), method(HttpMethod.POST))
				.andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
					.body(json("error-403001.json")));
			AzureTranslatorClient realClient = new AzureTranslatorClient(builder.build(), PROPS);
			TranslationService s = new TranslationService(mapper, usageMapper, writer, realClient, sleeper, PROPS);
			pending(List.of("가"), List.of(), List.of(), List.of());

			StepResult result = s.translatePending(NOW);

			server.verify();
			assertThat(result.success()).isFalse();
			assertThat(result.errorMessage()).startsWith("TranslatorQuotaExceededException:");
			assertThat(sleeps).isEmpty();
			verifyNoInteractions(writer);
		}

		@Test
		void unauthorizedResponseFileStopsWithoutRetry() {
			RestClient.Builder builder = RestClient.builder().baseUrl("https://translator.test");
			MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
			server.expect(ExpectedCount.once(), method(HttpMethod.POST))
				.andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
					.body(json("error-401000.json")));
			AzureTranslatorClient realClient = new AzureTranslatorClient(builder.build(), PROPS);
			TranslationService s = new TranslationService(mapper, usageMapper, writer, realClient, sleeper, PROPS);
			pending(List.of("가"), List.of(), List.of(), List.of());

			StepResult result = s.translatePending(NOW);

			server.verify();
			assertThat(result.success()).isFalse();
			assertThat(result.errorMessage()).startsWith("TranslatorFatalException: Azure Translator HTTP 401");
			assertThat(sleeps).isEmpty();
			verifyNoInteractions(writer);
		}

		@Test
		void successResponseFileIsSavedInInputOrder() {
			RestClient.Builder builder = RestClient.builder().baseUrl("https://translator.test");
			MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
			server.expect(ExpectedCount.once(), method(HttpMethod.POST))
				.andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
					.body(json("translate-success.json")));
			AzureTranslatorClient realClient = new AzureTranslatorClient(builder.build(), PROPS);
			TranslationService s = new TranslationService(mapper, usageMapper, writer, realClient, sleeper, PROPS);
			List<String> texts = successInputs();
			pending(texts, List.of(), List.of(), List.of());

			StepResult result = s.translatePending(NOW);

			server.verify();
			assertThat(result.success()).isTrue();
			@SuppressWarnings("unchecked")
			ArgumentCaptor<List<TranslationPair>> pairs = ArgumentCaptor.forClass(List.class);
			verify(writer).save(pairs.capture(), eq(TranslationService.charCount(texts)), eq(MONTH), eq(NOW));
			assertThat(pairs.getValue()).extracting(TranslationPair::sourceText).containsExactlyElementsOf(texts);
		}

	}

	/** translate-success.json の要素の数に合わせた原文（中身はクライアントのテストと同じでなくてよい） */
	private static List<String> successInputs() {
		int n = countItems();
		List<String> texts = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			texts.add("원문" + i);
		}
		return texts;
	}

	private static int countItems() {
		try (InputStream in = TranslationServiceTest.class.getResourceAsStream("/external/azure/translate-success.json")) {
			return tools.jackson.databind.json.JsonMapper.builder().build().readTree(in.readAllBytes()).size();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
