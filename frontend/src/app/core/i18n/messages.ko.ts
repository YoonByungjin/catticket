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

  // CM-02 フッターの出典のリンクの中の見えない文言（02 の 3.2・3.4、07 の 4.2.1）。出典の原文ではないので翻訳する
  'attribution.newTab': '(새 탭에서 열림)',

  // CM-03 通知（02 の 3.1・3.5）
  'notify.loggedOut': '로그아웃했습니다',
  'notify.sessionExpired': '일정 시간 조작이 없어 로그아웃되었습니다',

  // SC-99 エラー（02 の SC-99）
  'error.pageNotFound': '페이지를 찾을 수 없습니다',
  'error.performanceNotFound': '공연을 찾을 수 없습니다',
  'error.forbidden': '이 화면을 볼 권한이 없습니다',
  'error.goToMap': '지도로 이동',

  // 状態の表示（02 の 3.6、07 の 4.3）
  'state.loadError': '불러오지 못했습니다',
  'state.reload': '다시 불러오기',

  // 翻訳がない項目の印（02 の 3.4 の P-06、07 の 5.）
  'text.original': '원문',

  // 公演のカード（02 の 3.7、07 の 4.4）。状態は F-09 ③
  'performance.status.ONGOING': '공연 중',
  'performance.status.UPCOMING': '공연 예정',
  'performance.status.ENDED': '종료',
  'performance.award': '수상작',

  // ジャンル（03 の 1.2: コードを画面の辞書で訳す）。韓国語は KOPIS のジャンルの名前（genrenm）
  'genre.ALL': '전체',
  'genre.AAAA': '연극',
  'genre.GGGA': '뮤지컬',
  'genre.CCCA': '서양음악(클래식)',
  'genre.CCCC': '한국음악(국악)',
  'genre.CCCD': '대중음악',
  'genre.BBBC': '무용',
  'genre.BBBE': '대중무용',
  'genre.EEEA': '복합',
  'genre.EEEB': '서커스/마술',

  // SC-03 人気・トレンド（02 の SC-03、07 の 4.6）
  'rankings.title': '인기·트렌드',
  'rankings.tab.ranking': '인기 순위',
  'rankings.period': '기간',
  'rankings.period.daily': '일간',
  'rankings.period.weekly': '주간',
  'rankings.period.monthly': '월간',
  'rankings.genre': '장르',
  'rankings.aggregationPeriod': '집계 기간',
  // 順位の数字の読み上げ（見えない文言）
  'rankings.rank': '{rank}위',
  'rankings.empty': '이 기간의 순위 데이터가 없습니다',
} as const satisfies Record<string, string>;
