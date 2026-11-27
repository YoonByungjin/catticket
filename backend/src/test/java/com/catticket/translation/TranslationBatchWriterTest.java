package com.catticket.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.annotation.Transactional;

import com.catticket.common.text.Hashes;
import com.catticket.translation.row.TranslationInsert;

/**
 * TranslationBatchWriter（06 の 4.10 の 5.）。Mapper はモック。
 */
class TranslationBatchWriterTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 6, 0, 0);

	private static final LocalDate MONTH = LocalDate.of(2026, 11, 1);

	private final TranslationMapper mapper = mock(TranslationMapper.class);

	private final TranslationUsageMapper usageMapper = mock(TranslationUsageMapper.class);

	private final TranslationBatchWriter writer = new TranslationBatchWriter(mapper, usageMapper);

	@Test
	void insertsTranslationsWithHashThenAddsUsage() {
		writer.save(List.of(new TranslationPair("레미제라블", "レ・ミゼラブル"), new TranslationPair("극장", "劇場")), 7, MONTH,
				NOW);

		InOrder order = inOrder(mapper, usageMapper);
		order.verify(mapper)
			.insertTranslations(List.of(new TranslationInsert(Hashes.sha256Hex("레미제라블"), "레미제라블", "レ・ミゼラブル", NOW),
					new TranslationInsert(Hashes.sha256Hex("극장"), "극장", "劇場", NOW)));
		order.verify(usageMapper).addUsage(MONTH, 7, NOW);
	}

	@Test
	void emptyPairsAreRejected() {
		assertThatThrownBy(() -> writer.save(List.of(), 0, MONTH, NOW)).isInstanceOf(IllegalArgumentException.class);
		verifyNoInteractions(mapper, usageMapper);
	}

	@Test
	void saveIsOneTransaction() throws NoSuchMethodException {
		Method save = TranslationBatchWriter.class.getMethod("save", List.class, long.class, LocalDate.class,
				LocalDateTime.class);

		Transactional tx = save.getAnnotation(Transactional.class);
		assertThat(tx).isNotNull();
		assertThat(tx.readOnly()).isFalse();
	}

	@Test
	void translatePendingHasNoTransaction() throws NoSuchMethodException {
		// Azure を待つ間に DB の接続を持たない（06 の 6.1）
		Method method = TranslationService.class.getMethod("translatePending", LocalDateTime.class);

		assertThat(method.getAnnotation(Transactional.class)).isNull();
		assertThat(TranslationService.class.getAnnotation(Transactional.class)).isNull();
	}

}
