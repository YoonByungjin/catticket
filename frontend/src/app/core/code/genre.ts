/**
 * ジャンルのコード（KOPIS）。03 の 1.2: API はコードで返し、表示の文言は画面の辞書（genre.{コード}）で訳す。
 * 値はバックエンドの common.code.Genre と同じ（05 の 3.4 で分かっている 9 個。確定は 05 の 7）。
 * 並びは画面の選択肢の順。
 */
export const GENRE_CODES = [
  'AAAA',
  'GGGA',
  'CCCA',
  'CCCC',
  'CCCD',
  'BBBC',
  'BBBE',
  'EEEA',
  'EEEB',
] as const;

export type GenreCode = (typeof GENRE_CODES)[number];

/** 人気順位などの「全体」（03 の API-20 の genre の既定） */
export const ALL_GENRES = 'ALL';

export function isGenreCode(value: unknown): value is GenreCode {
  return typeof value === 'string' && (GENRE_CODES as readonly string[]).includes(value);
}
