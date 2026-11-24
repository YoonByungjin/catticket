package com.catticket.translation;

/**
 * 原文と訳文の組（S7 の1つの束の中の1つ）。
 */
public record TranslationPair(String sourceText, String translatedText) {
}
