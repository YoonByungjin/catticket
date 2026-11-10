import { ko } from './messages.ko';

/** 表示言語（F-10、N-01） */
export type Lang = 'ko' | 'ja';

export const LANGS: readonly Lang[] = ['ko', 'ja'];

/** 初めて開いたときの表示言語（F-10 ①） */
export const DEFAULT_LANG: Lang = 'ko';

/** 辞書のキー。韓国語の辞書のキーを正とする */
export type MessageKey = keyof typeof ko;

/** 1つの言語の辞書。キーの過不足は型エラーになる */
export type Messages = Record<MessageKey, string>;

export function isLang(value: unknown): value is Lang {
  return value === 'ko' || value === 'ja';
}
