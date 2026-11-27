package com.catticket.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.catticket.common.batch.BatchContext;
import com.catticket.common.batch.StepResult;
import com.catticket.common.batch.StepSummary;

/**
 * TranslationStep（S7。06 の 5.3）。ジョブの開始時刻で TranslationService を呼ぶ。
 */
class TranslationStepTest {

	@Test
	void runsTranslationWithTheJobTime() {
		LocalDateTime now = LocalDateTime.of(2026, 11, 24, 6, 0, 0);
		TranslationService service = mock(TranslationService.class);
		StepResult expected = StepResult.success(3, new StepSummary());
		when(service.translatePending(now)).thenReturn(expected);

		StepResult result = new TranslationStep(service).run(new BatchContext(1L, now, LocalDate.of(2026, 11, 24)));

		assertThat(result).isSameAs(expected);
		assertThat(TranslationStep.NAME).isEqualTo("S7");
	}

}
