import { TestBed } from '@angular/core/testing';
import { LanguageService } from '../../core/i18n/language.service';
import { BROWSER_STORAGE } from '../../core/storage/safe-storage';
import { ATTRIBUTION_LINES, Attribution, attributionText } from './attribution';

// 02 の 3.2 の「フッターの2行の原文」。この文字列のまま表示する
const LINE1 = '출처: (재)예술경영지원센터 공연예술통합전산망(www.kopis.or.kr)';
const LINE2 =
  '출처: 서울교통공사 공공데이터(역 좌표·주소, 물품보관함 위치, 역명 다국어 표기) - 공공데이터포털(www.data.go.kr), 서울 열린데이터 광장(data.seoul.go.kr)';

describe('Attribution (02 3.2, 07 4.2.1)', () => {
  afterEach(() => {
    document.documentElement.lang = '';
  });

  async function render(): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(Attribution);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  function expectOriginalTextAndLinks(el: HTMLElement): void {
    const lines = Array.from(el.querySelectorAll('p'));
    expect(lines.map((p) => p.textContent)).toEqual([LINE1, LINE2]);

    // 1行目はリンクなし
    expect(lines[0].querySelectorAll('a').length).toBe(0);

    // 2行目はサイト名の2つだけがリンク
    const links = Array.from(lines[1].querySelectorAll('a'));
    expect(links.map((a) => a.textContent)).toEqual(['공공데이터포털', '서울 열린데이터 광장']);
    expect(links.map((a) => a.getAttribute('href'))).toEqual([
      'https://www.data.go.kr',
      'https://data.seoul.go.kr/dataList/OA-2751/F/1/datasetView.do',
    ]);
    for (const a of links) {
      expect(a.getAttribute('target')).toBe('_blank');
      expect(a.getAttribute('rel')).toBe('noopener noreferrer');
      expect(a.classList.contains('ct-source-link')).toBe(true);
      // U-4 が未決のため、見えない文言・aria-label は足さない（07 の 4.2.1）
      expect(a.children.length).toBe(0);
      expect(a.hasAttribute('aria-label')).toBe(false);
    }
  }

  it('data joins to the original strings', () => {
    expect(ATTRIBUTION_LINES.map(attributionText)).toEqual([LINE1, LINE2]);
  });

  it('shows the original text and links only the two site names', async () => {
    const el = await render();
    expectOriginalTextAndLinks(el);
    expect(el.getAttribute('lang')).toBe('ko');
  });

  it('does not link the domains in parentheses', async () => {
    const el = await render();
    const linked = Array.from(el.querySelectorAll('a')).map((a) => a.textContent ?? '');
    for (const domain of ['www.kopis.or.kr', 'www.data.go.kr', 'data.seoul.go.kr']) {
      expect(linked.some((text) => text.includes(domain))).toBe(false);
    }
  });

  it('is not translated in Japanese (02 3.2)', async () => {
    TestBed.configureTestingModule({ providers: [{ provide: BROWSER_STORAGE, useValue: null }] });
    TestBed.inject(LanguageService).switchTo('ja');
    expect(document.documentElement.lang).toBe('ja');
    const el = await render();
    expectOriginalTextAndLinks(el);
    expect(el.getAttribute('lang')).toBe('ko');
  });
});
