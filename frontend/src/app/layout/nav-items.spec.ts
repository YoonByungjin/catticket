import { AuthMeResponse } from '../core/api/api.types';
import { SIGNED_OUT, accountLinksFor, navLinksFor } from './nav-items';

const USER: AuthMeResponse = { authenticated: true, role: 'USER', nickname: 'Cat' };
const ADMIN: AuthMeResponse = { authenticated: true, role: 'ADMIN', nickname: null };

const paths = (links: { path: string }[]) => links.map((l) => l.path);

describe('nav items (02 3.1)', () => {
  it('signed out: map, rankings / login, signup', () => {
    expect(paths(navLinksFor(SIGNED_OUT))).toEqual(['/', '/rankings']);
    expect(paths(accountLinksFor(SIGNED_OUT))).toEqual(['/login', '/signup']);
  });

  it('user: adds favorites / account with nickname', () => {
    expect(paths(navLinksFor(USER))).toEqual(['/', '/rankings', '/favorites']);
    const account = accountLinksFor(USER);
    expect(paths(account)).toEqual(['/account']);
    expect(account[0].showNickname).toBe(true);
  });

  it('admin: adds admin / no account link', () => {
    expect(paths(navLinksFor(ADMIN))).toEqual(['/', '/rankings', '/admin/performances']);
    expect(accountLinksFor(ADMIN)).toEqual([]);
  });

  it('only the map link matches exactly', () => {
    expect(navLinksFor(SIGNED_OUT).map((l) => l.exact)).toEqual([true, false]);
  });
});
