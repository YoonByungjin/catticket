package com.catticket.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.catticket.common.text.Hashes;
import com.catticket.translation.row.TranslationRow;

/**
 * TranslationLookupService（06 の 4.10）。1回の照会で引くこと（N+1 にしない）を確かめる。
 */
class TranslationLookupServiceTest {

	private final TranslationMapper mapper = mock(TranslationMapper.class);

	private final TranslationLookupService service = new TranslationLookupService(mapper);

	@Test
	void looksUpAllOriginalsInOneQuery() {
		when(mapper.findByHashes(any())).thenReturn(List.of(new TranslationRow(Hashes.sha256Hex("공연"), "公演"),
				new TranslationRow(Hashes.sha256Hex("극장"), "劇場")));

		Map<String, String> result = service.lookup(Arrays.asList("공연", "극장", "번역없음", "공연", null, ""));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Collection<String>> hashes = ArgumentCaptor.forClass(Collection.class);
		verify(mapper, times(1)).findByHashes(hashes.capture());
		// 同じ原文は1つ、null・空は引かない
		assertThat(hashes.getValue()).containsExactly(Hashes.sha256Hex("공연"), Hashes.sha256Hex("극장"),
				Hashes.sha256Hex("번역없음"));
		assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of("공연", "公演", "극장", "劇場"));
	}

	@Test
	void nothingToLookUpDoesNotQuery() {
		assertThat(service.lookup(Arrays.asList(null, ""))).isEmpty();
		assertThat(service.lookup(List.of())).isEmpty();
		assertThat(service.lookup(null)).isEmpty();
		verifyNoInteractions(mapper);
	}

}
