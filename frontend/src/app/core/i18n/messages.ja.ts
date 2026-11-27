import { Messages } from './messages';

/**
 * 画面の文言の辞書（日本語）。キーは messages.ko.ts と同じでなければ型エラーになる。
 */
export const ja: Messages = {
  'header.nav.map': '地図',
  'header.nav.rankings': '人気・トレンド',
  'header.nav.favorites': 'お気に入り',
  'header.nav.account': 'アカウント ({name})',
  'header.nav.admin': '管理',
  'header.login': 'ログイン',
  'header.signup': '会員登録',
  'header.logout': 'ログアウト',
  'header.language': '表示言語',
  'header.menu': 'メニュー',
  'header.theme.toDark': 'ダーク表示に切り替える',
  'header.theme.toLight': 'ライト表示に切り替える',

  'attribution.newTab': '(新しいタブで開きます)',

  'notify.loggedOut': 'ログアウトしました',
  'notify.sessionExpired': '一定時間操作がなかったため、ログアウトしました',

  'error.pageNotFound': 'ページが見つかりません',
  'error.performanceNotFound': '公演が見つかりません',
  'error.forbidden': 'この画面を表示する権限がありません',
  'error.goToMap': '地図へ移動',

  'state.loadError': '読み込めませんでした',
  'state.reload': 'もう一度読み込む',

  'text.original': '原文',

  'performance.status.ONGOING': '公演中',
  'performance.status.UPCOMING': '公演予定',
  'performance.status.ENDED': '終了',
  'performance.award': '受賞作',

  'genre.ALL': '全体',
  'genre.AAAA': '演劇',
  'genre.GGGA': 'ミュージカル',
  'genre.CCCA': '西洋音楽(クラシック)',
  'genre.CCCC': '韓国音楽(国楽)',
  'genre.CCCD': '大衆音楽',
  'genre.BBBC': '舞踊',
  'genre.BBBE': '大衆舞踊',
  'genre.EEEA': '複合',
  'genre.EEEB': 'サーカス/マジック',

  'rankings.title': '人気・トレンド',
  'rankings.tab.ranking': '人気順位',
  'rankings.period': '期間',
  'rankings.period.daily': '日次',
  'rankings.period.weekly': '週次',
  'rankings.period.monthly': '月次',
  'rankings.genre': 'ジャンル',
  'rankings.aggregationPeriod': '集計期間',
  'rankings.rank': '{rank}位',
  'rankings.empty': 'この期間の順位データがありません',
};
