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

  'notify.loggedOut': 'ログアウトしました',
  'notify.sessionExpired': '一定時間操作がなかったため、ログアウトしました',

  'error.pageNotFound': 'ページが見つかりません',
  'error.performanceNotFound': '公演が見つかりません',
  'error.forbidden': 'この画面を表示する権限がありません',
  'error.goToMap': '地図へ移動',
};
