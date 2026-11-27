package com.catticket.translation;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.catticket.common.text.Hashes;
import com.catticket.translation.row.TranslationRow;

/**
 * API 用の翻訳の参照（06 の 4.10）。原文の集まりを1回の照会で引く（1件ずつ引かない）。
 */
@Service
public class TranslationLookupService {

	private final TranslationMapper mapper;

	public TranslationLookupService(TranslationMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * @return 原文 → 日本語の翻訳。翻訳がない原文は入らない。null・空の文は引かない
	 */
	@Transactional(readOnly = true)
	public Map<String, String> lookup(Collection<String> originals) {
		Map<String, String> originalByHash = new LinkedHashMap<>();
		if (originals != null) {
			for (String original : originals) {
				// 空の文は S7 の対象にならず翻訳がない
				if (original != null && !original.isEmpty()) {
					originalByHash.putIfAbsent(Hashes.sha256Hex(original), original);
				}
			}
		}
		if (originalByHash.isEmpty()) {
			return Map.of();
		}
		Map<String, String> result = new HashMap<>();
		for (TranslationRow row : mapper.findByHashes(originalByHash.keySet())) {
			String original = originalByHash.get(row.sourceHash());
			if (original != null) {
				result.put(original, row.translatedText());
			}
		}
		return result;
	}

}
