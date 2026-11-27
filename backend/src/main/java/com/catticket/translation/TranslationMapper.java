package com.catticket.translation;

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.catticket.translation.row.TranslationRow;

/**
 * 翻訳の読み書き（mapper/translation/TranslationMapper.xml。06 の 4.10）。
 * 未翻訳の読みは 05 の 3.7 の種類ごと。同じ原文は1つにまとめ、空の文・NULL は返さない。
 */
@Mapper
public interface TranslationMapper {

	/** hashes が空なら呼ばない（空の IN は SQL の誤り） */
	List<TranslationRow> findByHashes(Collection<String> hashes);

}
