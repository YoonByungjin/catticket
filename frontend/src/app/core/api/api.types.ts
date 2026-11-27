/**
 * API の共通の型（03 の 7.2・7.3・API-30）。API の呼び出しは機能ごとのサービスで行う（Wave 2）。
 */

/** エラーの本文（03 の 7.3）。画面は code と errors だけを見る（message は表示しない） */
export interface ApiError {
  code: string;
  message: string;
  errors: ApiFieldError[];
}

export interface ApiFieldError {
  field: string;
  reason: string;
}

/** 役割（API-30） */
export type UserRole = 'USER' | 'ADMIN';

/** API-30 GET /api/auth/me のレスポンス */
export interface AuthMeResponse {
  authenticated: boolean;
  role: UserRole | null;
  /** 一般ユーザーのニックネーム。未ログイン・管理者は null */
  nickname: string | null;
}

/** 外部データの文（03 の 7.2 LocalizedText。表示は 02 の 3.4） */
export interface LocalizedText {
  /** 表示する文。lang=ja で翻訳があれば翻訳、それ以外は韓国語の原文 */
  value: string;
  /** 韓国語の原文 */
  original: string;
  /** value が翻訳か。lang=ko ではいつも false */
  translated: boolean;
}

/** 集計期間（03 の 7.2 Period）。日次は start = end。どちらも YYYY-MM-DD */
export interface Period {
  start: string;
  end: string;
}

/** 評価（03 の 7.2 Rating） */
export interface Rating {
  /** 平均（小数第1位）。count が 0 のとき null */
  average: number | null;
  count: number;
}

/** 公演の状態（03 の 7.2） */
export type PerformanceStatus = 'ONGOING' | 'UPCOMING' | 'ENDED';

/** 公演のカード（03 の 7.2 PerformanceSummary、02 の 3.7） */
export interface PerformanceSummary {
  performanceId: string;
  title: LocalizedText;
  venue: { venueId: string; name: LocalizedText };
  /** YYYY-MM-DD */
  startDate: string;
  /** YYYY-MM-DD */
  endDate: string;
  /** ジャンルのコード（KOPIS。表示は画面の辞書） */
  genre: string;
  status: PerformanceStatus;
  posterUrl: string | null;
  hasAward: boolean;
  /** API-20・21 では null（02 の SC-03 のカードは評価を除く） */
  rating: Rating | null;
}
