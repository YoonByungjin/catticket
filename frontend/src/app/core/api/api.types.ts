/**
 * API の共通の型（03 の 7.3・API-30）。API の呼び出しは機能ごとのサービスで行う（Wave 2）。
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
