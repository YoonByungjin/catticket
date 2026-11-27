package com.catticket.translation;

import org.springframework.stereotype.Component;

import com.catticket.common.batch.BatchContext;
import com.catticket.common.batch.StepResult;

/**
 * B-01 の S7 翻訳（05 の 3.7、06 の 5.3）。DailyCollectJob が S6 の後に呼ぶ。
 */
@Component
public class TranslationStep {

	public static final String NAME = "S7";

	private final TranslationService service;

	public TranslationStep(TranslationService service) {
		this.service = service;
	}

	public StepResult run(BatchContext ctx) {
		return service.translatePending(ctx.now());
	}

}
