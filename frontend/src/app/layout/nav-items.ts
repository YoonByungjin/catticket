import { AuthMeResponse } from '../core/api/api.types';
import { MessageKey } from '../core/i18n/messages';

export interface NavLink {
  /** 文言のキー。showNickname のときは {name} にニックネームを入れる */
  labelKey: MessageKey;
  path: string;
  /** '/' だけ完全一致にしないと、すべての画面で「地図」が選択中になる */
  exact: boolean;
  /** 一般ユーザーの「アカウント」は「계정 (ニックネーム)」と表示する（02 の 3.1、07 の 4.1） */
  showNickname?: boolean;
}

/** 未ログインの状態（API-30 を呼ぶまでの既定） */
export const SIGNED_OUT: AuthMeResponse = { authenticated: false, role: null, nickname: null };

/** CM-01 の画面への移動の項目（02 の 3.1 の表。ログイン状態で変わる） */
export function navLinksFor(auth: AuthMeResponse): NavLink[] {
  const links: NavLink[] = [
    { labelKey: 'header.nav.map', path: '/', exact: true },
    { labelKey: 'header.nav.rankings', path: '/rankings', exact: false },
  ];
  if (auth.authenticated && auth.role === 'USER') {
    links.push({ labelKey: 'header.nav.favorites', path: '/favorites', exact: false });
  }
  if (auth.authenticated && auth.role === 'ADMIN') {
    links.push({ labelKey: 'header.nav.admin', path: '/admin/performances', exact: false });
  }
  return links;
}

/** CM-01 の右端のログイン状態に応じた項目（02 の 3.1、07 の 4.1） */
export function accountLinksFor(auth: AuthMeResponse): NavLink[] {
  if (!auth.authenticated) {
    return [
      { labelKey: 'header.login', path: '/login', exact: false },
      { labelKey: 'header.signup', path: '/signup', exact: false },
    ];
  }
  if (auth.role === 'USER') {
    return [{ labelKey: 'header.nav.account', path: '/account', exact: false, showNickname: true }];
  }
  return [];
}
