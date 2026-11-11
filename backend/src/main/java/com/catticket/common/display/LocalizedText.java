package com.catticket.common.display;

/**
 * 外部データの文（03 の 7.2）。value は表示する文、original は韓国語の原文、translated は value が翻訳か。
 */
public record LocalizedText(String value, String original, boolean translated) {
}
