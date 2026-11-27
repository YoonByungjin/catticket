import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { PerformanceSummary } from '../../core/api/api.types';
import { LanguageService, RELOAD_PAGE } from '../../core/i18n/language.service';
import { BROWSER_STORAGE } from '../../core/storage/safe-storage';
import { HANDSET_MEDIA_QUERY, ViewportService } from '../../core/viewport/viewport.service';
import { RankingsPage } from './rankings-page';
import { RankingResponse } from './ranking.types';

function performance(id: string, title: string): PerformanceSummary {
  return {
    performanceId: id,
    title: { value: title, original: title, translated: false },
    venue: {
      venueId: 'FC001247',
      name: { value: '샤롯데씨어터', original: '샤롯데씨어터', translated: false },
    },
    startDate: '2026-09-01',
    endDate: '2027-02-28',
    genre: 'GGGA',
    status: 'ONGOING',
    posterUrl: null,
    hasAward: false,
    rating: null,
  };
}

const WEEKLY: RankingResponse = {
  periodType: 'WEEKLY',
  genre: 'ALL',
  period: { start: '2026-10-26', end: '2026-11-01' },
  items: [
    { rank: 1, performance: performance('PF000001', '오페라의 유령') },
    { rank: 2, performance: performance('PF000002', '레미제라블') },
  ],
};

