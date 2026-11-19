import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatTooltip } from '@angular/material/tooltip';
import { By } from '@angular/platform-browser';
import { Router, provideRouter } from '@angular/router';
import { LanguageService } from '../../core/i18n/language.service';
import { BROWSER_STORAGE } from '../../core/storage/safe-storage';
import { AccountLink, splitAroundName } from './account-link';

/** jsdom にはレイアウトがないため、<span> の表示の幅（clientWidth）と中身の幅（scrollWidth）を手で決める */
function setWidths(el: HTMLElement, widths: { client: number; scroll: number }): void {
  Object.defineProperty(el, 'clientWidth', { configurable: true, get: () => widths.client });
  Object.defineProperty(el, 'scrollWidth', { configurable: true, get: () => widths.scroll });
}

/** 画面の幅の変化を手で起こすための ResizeObserver */
class FakeResizeObserver {
  static instances: FakeResizeObserver[] = [];
  readonly targets: Element[] = [];
  disconnected = false;

  constructor(private readonly callback: ResizeObserverCallback) {
    FakeResizeObserver.instances.push(this);
  }

  observe(target: Element): void {
    this.targets.push(target);
  }

  unobserve(): void {
    // 使わない
  }

  disconnect(): void {
    this.disconnected = true;
  }

  fire(): void {
    this.callback([], this as unknown as ResizeObserver);
  }
}

/** /account へ移るためだけの空の画面 */
@Component({ template: '' })
class EmptyPage {}

