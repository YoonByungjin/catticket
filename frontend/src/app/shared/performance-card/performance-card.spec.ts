import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PerformanceSummary } from '../../core/api/api.types';
import { LanguageService, RELOAD_PAGE } from '../../core/i18n/language.service';
import { BROWSER_STORAGE } from '../../core/storage/safe-storage';
import { PerformanceCard } from './performance-card';

function summary(overrides: Partial<PerformanceSummary> = {}): PerformanceSummary {
  return {
    performanceId: 'PF270001',
    title: { value: '오페라의 유령', original: '오페라의 유령', translated: false },
    venue: {
      venueId: 'FC001247',
      name: { value: '샤롯데씨어터', original: '샤롯데씨어터', translated: false },
    },
    startDate: '2026-09-01',
    endDate: '2027-02-28',
    genre: 'GGGA',
    status: 'ONGOING',
    posterUrl: null,
    hasAward: true,
    rating: null,
    ...overrides,
  };
}

describe('PerformanceCard (02 3.7, 07 4.4)', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        // 記憶できない状態にして、読み込み直しなしで日本語に切り替えられるようにする
        { provide: BROWSER_STORAGE, useValue: null },
        { provide: RELOAD_PAGE, useValue: () => undefined },
      ],
    });
  });

  async function render(p: PerformanceSummary): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(PerformanceCard);
    fixture.componentRef.setInput('performance', p);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  const text = (el: HTMLElement, selector: string) =>
    el.querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim();
  const labels = (el: HTMLElement) =>
    Array.from(el.querySelectorAll('.ct-card__labels .ct-label')).map((n) => n.textContent?.trim());

  it('links the whole card to SC-02', async () => {
    const el = await render(summary());
    const link = el.querySelector('a.ct-card-link');
    expect(link?.getAttribute('href')).toBe('/performances/PF270001');
    expect(link?.querySelector('mat-card')).not.toBeNull();
  });

  it('shows title, venue, period, genre, status and award in Korean', async () => {
    const el = await render(summary());
    expect(text(el, '.ct-card__title')).toBe('오페라의 유령');
    expect(text(el, '.ct-card__sub')).toBe('샤롯데씨어터');
    expect(el.querySelector('app-date-range')?.textContent).toBe('2026-09-01~2027-02-28');
    expect(labels(el)).toEqual(['뮤지컬', '공연 중', '수상작']);
    // 韓国語表示では原文の併記も「원문」の印も出さない
    expect(el.querySelector('.ct-card__original')).toBeNull();
    expect(el.querySelector('.ct-label--original')).toBeNull();
  });

  it('does not show a rating (SC-03 cards exclude the rating)', async () => {
    const el = await render(summary({ rating: { average: 4.3, count: 12 } }));
    expect(el.textContent).not.toContain('4.3');
    expect(el.querySelector('mat-icon')).toBeNull();
  });

  it('shows the award badge only with an award (F-31)', async () => {
    const el = await render(summary({ hasAward: false }));
    expect(labels(el)).toEqual(['뮤지컬', '공연 중']);
  });

  it.each([
    ['UPCOMING', '공연 예정'],
    ['ENDED', '종료'],
  ] as const)('shows the status %s as text (F-09 ③)', async (status, label) => {
    const el = await render(summary({ status, hasAward: false }));
    expect(labels(el)[1]).toBe(label);
    expect(el.querySelector(`.ct-label--${status}`)).not.toBeNull();
  });

  it('shows an unknown genre code as is', async () => {
    const el = await render(summary({ genre: 'ZZZZ' }));
    expect(labels(el)[0]).toBe('ZZZZ');
  });

  it('shows the poster with the title as alt text, or a placeholder box', async () => {
    let el = await render(summary({ posterUrl: 'https://example.com/p.jpg' }));
    const img = el.querySelector('img');
    expect(img?.getAttribute('src')).toBe('https://example.com/p.jpg');
    expect(img?.getAttribute('alt')).toBe('오페라의 유령');

    el = await render(summary());
    expect(el.querySelector('img')).toBeNull();
    expect(el.querySelector('.ct-card__poster--none')).not.toBeNull();
  });

  describe('Japanese', () => {
    beforeEach(() => TestBed.inject(LanguageService).switchTo('ja'));

    it('shows the translation with the original below (F-11 ②)', async () => {
      const el = await render(
        summary({
          title: { value: 'オペラ座の怪人', original: '오페라의 유령', translated: true },
        }),
      );
      expect(text(el, '.ct-card__title')).toBe('オペラ座の怪人');
      expect(text(el, '.ct-card__original')).toBe('오페라의 유령');
      expect(labels(el)).toEqual(['ミュージカル', '公演中', '受賞作']);
    });

    it('marks untranslated text with "原文" and shows it once (P-06)', async () => {
      const el = await render(summary());
      expect(text(el, '.ct-card__title')).toBe('오페라의 유령 原文');
      expect(el.querySelectorAll('.ct-card__original').length).toBe(0);
      expect(el.querySelectorAll('.ct-label--original').length).toBe(2);
    });
  });
});
