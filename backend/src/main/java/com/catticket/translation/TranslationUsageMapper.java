package com.catticket.translation;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;

/**
 * 翻訳の月（Asia/Seoul）の文字数（mapper/translation/TranslationUsageMapper.xml。04 の translation_usage）。
 */
@Mapper
public interface TranslationUsageMapper {

	/** month は月の1日。行がなければ 0 */
	long findChars(LocalDate month);

	/** その月の数に足す（行がなければ作る） */
	void addUsage(LocalDate month, long chars, LocalDateTime now);

}
