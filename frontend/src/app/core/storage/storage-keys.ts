/**
 * localStorage のキー。
 * src/index.html のインラインスクリプトも同じキーを読むので、変えるときは両方を直す。
 */
export const STORAGE_KEYS = {
  /** 表示言語（F-10）。値は 'ko' | 'ja' */
  language: 'catticket.lang',
  /** ライト表示・ダーク表示（F-36）。値は 'light' | 'dark' */
  colorScheme: 'catticket.colorScheme',
} as const;
