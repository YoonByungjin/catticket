import { Period, PerformanceSummary } from '../../core/api/api.types';
import { ALL_GENRES, GenreCode } from '../../core/code/genre';
import { Lang } from '../../core/i18n/messages';

/** 期間の種類のクエリの値（03 の API-20 の period） */
export type RankingPeriod = 'daily' | 'weekly' | 'monthly';

export const RANKING_PERIODS: readonly RankingPeriod[] = ['daily', 'weekly', 'monthly'];

export function isRankingPeriod(value: unknown): value is RankingPeriod {
  return value === 'daily' || value === 'weekly' || value === 'monthly';
}

/** ジャンルのクエリの値（`ALL` またはジャンルのコード） */
export type RankingGenre = typeof ALL_GENRES | GenreCode;

/** API-20 GET /api/rankings のクエリ */
export interface RankingQuery {
  period: RankingPeriod;
  genre: RankingGenre;
  lang: Lang;
}

/** API-20 のレスポンス（03 の 8.4） */
export interface RankingResponse {
  periodType: 'DAILY' | 'WEEKLY' | 'MONTHLY';
  genre: string;
  /** 表示する期間の集計期間。表示できる期間がないとき null（03 の 4.9） */
  period: Period | null;
  /** 最大 10。rank の順 */
  items: RankingItem[];
}

export interface RankingItem {
  /** 表示できない公演を除いて振り直した順位（1〜10。03 の 9.1） */
  rank: number;
  performance: PerformanceSummary;
}
