package com.catticket.translation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.catticket.common.text.Hashes;
import com.catticket.translation.row.TranslationInsert;

/**
 * S7 の束1つの保存（06 の 4.10 の 5.、6.4）。翻訳と月の使用量を同じトランザクションで書く
 * （どちらかだけが残ると、月の数が実際とずれるため）。
 */
@Service
public class TranslationBatchWriter {

	private final TranslationMapper mapper;

	private final TranslationUsageMapper usageMapper;

	public TranslationBatchWriter(TranslationMapper mapper, TranslationUsageMapper usageMapper) {
		this.mapper = mapper;
		this.usageMapper = usageMapper;
	}

	/**
	 * @param chars 送った文字数（コードポイント）。今月の数に足す
	 * @param month 月の1日（Asia/Seoul。05 の 2.）
	 */
	@Transactional
	public void save(List<TranslationPair> pairs, long chars, LocalDate month, LocalDateTime now) {
		if (pairs.isEmpty()) {
			throw new IllegalArgumentException("pairs must not be empty");
		}
		List<TranslationInsert> rows = pairs.stream()
			.map(p -> new TranslationInsert(Hashes.sha256Hex(p.sourceText()), p.sourceText(), p.translatedText(), now))
			.toList();
		mapper.insertTranslations(rows);
		usageMapper.addUsage(month, chars, now);
	}

}
