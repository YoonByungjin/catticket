import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { App } from './app';
import { routes } from './app.routes';
import { BROWSER_STORAGE } from './core/storage/safe-storage';
import { ViewportService } from './core/viewport/viewport.service';

describe('App shell and routes', () => {
  const handset = signal(false);

  beforeEach(() => {
    handset.set(false);
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes, withComponentInputBinding()),
        { provide: ViewportService, useValue: { handset } },
        { provide: BROWSER_STORAGE, useValue: null },
      ],
    });
  });

  it('renders header, main and footer', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('app-header mat-toolbar')).not.toBeNull();
    expect(el.querySelector('main router-outlet')).not.toBeNull();
    expect(el.querySelector('app-footer footer')).not.toBeNull();
  });

  // 02 の 2. 画面一覧の URL
  const screens: [string, string][] = [
    ['/', 'SC-01'],
    ['/performances/PF123456', 'SC-02'],
    ['/rankings', 'SC-03'],
    ['/login', 'SC-04'],
    ['/signup', 'SC-05'],
    ['/signup/verify', 'SC-06'],
    ['/password-reset', 'SC-07'],
    ['/password-reset/confirm', 'SC-08'],
    ['/unlock', 'SC-09'],
    ['/unlock/confirm', 'SC-10'],
    ['/favorites', 'SC-11'],
    ['/account', 'SC-12'],
    ['/admin/performances', 'SC-13'],
    ['/admin/performances/PF123456', 'SC-14'],
  ];

  it.each(screens)('routes %s to %s', async (url, screenId) => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(url);
    const section = harness.routeNativeElement?.querySelector('[data-screen-id]');
    expect(section?.getAttribute('data-screen-id')).toBe(screenId);
  });

  it('routes an unknown URL to SC-99 (page not found)', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/no-such-page');
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent?.trim()).toBe(
      '페이지를 찾을 수 없습니다',
    );
  });

  it('opens the handset menu and closes it after navigation', async () => {
    handset.set(true);
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;

    (el.querySelector('.ct-menu-button') as HTMLButtonElement).click();
    await fixture.whenStable();
    const items = Array.from(el.querySelectorAll('mat-sidenav a')).map((a) =>
      a.textContent?.trim(),
    );
    expect(items).toEqual(['지도', '인기·트렌드', '로그인', '회원가입']);
    expect(el.querySelector('.ct-menu-button')?.getAttribute('aria-expanded')).toBe('true');

    await TestBed.inject(Router).navigateByUrl('/rankings');
    await fixture.whenStable();
    expect(el.querySelector('.ct-menu-button')?.getAttribute('aria-expanded')).toBe('false');
  });

  // CI で起きた競合の再現: mat-sidenav は開いたことを setTimeout で遅れて通知する（openedChange）。
  // その通知が「画面を移る → メニューを閉じる」の後に届いても、閉じたままでなければならない。
  // whenStable の後に少し待ってから移ると、遅れた通知がちょうど閉じる処理の後に届く
  it.each([1, 2, 5])(
    'keeps the menu closed when the open notification arrives late (%i ms)',
    async (gapMs) => {
      handset.set(true);
      const fixture = TestBed.createComponent(App);
      await fixture.whenStable();
      const el = fixture.nativeElement as HTMLElement;

      (el.querySelector('.ct-menu-button') as HTMLButtonElement).click();
      await fixture.whenStable();
      await new Promise((resolve) => setTimeout(resolve, gapMs));
      await TestBed.inject(Router).navigateByUrl('/rankings');
      await fixture.whenStable();
      // 遅れて届く通知をすべて流してから確かめる
      await new Promise((resolve) => setTimeout(resolve, 20));
      await fixture.whenStable();

      expect(el.querySelector('.ct-menu-button')?.getAttribute('aria-expanded')).toBe('false');
      expect(el.querySelector('mat-sidenav')?.classList.contains('mat-drawer-opened')).toBe(false);
    },
  );
});
