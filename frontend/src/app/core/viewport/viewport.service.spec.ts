import { MediaMatcher } from '@angular/cdk/layout';
import { TestBed } from '@angular/core/testing';
import breakpointsScss from '../../../theme/_breakpoints.scss' with { loader: 'text' };
import { HANDSET_MEDIA_QUERY, ViewportService } from './viewport.service';

/**
 * jsdom は matchMedia を持たないため、画面の幅を手で決める MediaMatcher に差し替える。
 * ブラウザと同じく max-width は「その幅以下」で当たる。向き（orientation）などは評価しない（幅だけで決まることの確認）
 */
class FakeMediaMatcher {
  width = 1280;
  private readonly lists: {
    query: string;
    list: MediaQueryList;
    handlers: ((e: unknown) => void)[];
  }[] = [];

  matchMedia(query: string): MediaQueryList {
    const handlers: ((e: unknown) => void)[] = [];
    const evaluate = () => this.evaluate(query);
    const list = {
      media: query,
      get matches() {
        return evaluate();
      },
      addListener: (h: (e: unknown) => void) => handlers.push(h),
      removeListener: () => undefined,
    } as unknown as MediaQueryList;
    this.lists.push({ query, list, handlers });
    return list;
  }

  resize(width: number): void {
    this.width = width;
    for (const { list, handlers } of this.lists) {
      handlers.forEach((h) => h({ matches: list.matches, media: list.media }));
    }
  }

  private evaluate(query: string): boolean {
    const m = /^\(max-width: ([\d.]+)px\)$/.exec(query);
    if (!m) {
      throw new Error(`unexpected media query: ${query}`);
    }
    return this.width <= Number(m[1]);
  }
}

describe('ViewportService (02 1., 07 2.4)', () => {
  let matcher: FakeMediaMatcher;

  function create(width: number): ViewportService {
    matcher = new FakeMediaMatcher();
    matcher.width = width;
    TestBed.configureTestingModule({
      providers: [{ provide: MediaMatcher, useValue: matcher }],
    });
    return TestBed.inject(ViewportService);
  }

  it('uses the width-only query (max-width: 959.98px), not Breakpoints.Handset', () => {
    expect(HANDSET_MEDIA_QUERY).toBe('(max-width: 959.98px)');
  });

  it('uses the same boundary as the CSS media query (src/theme/_breakpoints.scss)', () => {
    expect(breakpointsScss).toContain(`$handset: '${HANDSET_MEDIA_QUERY}';`);
  });

  it.each([
    [360, true],
    [600, true],
    [959, true],
    [959.5, true],
    [960, false],
    [1280, false],
  ])('width %spx: handset = %s', (width, expected) => {
    expect(create(width).handset()).toBe(expected);
  });

  it('switches between handset and PC when the width crosses 959px / 960px', async () => {
    const viewport = create(960);
    expect(viewport.handset()).toBe(false);

    matcher.resize(959);
    // BreakpointObserver は 2 回目以降の変化を debounceTime(0) で流す
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(viewport.handset()).toBe(true);

    matcher.resize(960);
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(viewport.handset()).toBe(false);
  });
});
