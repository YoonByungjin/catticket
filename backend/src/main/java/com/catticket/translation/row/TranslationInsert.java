package com.catticket.translation.row;

import java.time.LocalDateTime;

/**
 * 入れる翻訳（04 の translation）。sourceHash は sourceText の UTF-8 の SHA-256（小文字の16進。04 の 11.2.7）。
 */
public record TranslationInsert(String sourceHash, String sourceText, String translatedText, LocalDateTime createdAt) {
}
