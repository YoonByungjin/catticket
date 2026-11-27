package com.catticket.ranking;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.catticket.auth.AuthSessionService;
import com.catticket.common.code.Lang;
import com.catticket.common.code.PeriodType;
import com.catticket.common.config.JacksonConfig;
import com.catticket.common.display.LocalizedText;
import com.catticket.common.display.Period;
import com.catticket.common.display.PerformanceStatus;
import com.catticket.common.display.PerformanceSummary;
import com.catticket.common.display.VenueRef;
import com.catticket.common.security.SecurityConfig;
import com.catticket.ranking.dto.RankingItem;
import com.catticket.ranking.dto.RankingResponse;

/**
 * API-20 GET /api/rankings（03 の API-20・7.3、06 の 4.4）。
 */
@WebMvcTest(controllers = RankingController.class)
@Import({ SecurityConfig.class, JacksonConfig.class })
class RankingControllerWebMvcTest {

	@Autowired
	MockMvc mvc;

	@MockitoBean
	AuthSessionService authSessionService;

	@MockitoBean
	RankingService rankingService;

	private static ResultActions assertValidationError(ResultActions actions) throws Exception {
		return actions.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.message").isString());
	}

	@Test
	void validRequestReturnsRankingJson() throws Exception {
		PerformanceSummary card = new PerformanceSummary("PF270001",
				new LocalizedText("오페라의 유령", "오페라의 유령", false),
				new VenueRef("FC001247", new LocalizedText("샤롯데씨어터", "샤롯데씨어터", false)),
				LocalDate.of(2026, 9, 1), LocalDate.of(2027, 2, 28), "GGGA", PerformanceStatus.ONGOING, null, true,
				null);
		given(rankingService.getRanking(PeriodType.WEEKLY, "GGGA", Lang.JA))
			.willReturn(new RankingResponse(PeriodType.WEEKLY, "GGGA",
					new Period(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 1)),
					List.of(new RankingItem(1, card))));

		mvc.perform(get("/api/rankings").param("period", "weekly").param("genre", "GGGA").param("lang", "ja"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.periodType").value("WEEKLY"))
			.andExpect(jsonPath("$.genre").value("GGGA"))
			.andExpect(jsonPath("$.period.start").value("2026-10-26"))
			.andExpect(jsonPath("$.period.end").value("2026-11-01"))
			.andExpect(jsonPath("$.items", hasSize(1)))
			.andExpect(jsonPath("$.items[0].rank").value(1))
			.andExpect(jsonPath("$.items[0].performance.performanceId").value("PF270001"))
			.andExpect(jsonPath("$.items[0].performance.title.value").value("오페라의 유령"))
			.andExpect(jsonPath("$.items[0].performance.title.original").value("오페라의 유령"))
			.andExpect(jsonPath("$.items[0].performance.title.translated").value(false))
			.andExpect(jsonPath("$.items[0].performance.venue.venueId").value("FC001247"))
			.andExpect(jsonPath("$.items[0].performance.venue.name.value").value("샤롯데씨어터"))
			.andExpect(jsonPath("$.items[0].performance.startDate").value("2026-09-01"))
			.andExpect(jsonPath("$.items[0].performance.endDate").value("2027-02-28"))
			.andExpect(jsonPath("$.items[0].performance.genre").value("GGGA"))
			.andExpect(jsonPath("$.items[0].performance.status").value("ONGOING"))
			.andExpect(jsonPath("$.items[0].performance.hasAward").value(true))
			// null の項目も省かずに出す（03 の 7.2）
			.andExpect(jsonPath("$.items[0].performance.posterUrl").hasJsonPath())
			.andExpect(jsonPath("$.items[0].performance.posterUrl").isEmpty())
			.andExpect(jsonPath("$.items[0].performance.rating").hasJsonPath())
			.andExpect(jsonPath("$.items[0].performance.rating").isEmpty());
	}

	@Test
	void genreAndLangDefaultToAllAndKo() throws Exception {
		given(rankingService.getRanking(PeriodType.DAILY, "ALL", Lang.KO))
			.willReturn(new RankingResponse(PeriodType.DAILY, "ALL", null, List.of()));

		// 表示できる期間がない（03 の 4.9）: period は null で出し、items は []
		mvc.perform(get("/api/rankings").param("period", "daily"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.periodType").value("DAILY"))
			.andExpect(jsonPath("$.genre").value("ALL"))
			.andExpect(jsonPath("$.period").hasJsonPath())
			.andExpect(jsonPath("$.period").isEmpty())
			.andExpect(jsonPath("$.items", hasSize(0)));
		verify(rankingService).getRanking(PeriodType.DAILY, "ALL", Lang.KO);
	}

	@Test
	void explicitAllAndMonthlyAreAccepted() throws Exception {
		given(rankingService.getRanking(PeriodType.MONTHLY, "ALL", Lang.KO))
			.willReturn(new RankingResponse(PeriodType.MONTHLY, "ALL", null, List.of()));

		mvc.perform(get("/api/rankings").param("period", "monthly").param("genre", "ALL").param("lang", "ko"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.periodType").value("MONTHLY"));
	}

	@Test
	void missingPeriodIsRequired() throws Exception {
		assertValidationError(mvc.perform(get("/api/rankings").param("genre", "ALL")))
			.andExpect(jsonPath("$.errors", hasSize(1)))
			.andExpect(jsonPath("$.errors[0].field").value("period"))
			.andExpect(jsonPath("$.errors[0].reason").value("REQUIRED"));
		verify(rankingService, never()).getRanking(any(), any(), any());
	}

	@Test
	void invalidPeriodIsInvalidValue() throws Exception {
		// 値は大文字・小文字を区別する（03 の API-20 の daily / weekly / monthly）
		for (String period : List.of("yearly", "DAILY", "")) {
			assertValidationError(mvc.perform(get("/api/rankings").param("period", period)))
				.andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors[0].field").value("period"))
				.andExpect(jsonPath("$.errors[0].reason").value("INVALID_VALUE"));
		}
		verify(rankingService, never()).getRanking(any(), any(), any());
	}

	@Test
	void invalidGenreIsInvalidValue() throws Exception {
		for (String genre : List.of("all", "ZZZZ", "gggA", "")) {
			assertValidationError(mvc.perform(get("/api/rankings").param("period", "daily").param("genre", genre)))
				.andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors[0].field").value("genre"))
				.andExpect(jsonPath("$.errors[0].reason").value("INVALID_VALUE"));
		}
		verify(rankingService, never()).getRanking(any(), any(), any());
	}

	@Test
	void allInvalidFieldsAreReportedTogether() throws Exception {
		assertValidationError(mvc.perform(get("/api/rankings").param("genre", "ZZZZ").param("lang", "en")))
			.andExpect(jsonPath("$.errors", hasSize(3)))
			.andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("period", "genre", "lang")))
			.andExpect(
					jsonPath("$.errors[*].reason", containsInAnyOrder("REQUIRED", "INVALID_VALUE", "INVALID_VALUE")));
	}

}
