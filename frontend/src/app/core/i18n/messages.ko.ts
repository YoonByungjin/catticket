/**
 * 画面の文言の辞書（韓国語）。キーの正はこのファイルで、日本語（messages.ja.ts）は同じキーを型で強制する。
 * 文言の出典は 02（画面設計書）・07（UI デザインガイド）。
 * `{name}` は LanguageService.t の引数で置き換える。
 */
export const ko = {
  // CM-01 ヘッダー（02 の 3.1、07 の 3.・4.1）
  'header.nav.map': '지도',
  'header.nav.rankings': '인기·트렌드',
  'header.nav.favorites': '즐겨찾기',
  // {name} はニックネーム（02 の 3.1、07 の 4.1。Issue #24 の決定4: 半角スペースと半角の括弧）
  'header.nav.account': '계정 ({name})',
  'header.nav.admin': '관리',
  'header.login': '로그인',
  'header.signup': '회원가입',
  'header.logout': '로그아웃',
  'header.language': '표시 언어',
  'header.menu': '메뉴',
  'header.theme.toDark': '다크 표시로 전환',
  'header.theme.toLight': '라이트 표시로 전환',

  // CM-03 通知（02 の 3.1・3.5）
  'notify.loggedOut': '로그아웃했습니다',
  'notify.sessionExpired': '일정 시간 조작이 없어 로그아웃되었습니다',

  // SC-99 エラー（02 の SC-99）
  'error.pageNotFound': '페이지를 찾을 수 없습니다',
  'error.performanceNotFound': '공연을 찾을 수 없습니다',
  'error.forbidden': '이 화면을 볼 권한이 없습니다',
  'error.goToMap': '지도로 이동',
} as const satisfies Record<string, string>;
