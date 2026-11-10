package com.catticket.common.error;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.catticket.auth.AuthSessionService;
import com.catticket.common.config.JacksonConfig;
import com.catticket.common.security.SecurityConfig;
import com.catticket.common.support.TestApiController;

/**
 * 03 の 7.3 のエラーの本文・コード（06 の 3.2 の表）、JSON だけにする設定（06 の 3.1 のテスト ①〜③）、
 * JSON の型の厳しさ（06 の 3.3、U-01）を確かめる。
 */
@WebMvcTest(controllers = TestApiController.class)
@Import({ SecurityConfig.class, JacksonConfig.class })
class ErrorResponseWebMvcTest {

	@Autowired
	MockMvc mvc;

	@MockitoBean
	AuthSessionService authSessionService;

	private ResultActions postJson(String body) throws Exception {
		return mvc.perform(post("/api/test/body").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private static void assertError(ResultActions actions, int status, String code) throws Exception {
		actions.andExpect(status().is(status))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.code").value(code))
			.andExpect(jsonPath("$.message").isString())
			.andExpect(jsonPath("$.errors").isArray());
	}

	@Nested
	class JsonOnly {

		@Test
		void xmlBodyIs415() throws Exception {
			// ① XML の本文
			ResultActions actions = mvc.perform(post("/api/test/body").with(csrf())
				.contentType(MediaType.APPLICATION_XML)
				.content("<TestBody><name>cat</name></TestBody>"));
			assertError(actions, 415, "UNSUPPORTED_MEDIA_TYPE");
			actions.andExpect(jsonPath("$.errors", hasSize(0)));
		}

		@Test
		void acceptXmlStillReturnsJson() throws Exception {
			// ② Accept: application/xml でも 200 の JSON
			mvc.perform(get("/api/test/time").accept(MediaType.APPLICATION_XML))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.date").value("2026-11-05"));
		}

		@Test
		void textPlainBodyIs415() throws Exception {
			// ③ text/plain の本文
			assertError(mvc.perform(post("/api/test/body").with(csrf())
				.contentType(MediaType.TEXT_PLAIN)
				.content("name=cat")), 415, "UNSUPPORTED_MEDIA_TYPE");
		}

		@Test
		void dateTimeIsIsoWithSeoulOffsetAndSeconds() throws Exception {
			// 03 の 7.2: "2026-11-05T14:03:00+09:00"（秒が 0 でも秒まで出す）
			mvc.perform(get("/api/test/time"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.at").value("2026-11-05T14:03:00+09:00"));
		}

	}

	@Nested
	class StrictJsonTypes {

		@Test
		void validBodyIsAccepted() throws Exception {
			postJson("{\"name\":\"cat\",\"count\":1,\"flag\":true,\"ratio\":1.5,\"unknown\":\"ignored\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(1))
				.andExpect(jsonPath("$.flag").value(true));
		}

		@Test
		void stringForIntegerIsMalformed() throws Exception {
			assertError(postJson("{\"name\":\"cat\",\"count\":\"1\"}"), 400, "MALFORMED_REQUEST");
		}

		@Test
		void stringForDecimalIsMalformed() throws Exception {
			assertError(postJson("{\"name\":\"cat\",\"ratio\":\"1.5\"}"), 400, "MALFORMED_REQUEST");
		}

		@Test
		void stringForBooleanIsMalformed() throws Exception {
			assertError(postJson("{\"name\":\"cat\",\"flag\":\"true\"}"), 400, "MALFORMED_REQUEST");
		}

		@Test
		void numberForBooleanIsMalformed() throws Exception {
			assertError(postJson("{\"name\":\"cat\",\"flag\":1}"), 400, "MALFORMED_REQUEST");
		}

		@Test
		void decimalForIntegerIsMalformed() throws Exception {
			assertError(postJson("{\"name\":\"cat\",\"count\":1.5}"), 400, "MALFORMED_REQUEST");
		}

		@Test
		void emptyStringForIntegerIsMalformed() throws Exception {
			// 空の文字列を null として通さない（03 の 7.1）
			assertError(postJson("{\"name\":\"cat\",\"count\":\"\"}"), 400, "MALFORMED_REQUEST");
		}

		@Test
		void blankStringForDecimalIsMalformed() throws Exception {
			assertError(postJson("{\"name\":\"cat\",\"ratio\":\" \"}"), 400, "MALFORMED_REQUEST");
		}

		@Test
		void emptyStringForBooleanIsMalformed() throws Exception {
			assertError(postJson("{\"name\":\"cat\",\"flag\":\"\"}"), 400, "MALFORMED_REQUEST");
		}

		@Test
		void blankStringForIntegerAndBooleanIsMalformed() throws Exception {
			assertError(postJson("{\"name\":\"cat\",\"count\":\"  \"}"), 400, "MALFORMED_REQUEST");
			assertError(postJson("{\"name\":\"cat\",\"flag\":\" \"}"), 400, "MALFORMED_REQUEST");
		}

		@Test
		void brokenJsonIsMalformed() throws Exception {
			assertError(postJson("{\"name\":"), 400, "MALFORMED_REQUEST");
		}

		@Test
		void malformedMessageDoesNotLeakDetails() throws Exception {
			postJson("{\"name\":\"cat\",\"count\":\"1\"}")
				.andExpect(jsonPath("$.message", not(containsString("Integer"))))
				.andExpect(jsonPath("$.message", not(containsString("Exception"))));
		}

	}

	@Nested
	class Validation {

		@Test
		void bodyConstraintsListEveryField() throws Exception {
			ResultActions actions = postJson("{\"name\":null,\"tags\":[\"ok\",\"toolong\"]}");
			assertError(actions, 400, "VALIDATION_FAILED");
			// 本文は経路のまま（添字を残す）
			actions.andExpect(jsonPath("$.errors", hasSize(2)))
				.andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("name", "tags[1]")))
				.andExpect(jsonPath("$.errors[*].reason", containsInAnyOrder("REQUIRED", "LENGTH")));
		}

		@Test
		void codePointsAreCountedNotUtf16Units() throws Exception {
			// 絵文字 5 個は UTF-16 では 10 単位だが、コードポイントでは 5 文字なので通る
			postJson("{\"name\":\"🐱🐱🐱🐱🐱\"}")
				.andExpect(status().isOk());
		}

		@Test
		void queryRecordRangeAndInvalidValue() throws Exception {
			ResultActions actions = mvc.perform(get("/api/test/query").param("page", "10001").param("size", "0"));
			assertError(actions, 400, "VALIDATION_FAILED");
			actions.andExpect(jsonPath("$.errors", hasSize(2)))
				.andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("page", "size")))
				.andExpect(jsonPath("$.errors[*].reason", containsInAnyOrder("RANGE", "RANGE")));

			// クエリは添字を除く（genre[1] → genre）
			mvc.perform(get("/api/test/query").param("genre", "AAAA", "XXXX"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors[0].field").value("genre"))
				.andExpect(jsonPath("$.errors[0].reason").value("INVALID_VALUE"));

			mvc.perform(get("/api/test/query").param("lang", "en"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("lang"))
				.andExpect(jsonPath("$.errors[0].reason").value("INVALID_VALUE"));
		}

		@Test
		void queryRecordsAreAggregatedWithMethodValidation() throws Exception {
			// 引数に直接の制約があると、複数のクエリの record の誤りが HandlerMethodValidationException にまとまる
			ResultActions actions = mvc.perform(get("/api/test/query-all").param("page", "10001")
				.param("size", "0")
				.param("lang", "en")
				.param("genre", "AAAA", "XXXX")
				.param("limit", "0"));
			assertError(actions, 400, "VALIDATION_FAILED");
			actions.andExpect(jsonPath("$.errors", hasSize(5)))
				.andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("page", "size", "lang", "genre", "limit")))
				.andExpect(jsonPath("$.errors[*].reason",
						containsInAnyOrder("RANGE", "RANGE", "INVALID_VALUE", "INVALID_VALUE", "RANGE")));
		}

		@Test
		void queryRecordsAreAggregatedWithNotNullPageQuery() throws Exception {
			// 06 の 3.3（D-4）: @Valid @NotNull PageQuery でメソッドの検証になり、2つの record の誤りが1つの応答にまとまる
			ResultActions actions = mvc.perform(get("/api/test/query-d4").param("size", "101")
				.param("lang", "en")
				.param("district", "종로구", "JONGNO"));
			assertError(actions, 400, "VALIDATION_FAILED");
			actions.andExpect(jsonPath("$.errors", hasSize(3)))
				.andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("size", "lang", "district")))
				.andExpect(jsonPath("$.errors[*].reason", containsInAnyOrder("RANGE", "INVALID_VALUE", "INVALID_VALUE")));

			mvc.perform(get("/api/test/query-d4").param("district", "종로구", "중구"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.district", hasSize(2)))
				.andExpect(jsonPath("$.page").value(0));
		}

		@Test
		void districtIsComparedByKoreanName() throws Exception {
			mvc.perform(get("/api/test/query").param("district", "종로구")).andExpect(status().isOk());
			for (String value : new String[] { "JONGNO", "jongno", "종로", "수원시" }) {
				mvc.perform(get("/api/test/query").param("district", value))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.errors[0].field").value("district"))
					.andExpect(jsonPath("$.errors[0].reason").value("INVALID_VALUE"));
			}
		}

		@Test
		void queryTypeMismatchIsType() throws Exception {
			ResultActions actions = mvc.perform(get("/api/test/query").param("page", "abc"));
			assertError(actions, 400, "VALIDATION_FAILED");
			actions.andExpect(jsonPath("$.errors[0].field").value("page"))
				.andExpect(jsonPath("$.errors[0].reason").value("TYPE"));
		}

		@Test
		void queryDefaultsAndDuplicates() throws Exception {
			mvc.perform(get("/api/test/query").param("genre", "GGGA", "GGGA", "AAAA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.lang").value("KO"))
				.andExpect(jsonPath("$.genre", hasSize(2)));
			mvc.perform(get("/api/test/query").param("lang", "ja"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lang").value("JA"));
		}

		@Test
		void requestParamConstraintIsValidationFailed() throws Exception {
			ResultActions actions = mvc.perform(get("/api/test/param").param("n", "11"));
			assertError(actions, 400, "VALIDATION_FAILED");
			actions.andExpect(jsonPath("$.errors[0].field").value("n"))
				.andExpect(jsonPath("$.errors[0].reason").value("RANGE"));
		}

		@Test
		void methodValidationUsesRequestNamesNotJavaNames() throws Exception {
			ResultActions actions = mvc.perform(get("/api/test/named/0").param("max", "11"));
			assertError(actions, 400, "VALIDATION_FAILED");
			actions.andExpect(jsonPath("$.errors", hasSize(2)))
				.andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("no", "max")))
				.andExpect(jsonPath("$.errors[*].reason", containsInAnyOrder("RANGE", "RANGE")));
		}

		@Test
		void missingRequestParamIsRequired() throws Exception {
			ResultActions actions = mvc.perform(get("/api/test/param"));
			assertError(actions, 400, "VALIDATION_FAILED");
			actions.andExpect(jsonPath("$.errors[0].field").value("n"))
				.andExpect(jsonPath("$.errors[0].reason").value("REQUIRED"));
		}

		@Test
		void pathTypeMismatchIsType() throws Exception {
			ResultActions actions = mvc.perform(get("/api/test/path/abc"));
			assertError(actions, 400, "VALIDATION_FAILED");
			actions.andExpect(jsonPath("$.errors[0].field").value("id"))
				.andExpect(jsonPath("$.errors[0].reason").value("TYPE"));
		}

		@Test
		void requestValidationExceptionKeepsGivenItems() throws Exception {
			ResultActions actions = mvc.perform(get("/api/test/request-validation"));
			assertError(actions, 400, "VALIDATION_FAILED");
			actions.andExpect(jsonPath("$.errors", hasSize(2)))
				.andExpect(jsonPath("$.errors[0].field").value("bookingSites[0].url"))
				.andExpect(jsonPath("$.errors[0].reason").value("FORMAT"));
		}

	}

	@Nested
	class OtherErrors {

		@Test
		void apiExceptionUsesItsCode() throws Exception {
			ResultActions actions = mvc.perform(get("/api/test/api-error"));
			assertError(actions, 404, "PERFORMANCE_NOT_FOUND");
			actions.andExpect(jsonPath("$.message").value("Performance not found"))
				.andExpect(jsonPath("$.errors", hasSize(0)));
		}

		@Test
		void unknownPathIsNotFound() throws Exception {
			assertError(mvc.perform(get("/api/no-such-path")), 404, "NOT_FOUND");
		}

		@Test
		void wrongMethodIsMethodNotAllowed() throws Exception {
			assertError(mvc.perform(delete("/api/test/time").with(csrf())), 405, "METHOD_NOT_ALLOWED");
		}

		@Test
		void unexpectedExceptionIsInternalErrorWithoutDetails() throws Exception {
			ResultActions actions = mvc.perform(get("/api/test/boom"));
			assertError(actions, 500, "INTERNAL_ERROR");
			actions.andExpect(jsonPath("$.message").value("Internal server error"))
				.andExpect(content().string(not(containsString("secret"))))
				.andExpect(content().string(not(containsString("IllegalStateException"))));
		}

	}

}
