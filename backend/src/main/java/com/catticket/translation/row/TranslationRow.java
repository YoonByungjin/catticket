package com.catticket.translation.row;

/**
 * 翻訳の1行（findByHashes の結果。04 の translation）。
 */
public record TranslationRow(String sourceHash, String translatedText) {
}
