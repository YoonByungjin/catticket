import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthMeResponse } from '../../core/api/api.types';
import { LanguageService, RELOAD_PAGE } from '../../core/i18n/language.service';
import { BROWSER_STORAGE } from '../../core/storage/safe-storage';
import { ThemeService } from '../../core/theme/theme.service';
import { HANDSET_MEDIA_QUERY, ViewportService } from '../../core/viewport/viewport.service';
import { Header } from './header';

describe('Header (CM-01)', () => {
  const handset = signal(false);
  let reload: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    handset.set(false);
    reload = vi.fn();
    localStorage.clear();
    document.documentElement.style.colorScheme = 'light';
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: ViewportService, useValue: { handset } },
        { provide: RELOAD_PAGE, useValue: reload },
      ],
    });
  });

  afterEach(() => {
    localStorage.clear();
    document.documentElement.style.colorScheme = '';
  });

  async function render(auth?: AuthMeResponse): Promise<ComponentFixture<Header>> {
    const fixture = TestBed.createComponent(Header);
    if (auth) {
      fixture.componentRef.setInput('auth', auth);
    }
    await fixture.whenStable();
    return fixture;
  }

  const texts = (el: HTMLElement, selector: string) =>
    Array.from(el.querySelectorAll(selector)).map((n) => n.textContent?.trim());

  it('PC, signed out: service name, map, rankings, login, signup, language, theme', async () => {
    const el = (await render()).nativeElement as HTMLElement;
    expect(el.querySelector('.ct-brand')?.textContent?.trim()).toBe('CatTicket');
    expect(el.querySelector('.ct-brand')?.getAttribute('href')).toBe('/');
    expect(texts(el, '.ct-nav a')).toEqual(['지도', '인기·트렌드']);
    expect(texts(el, '.ct-actions a')).toEqual(['로그인', '회원가입']);
    expect(texts(el, 'mat-button-toggle')).toEqual(['한국어', '日本語']);
    expect(el.querySelector('.ct-menu-button')).toBeNull();
  });

  it('PC, user: favorites, "계정 (nickname)", logout (Issue #24 decision 4)', async () => {
    const fixture = await render({ authenticated: true, role: 'USER', nickname: 'Cat' });
    const el = fixture.nativeElement as HTMLElement;
    expect(texts(el, '.ct-nav a')).toEqual(['지도', '인기·트렌드', '즐겨찾기']);
    expect(texts(el, '.ct-actions a')).toEqual(['계정 (Cat)']);
    expect(el.querySelector('.ct-actions a')?.getAttribute('href')).toBe('/account');
    expect(texts(el, '.ct-actions button')).toEqual(['로그아웃']);

    const logout = vi.fn();
    fixture.componentInstance.logout.subscribe(logout);
    (el.querySelector('.ct-actions button') as HTMLButtonElement).click();
    expect(logout).toHaveBeenCalled();
  });

  it('PC, user: only the nickname part can shrink (07 4.1.1)', async () => {
    const nickname = 'あ'.repeat(20);
    const el = (await render({ authenticated: true, role: 'USER', nickname }))
      .nativeElement as HTMLElement;
    const account = el.querySelector('.ct-actions > app-account-link') as HTMLElement;
    expect(account.querySelector('.ct-account-link__name')?.textContent).toBe(nickname);
    expect(account.querySelector('a')?.getAttribute('aria-label')).toBe(`계정 (${nickname})`);
    // 縮めない項目には自作のクラスで flex-shrink: 0 を付ける
    expect(el.querySelector('.ct-actions > button')?.classList.contains('ct-fixed')).toBe(true);
  });

  // jsdom はスタイルの優先順位をブラウザと同じには計算しない。実際の画面の値は 07 の 4.1.1 の実測
  // （ヘッドレス Chrome）で確かめた
  it('side padding is set by our own class: PC 24px (07 2.4)', async () => {
    const el = (await render()).nativeElement as HTMLElement;
    const toolbar = el.querySelector('mat-toolbar') as HTMLElement;
    expect(toolbar.classList.contains('ct-header')).toBe(true);
    const style = getComputedStyle(toolbar);
    expect(style.paddingLeft).toBe('24px');
    expect(style.paddingRight).toBe('24px');
  });

  it('side padding on handset is 16px at the shared breakpoint (07 2.4)', async () => {
    await render();
    // jsdom はメディアクエリを評価しないため、境目の @media の中の .ct-header の規則を読む
    const rules = Array.from(document.styleSheets)
      .flatMap((sheet) => Array.from(sheet.cssRules))
      .filter((rule): rule is CSSMediaRule => rule instanceof CSSMediaRule)
      .filter((rule) => rule.media.mediaText === HANDSET_MEDIA_QUERY)
      .flatMap((rule) => Array.from(rule.cssRules))
      .filter(
        (rule): rule is CSSStyleRule =>
          rule instanceof CSSStyleRule && /^\.ct-header(\[|$)/.test(rule.selectorText),
      );
    expect(rules.length).toBe(1);
    expect(rules[0].style.paddingLeft).toBe('16px');
    expect(rules[0].style.paddingRight).toBe('16px');
  });

  it('PC, signed out: login and signup do not shrink', async () => {
    const el = (await render()).nativeElement as HTMLElement;
    const links = Array.from(el.querySelectorAll('.ct-actions > a'));
    expect(links.length).toBe(2);
    expect(links.every((a) => a.classList.contains('ct-fixed'))).toBe(true);
    expect(el.querySelector('app-account-link')).toBeNull();
  });

  it('PC, admin: admin link and logout, no account', async () => {
    const el = (await render({ authenticated: true, role: 'ADMIN', nickname: null }))
      .nativeElement as HTMLElement;
    expect(texts(el, '.ct-nav a')).toEqual(['지도', '인기·트렌드', '관리']);
    expect(texts(el, '.ct-actions a')).toEqual([]);
    expect(texts(el, '.ct-actions button')).toEqual(['로그아웃']);
    // 管理者にも切り替えを出す（P-16）
    expect(el.querySelector('.ct-theme-toggle')).not.toBeNull();
    expect(el.querySelector('mat-button-toggle-group')).not.toBeNull();
  });

  it('handset: only service name, language, theme and menu button', async () => {
    handset.set(true);
    const fixture = await render();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.ct-nav')).toBeNull();
    expect(el.querySelector('.ct-actions')).toBeNull();
    const menu = el.querySelector('.ct-menu-button') as HTMLButtonElement;
    expect(menu.getAttribute('aria-label')).toBe('메뉴');
    expect(menu.getAttribute('aria-expanded')).toBe('false');

    const toggled = vi.fn();
    fixture.componentInstance.menuToggle.subscribe(toggled);
    menu.click();
    expect(toggled).toHaveBeenCalled();
  });

  it('theme button shows the scheme after pressing and toggles it (07 3., F-36)', async () => {
    const fixture = await render();
    const el = fixture.nativeElement as HTMLElement;
    const button = el.querySelector('.ct-theme-toggle') as HTMLButtonElement;
    expect(button.getAttribute('aria-label')).toBe('다크 표시로 전환');
    expect(button.textContent?.trim()).toBe('dark_mode');

    button.click();
    await fixture.whenStable();
    expect(TestBed.inject(ThemeService).scheme()).toBe('dark');
    expect(document.documentElement.style.colorScheme).toBe('dark');
    expect(button.getAttribute('aria-label')).toBe('라이트 표시로 전환');
    expect(button.textContent?.trim()).toBe('light_mode');
  });

  it('language toggle stores Japanese and reloads (F-10 ②③)', async () => {
    const el = (await render()).nativeElement as HTMLElement;
    const toggles = el.querySelectorAll('mat-button-toggle button');
    expect(toggles[0].getAttribute('aria-pressed') ?? toggles[0].getAttribute('aria-checked')).toBe(
      'true',
    );
    (toggles[1] as HTMLButtonElement).click();
    expect(localStorage.getItem('catticket.lang')).toBe('ja');
    expect(reload).toHaveBeenCalledTimes(1);
  });

  it('shows Japanese text when the language is Japanese', async () => {
    TestBed.overrideProvider(BROWSER_STORAGE, { useValue: null });
    TestBed.inject(LanguageService).switchTo('ja');
    const el = (await render()).nativeElement as HTMLElement;
    expect(texts(el, '.ct-nav a')).toEqual(['地図', '人気・トレンド']);
    expect(texts(el, '.ct-actions a')).toEqual(['ログイン', '会員登録']);
    expect(el.querySelector('.ct-theme-toggle')?.getAttribute('aria-label')).toBe(
      'ダーク表示に切り替える',
    );
    // 言語名は切り替えても変えない（07 の 3.）
    expect(texts(el, 'mat-button-toggle')).toEqual(['한국어', '日本語']);
  });

  it('shows "アカウント (nickname)" in Japanese', async () => {
    TestBed.overrideProvider(BROWSER_STORAGE, { useValue: null });
    TestBed.inject(LanguageService).switchTo('ja');
    const el = (await render({ authenticated: true, role: 'USER', nickname: 'ネコ' }))
      .nativeElement as HTMLElement;
    expect(texts(el, '.ct-actions a')).toEqual(['アカウント (ネコ)']);
    expect(el.querySelector('.ct-actions a')?.getAttribute('aria-label')).toBe('アカウント (ネコ)');
    expect(texts(el, '.ct-actions button')).toEqual(['ログアウト']);
  });
});