describe('RankingsPage (SC-03 popularity ranking tab)', () => {
  const handset = signal(false);
  let http: HttpTestingController;

  beforeEach(() => {
    handset.set(false);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'rankings', component: RankingsPage }], withComponentInputBinding()),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ViewportService, useValue: { handset } },
        // 記憶できない状態にして、読み込み直しなしで日本語に切り替えられるようにする
        { provide: BROWSER_STORAGE, useValue: null },
        { provide: RELOAD_PAGE, useValue: () => undefined },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function open(url = '/rankings'): Promise<RouterTestingHarness> {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(url);
    return harness;
  }

  function expectRanking(period: string, genre: string, lang = 'ko'): TestRequest {
    return http.expectOne(
      (r) =>
        r.url === '/api/rankings' &&
        r.params.get('period') === period &&
        r.params.get('genre') === genre &&
        r.params.get('lang') === lang,
    );
  }

  /** 画面の移動は非同期なので、リクエストが出るまで待つ */
  function nextRequest(period: string, genre: string, lang = 'ko'): Promise<TestRequest> {
    return vi.waitFor(() => expectRanking(period, genre, lang));
  }

  /** 読み込み中を見るとき。HTTP が終わるまで安定しないので whenStable を待たずに描画する */
  function renderNow(harness: RouterTestingHarness): HTMLElement {
    harness.fixture.detectChanges();
    return harness.routeNativeElement as HTMLElement;
  }

  async function settle(harness: RouterTestingHarness): Promise<HTMLElement> {
    await harness.fixture.whenStable();
    return harness.routeNativeElement as HTMLElement;
  }

  const texts = (el: HTMLElement, selector: string) =>
    Array.from(el.querySelectorAll(selector)).map((n) =>
      n.textContent?.replace(/\s+/g, ' ').trim(),
    );

  it('shows only the popularity ranking tab (rising and trend tabs are hidden)', async () => {
    const harness = await open();
    expectRanking('weekly', 'ALL').flush(WEEKLY);
    const el = await settle(harness);
    expect(el.querySelector('h1')?.textContent?.trim()).toBe('인기·트렌드');
    expect(texts(el, '[role="tab"]')).toEqual(['인기 순위']);
  });

  it('shows skeletons while loading (02 3.6, 07 4.3)', async () => {
    const harness = await open();
    const el = renderNow(harness);
    expect(el.querySelectorAll('.ct-skeleton').length).toBe(3);
    expect(el.querySelector('[aria-busy="true"]')).not.toBeNull();
    expect(el.querySelector('.ct-aggregation')).toBeNull();
    expectRanking('weekly', 'ALL').flush(WEEKLY);
  });

  it('loads weekly / all genres by default and shows ranks, cards and the aggregation period (P-18)', async () => {
    const harness = await open();
    expectRanking('weekly', 'ALL').flush(WEEKLY);
    const el = await settle(harness);

    expect(el.querySelectorAll('.ct-skeleton').length).toBe(0);
    expect(texts(el, '.ct-aggregation')).toEqual(['집계 기간 2026-10-26~2026-11-01']);
    expect(texts(el, '.ct-row__rank')).toEqual(['1', '2']);
    expect(texts(el, '.ct-row .cdk-visually-hidden')).toEqual(['1위', '2위']);
    expect(texts(el, '.ct-card__title')).toEqual(['오페라의 유령', '레미제라블']);
    expect(el.querySelector('ol.ct-list app-performance-card a')?.getAttribute('href')).toBe(
      '/performances/PF000001',
    );
  });

  it('shows a single date as the aggregation period of a daily ranking', async () => {
    const harness = await open('/rankings?period=daily');
    expectRanking('daily', 'ALL').flush({
      ...WEEKLY,
      periodType: 'DAILY',
      period: { start: '2026-11-04', end: '2026-11-04' },
    });
    const el = await settle(harness);
    expect(texts(el, '.ct-aggregation')).toEqual(['집계 기간 2026-11-04']);
  });

  it('reads tab, period and genre from the URL query', async () => {
    const harness = await open('/rankings?tab=ranking&period=monthly&genre=GGGA');
    expectRanking('monthly', 'GGGA').flush(WEEKLY);
    const el = await settle(harness);

    const checked = el.querySelector('mat-button-toggle.mat-button-toggle-checked');
    expect(checked?.textContent?.trim()).toBe('월간');
    expect(texts(el, 'mat-chip-option.mat-mdc-chip-selected')).toEqual(['뮤지컬']);
  });

  it('treats unknown query values as the defaults', async () => {
    const harness = await open('/rankings?tab=rising&period=yearly&genre=XXXX');
    expectRanking('weekly', 'ALL').flush(WEEKLY);
    const el = await settle(harness);
    expect(texts(el, '[role="tab"]')).toEqual(['인기 순위']);
  });

  it('changes the period from the toggle and keeps it in the URL', async () => {
    const harness = await open('/rankings?genre=AAAA');
    expectRanking('weekly', 'AAAA').flush(WEEKLY);
    const el = await settle(harness);

    const toggles = el.querySelectorAll<HTMLButtonElement>('mat-button-toggle button');
    expect(texts(el, 'mat-button-toggle')).toEqual(['일간', '주간', '월간']);
    toggles[0].click();

    // 画面を移った後に取り直す
    (await nextRequest('daily', 'AAAA')).flush(WEEKLY);
    expect(TestBed.inject(Router).url).toBe('/rankings?tab=ranking&period=daily&genre=AAAA');
  });

  it('PC: picks the genre with chips (02 2.)', async () => {
    const harness = await open();
    expectRanking('weekly', 'ALL').flush(WEEKLY);
    const el = await settle(harness);

    expect(el.querySelector('mat-select')).toBeNull();
    expect(el.querySelector('mat-chip-listbox')?.getAttribute('aria-label')).toBe('장르');
    const chips = texts(el, 'mat-chip-option');
    expect(chips[0]).toBe('전체');
    expect(chips).toContain('뮤지컬');
    expect(chips.length).toBe(10);

    const musical = Array.from(el.querySelectorAll('mat-chip-option')).find(
      (c) => c.textContent?.trim() === '뮤지컬',
    );
    (musical?.querySelector('button, [role="option"]') as HTMLElement).click();

    (await nextRequest('weekly', 'GGGA')).flush(WEEKLY);
    expect(TestBed.inject(Router).url).toBe('/rankings?tab=ranking&period=weekly&genre=GGGA');
  });

  it('PC: pressing the selected chip keeps it selected and does not navigate', async () => {
    const harness = await open('/rankings?genre=GGGA');
    expectRanking('weekly', 'GGGA').flush(WEEKLY);
    const el = await settle(harness);

    const musical = Array.from(el.querySelectorAll('mat-chip-option')).find(
      (c) => c.textContent?.trim() === '뮤지컬',
    );
    (musical?.querySelector('button, [role="option"]') as HTMLElement).click();
    await settle(harness);

    expect(TestBed.inject(Router).url).toBe('/rankings?genre=GGGA');
    expect(musical?.classList.contains('mat-mdc-chip-selected')).toBe(true);
  });

  it('handset: picks the genre with a dropdown (02 2.)', async () => {
    handset.set(true);
    const harness = await open();
    expectRanking('weekly', 'ALL').flush(WEEKLY);
    const el = await settle(harness);

    expect(el.querySelector('mat-chip-listbox')).toBeNull();
    expect(el.querySelector('mat-form-field mat-label')?.textContent?.trim()).toBe('장르');
    (el.querySelector('.mat-mdc-select-trigger') as HTMLElement).click();
    await settle(harness);

    const options = Array.from(document.querySelectorAll<HTMLElement>('mat-option'));
    expect(options.map((o) => o.textContent?.trim())[0]).toBe('전체');
    options.find((o) => o.textContent?.trim() === '연극')?.click();

    (await nextRequest('weekly', 'AAAA')).flush(WEEKLY);
    expect(TestBed.inject(Router).url).toBe('/rankings?tab=ranking&period=weekly&genre=AAAA');
  });

  it('shows the empty message when there is no period to show (03 4.9)', async () => {
    const harness = await open();
    expectRanking('weekly', 'ALL').flush({ ...WEEKLY, period: null, items: [] });
    const el = await settle(harness);
    expect(texts(el, '.ct-message__text')).toEqual(['이 기간의 순위 데이터가 없습니다']);
    expect(el.querySelector('.ct-aggregation')).toBeNull();
    expect(el.querySelector('ol.ct-list')).toBeNull();
  });

  it('shows the empty message with the aggregation period when the period has no items', async () => {
    const harness = await open();
    expectRanking('weekly', 'ALL').flush({ ...WEEKLY, items: [] });
    const el = await settle(harness);
    expect(texts(el, '.ct-message__text')).toEqual(['이 기간의 순위 데이터가 없습니다']);
    expect(texts(el, '.ct-aggregation')).toEqual(['집계 기간 2026-10-26~2026-11-01']);
  });

  it('shows the error state and reloads (02 3.6)', async () => {
    const harness = await open();
    expectRanking('weekly', 'ALL').flush(
      { code: 'INTERNAL_ERROR', message: 'error', errors: [] },
      { status: 500, statusText: 'Internal Server Error' },
    );
    let el = await settle(harness);
    expect(texts(el, '.ct-message__text')).toEqual(['불러오지 못했습니다']);
    const button = el.querySelector('.ct-message button') as HTMLButtonElement;
    expect(button.textContent?.trim()).toBe('다시 불러오기');
    expect(el.querySelector('.ct-aggregation')).toBeNull();

    button.click();
    el = renderNow(harness);
    expect(el.querySelectorAll('.ct-skeleton').length).toBe(3);
    expectRanking('weekly', 'ALL').flush(WEEKLY);
    el = await settle(harness);
    expect(texts(el, '.ct-row__rank')).toEqual(['1', '2']);
  });

  it('shows Japanese text and requests lang=ja (N-01)', async () => {
    TestBed.inject(LanguageService).switchTo('ja');
    const harness = await open();
    expectRanking('weekly', 'ALL', 'ja').flush(WEEKLY);
    const el = await settle(harness);

    expect(el.querySelector('h1')?.textContent?.trim()).toBe('人気・トレンド');
    expect(texts(el, '[role="tab"]')).toEqual(['人気順位']);
    expect(texts(el, 'mat-button-toggle')).toEqual(['日次', '週次', '月次']);
    expect(el.querySelector('mat-button-toggle-group')?.getAttribute('aria-label')).toBe('期間');
    expect(texts(el, 'mat-chip-option')[0]).toBe('全体');
    expect(texts(el, '.ct-aggregation')).toEqual(['集計期間 2026-10-26~2026-11-01']);
    expect(texts(el, '.ct-row .cdk-visually-hidden')).toEqual(['1位', '2位']);
  });

  it('shows the Japanese empty and error messages', async () => {
    TestBed.inject(LanguageService).switchTo('ja');
    const harness = await open();
    expectRanking('weekly', 'ALL', 'ja').flush({ ...WEEKLY, period: null, items: [] });
    let el = await settle(harness);
    expect(texts(el, '.ct-message__text')).toEqual(['この期間の順位データがありません']);

    await harness.navigateByUrl('/rankings?period=daily');
    // 通信の失敗
    expectRanking('daily', 'ALL', 'ja').error(new ProgressEvent('error'));
    el = await settle(harness);
    expect(texts(el, '.ct-message__text')).toEqual(['読み込めませんでした']);
    expect(texts(el, '.ct-message button')).toEqual(['もう一度読み込む']);
  });

  // jsdom はメディアクエリを評価しないため、境目の @media の中の規則を読む
  it('limits the handset content width to 640px, centered (07 2.4)', async () => {
    const harness = await open();
    expectRanking('weekly', 'ALL').flush(WEEKLY);
    await settle(harness);

    const page = (harness.routeNativeElement as HTMLElement).querySelector(
      '.ct-page',
    ) as HTMLElement;
    const style = getComputedStyle(page);
    expect(style.maxWidth).toBe('1200px');
    expect(style.marginLeft).toBe('auto');
    expect(style.marginRight).toBe('auto');

    const rules = Array.from(document.styleSheets)
      .flatMap((sheet) => Array.from(sheet.cssRules))
      .filter((rule): rule is CSSMediaRule => rule instanceof CSSMediaRule)
      .filter((rule) => rule.media.mediaText === HANDSET_MEDIA_QUERY)
      .flatMap((rule) => Array.from(rule.cssRules))
      .filter(
        (rule): rule is CSSStyleRule =>
          rule instanceof CSSStyleRule && /^\.ct-page(\[|$)/.test(rule.selectorText),
      );
    expect(rules.length).toBe(1);
    expect(rules[0].style.maxWidth).toBe('640px');
  });
});
