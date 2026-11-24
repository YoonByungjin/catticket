package com.catticket.collect;

import java.util.List;

/**
 * ProviderValues の変換の結果。value が null なら保存しない（notes に理由がある）。
 */
public record Conversion<T>(T value, List<CollectNote> notes) {

	public Conversion {
		notes = List.copyOf(notes);
	}

	public boolean skipped() {
		return value == null;
	}

}
