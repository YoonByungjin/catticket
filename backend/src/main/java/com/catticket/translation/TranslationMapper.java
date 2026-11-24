package com.catticket.translation;

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.catticket.translation.row.TranslationInsert;
import com.catticket.translation.row.TranslationRow;

/**
 * 翻訳の読み書き（mapper/translation/TranslationMapper.xml。06 の 4.10）。
 * 未翻訳の読みは 05 の 3.7 の種類ごと。同じ原文は1つにまとめ、空の文・NULL は返さない。
 */
@Mapper
public interface TranslationMapper {

	/** hashes が空なら呼ばない（空の IN は SQL の誤り） */
	List<TranslationRow> findByHashes(Collection<String> hashes);

	/** 公演名（自動収集の値と管理者の修正の値） */
	List<String> findUntranslatedTitles(int limit);

	List<String> findUntranslatedVenueNames(int limit);

	/** 公式の日本語表記がない駅名 */
	List<String> findUntranslatedStationNames(int limit);

	/** それ以外（出演者・価格・公演時間・上演時間・観覧年齢・名前のある予約サイト・会場の住所・コインロッカーの位置） */
	List<String> findUntranslatedOthers(int limit);

	/** 同じ原文がすでにあれば何もしない。rows が空なら呼ばない */
	void insertTranslations(List<TranslationInsert> rows);

}
