package com.catticket.collect;

import java.util.List;

/**
 * 公演1件の保存の結果（PerformanceSaveService.saveFromKopis）。notes を StepSummary に数える。
 */
public record SaveOutcome(boolean saved, List<CollectNote> notes) {

	public SaveOutcome {
		notes = List.copyOf(notes);
	}

}