describe('AccountLink (CM-01 account link, 07 4.1.1)', () => {
  beforeEach(() => {
    FakeResizeObserver.instances = [];
    vi.stubGlobal('ResizeObserver', FakeResizeObserver);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'account', component: EmptyPage }]),
        // 保存できない扱いにして、言語の切り替えをその場で反映させる（読み込み直さない）
        { provide: BROWSER_STORAGE, useValue: null },
      ],
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  async function render(nickname: string): Promise<ComponentFixture<AccountLink>> {
    const fixture = TestBed.createComponent(AccountLink);
    fixture.componentRef.setInput('nickname', nickname);
    await fixture.whenStable();
    return fixture;
  }

  const link = (f: ComponentFixture<AccountLink>) =>
    (f.nativeElement as HTMLElement).querySelector('a.ct-account-link') as HTMLAnchorElement;
  const nameSpan = (f: ComponentFixture<AccountLink>) =>
    (f.nativeElement as HTMLElement).querySelector('.ct-account-link__name') as HTMLElement;
  const tooltip = (f: ComponentFixture<AccountLink>) =>
    f.debugElement.query(By.directive(MatTooltip)).injector.get(MatTooltip);

  /** 幅を決めてから、画面の幅が変わったときと同じ通知を送る */
  async function resize(
    f: ComponentFixture<AccountLink>,
    widths: { client: number; scroll: number },
  ): Promise<void> {
    setWidths(nameSpan(f), widths);
    FakeResizeObserver.instances[0].fire();
    await f.whenStable();
  }

  it('shows "계정 (nickname)" with only the nickname in its own span (02 3.1)', async () => {
    const fixture = await render('Cat');
    const a = link(fixture);
    expect(a.textContent).toBe('계정 (Cat)');
    expect(a.getAttribute('href')).toBe('/account');
    expect(Array.from(a.querySelectorAll('span')).map((s) => s.textContent)).toEqual([
      '계정 (',
      'Cat',
      ')',
    ]);
    expect(nameSpan(fixture).textContent).toBe('Cat');
  });

  it('is a plain <a>, not mat-button (the mat-button label does not shrink, 07 4.1.1)', async () => {
    const a = link(await render('Cat'));
    expect(a.classList.contains('mat-mdc-button')).toBe(false);
    expect(a.querySelector('.mdc-button__label')).toBeNull();
  });

  it('aria-label is "계정 (full nickname)" even when truncated (decision No.6)', async () => {
    const long = '고양이를좋아하는사람입니다고양이를좋아하는';
    const fixture = await render(long);
    await resize(fixture, { client: 59, scroll: 280 });
    expect(link(fixture).getAttribute('aria-label')).toBe(`계정 (${long})`);
  });

  it('Japanese: "アカウント (nickname)" and aria-label in the same form (decision No.4)', async () => {
    TestBed.inject(LanguageService).switchTo('ja');
    const fixture = await render('ネコ');
    expect(link(fixture).textContent).toBe('アカウント (ネコ)');
    expect(link(fixture).getAttribute('aria-label')).toBe('アカウント (ネコ)');
  });

  it('not truncated: no tooltip (decision No.7)', async () => {
    const fixture = await render('Cat');
    await resize(fixture, { client: 24, scroll: 24 });
    expect(tooltip(fixture).disabled).toBe(true);
  });

  it('truncated: tooltip shows only the full nickname (decision No.6, No.7)', async () => {
    const long = 'あ'.repeat(20);
    const fixture = await render(long);
    await resize(fixture, { client: 160, scroll: 280 });
    expect(tooltip(fixture).disabled).toBe(false);
    expect(tooltip(fixture).message).toBe(long);
  });

  it('decides by widths, not by the number of characters', async () => {
    // 20 文字でも幅に収まっていれば切れていない
    const fixture = await render('i'.repeat(20));
    await resize(fixture, { client: 80, scroll: 80 });
    expect(tooltip(fixture).disabled).toBe(true);

    // 短くても中身が表示の幅を超えていれば切れている
    await resize(fixture, { client: 10, scroll: 30 });
    expect(tooltip(fixture).disabled).toBe(false);
  });

  it('re-checks when the screen width changes (ResizeObserver on the span)', async () => {
    const fixture = await render('あ'.repeat(20));
    expect(FakeResizeObserver.instances[0].targets).toEqual([nameSpan(fixture)]);

    await resize(fixture, { client: 160, scroll: 280 });
    expect(tooltip(fixture).disabled).toBe(false);

    // 幅が広がって中身が収まった（ヘッダーの中で縮んでいた場合）
    await resize(fixture, { client: 280, scroll: 280 });
    expect(tooltip(fixture).disabled).toBe(true);
  });

  it('re-checks when the nickname changes (SC-12)', async () => {
    const fixture = await render('Cat');
    const widths = { client: 24, scroll: 24 };
    await resize(fixture, widths);
    expect(tooltip(fixture).disabled).toBe(true);

    // 幅の通知を送らず、ニックネームの変化だけで判定し直すことを確かめる
    widths.client = 160;
    widths.scroll = 280;
    fixture.componentRef.setInput('nickname', 'あ'.repeat(20));
    await fixture.whenStable();
    expect(tooltip(fixture).disabled).toBe(false);
    expect(tooltip(fixture).message).toBe('あ'.repeat(20));
  });

  it('re-checks when the display language changes', async () => {
    const fixture = await render('あ'.repeat(12));
    const widths = { client: 160, scroll: 160 };
    await resize(fixture, widths);
    expect(tooltip(fixture).disabled).toBe(true);

    // 日本語のフォントで中身の幅が変わった（幅の通知は送らない）
    widths.scroll = 170;
    TestBed.inject(LanguageService).switchTo('ja');
    await fixture.whenStable();
    expect(link(fixture).textContent).toBe(`アカウント (${'あ'.repeat(12)})`);
    expect(tooltip(fixture).disabled).toBe(false);
  });

  it('menu: marks the link active while the account page is open (07 2.1.1 selected item)', async () => {
    const fixture = TestBed.createComponent(AccountLink);
    fixture.componentRef.setInput('nickname', 'Cat');
    fixture.componentRef.setInput('place', 'menu');
    await fixture.whenStable();
    expect(fixture.nativeElement.classList.contains('ct-account--menu')).toBe(true);
    expect(link(fixture).classList.contains('ct-account-link--active')).toBe(false);

    await TestBed.inject(Router).navigateByUrl('/account');
    await fixture.whenStable();
    expect(link(fixture).classList.contains('ct-account-link--active')).toBe(true);
    expect(link(fixture).getAttribute('aria-current')).toBe('page');
  });

  it('stops watching the width when destroyed', async () => {
    const fixture = await render('Cat');
    fixture.destroy();
    expect(FakeResizeObserver.instances[0].disconnected).toBe(true);
  });

  it('works without ResizeObserver', async () => {
    vi.stubGlobal('ResizeObserver', undefined);
    const fixture = await render('Cat');
    expect(link(fixture).textContent).toBe('계정 (Cat)');
    expect(FakeResizeObserver.instances).toEqual([]);
  });
});

describe('splitAroundName', () => {
  it('splits the message around {name}', () => {
    expect(splitAroundName('계정 ({name})')).toEqual({ prefix: '계정 (', suffix: ')' });
    expect(splitAroundName('アカウント ({name})')).toEqual({ prefix: 'アカウント (', suffix: ')' });
  });

  it('keeps the whole text as the prefix when {name} is missing', () => {
    expect(splitAroundName('계정')).toEqual({ prefix: '계정', suffix: '' });
  });
});
