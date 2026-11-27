import { LocalizedText } from '../../core/api/api.types';
import { Lang } from '../../core/i18n/messages';

/** 外部データの文の表示の形（02 の 3.4・P-06、03 の 7.2） */
export interface LocalizedDisplay {
  /** 大きく出す文 */
  value: string;
  /** 下に小さく併記する原文。併記しないときは null */
  original: string | null;
  /** 翻訳がないことを示す「原文」の印を出すか（日本語表示だけ） */
  untranslated: boolean;
}

/**
 * LocalizedText をどう出すかを決める。
 * withOriginal は公演名・会場名・駅名（翻訳の下に原文を併記する項目。F-11 ②）のとき true。
 * 翻訳がないときは原文を1回だけ出し「原文」と示す（P-06）。韓国語表示では印を出さない。
 */
export function localizedDisplay(
  text: LocalizedText,
  lang: Lang,
  withOriginal: boolean,
): LocalizedDisplay {
  if (lang === 'ko') {
    return { value: text.value, original: null, untranslated: false };
  }
  if (!text.translated) {
    return { value: text.value, original: null, untranslated: true };
  }
  return { value: text.value, original: withOriginal ? text.original : null, untranslated: false };
}
