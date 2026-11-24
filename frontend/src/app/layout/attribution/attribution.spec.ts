import { TestBed } from '@angular/core/testing';
import { LanguageService } from '../../core/i18n/language.service';
import { BROWSER_STORAGE } from '../../core/storage/safe-storage';
import globalStyles from '../../../styles.scss' with { loader: 'text' };
import { ATTRIBUTION_LINES, Attribution, attributionText } from './attribution';

// 02 の 3.2 の「フッターの2行の原文」。この文字列のまま表示する
const LINE1 = '출처: (재)예술경영지원센터 공연예술통합전산망(www.kopis.or.kr)';
const LINE2 =
  '출처: 서울교통공사 공공데이터(역 좌표·주소, 물품보관함 위치, 역명 다국어 표기) - 공공데이터포털(www.data.go.kr), 서울 열린데이터 광장(data.seoul.go.kr)';

// 02 の 3.2・3.4: 出典のリンクの中の見えない文言（画面の文言なので翻訳する）
const NEW_TAB = { ko: '(새 탭에서 열림)', ja: '(新しいタブで開きます)' } as const;

/** 見えない文言（cdk-visually-hidden）を除いた、画面に見える文字 */
function visibleText(el: Element): string {
  const clone = el.cloneNode(true) as Element;
  clone.querySelectorAll('.cdk-visually-hidden').forEach((hidden) => hidden.remove());
  return clone.textContent ?? '';
}

describe('Attribution (02 3.2, 07 4.2.1)', () => {
  afterEach(() => {
    document.documentElement.lang = '';
  });

  /** 記憶できないブラウザとして扱い、言語の切り替えをその場で反映させる（読み込み直さない） */
  function withoutStorage(): void {
    TestBed.configureTestingModule({ providers: [{ provide: BROWSER_STORAGE, useValue: null }] });
  }

  async function render() {
    const fixture = TestBed.createComponent(Attribution);
    await fixture.whenStable();
    return fixture;
  }

  function links(el: HTMLElement): HTMLAnchorElement[] {
    return Array.from(el.querySelectorAll('a'));
  }

  function expectOriginalTextAndLinks(el: HTMLElement, lang: 'ko' | 'ja'): void {
    const lines = Array.from(el.querySelectorAll('p'));
    expect(lines.map(visibleText)).toEqual([LINE1, LINE2]);

    // 1行目はリンクなし
    expect(lines[0].querySelectorAll('a').length).toBe(0);

    // 2行目はサイト名の2つだけがリンク
    const anchors = Array.from(lines[1].querySelectorAll('a'));
    expect(anchors.map(visibleText)).toEqual(['공공데이터포털', '서울 열린데이터 광장']);
    expect(anchors.map((a) => a.getAttribute('href'))).toEqual([
      'https://www.data.go.kr',
      'https://data.seoul.go.kr/dataList/OA-2751/F/1/datasetView.do',
    ]);
    for (const a of anchors) {
      expect(a.getAttribute('target')).toBe('_blank');
      expect(a.getAttribute('rel')).toBe('noopener noreferrer');
      expect(a.classList.contains('ct-source-link')).toBe(true);
      expectNewTabText(a, lang);
    }
  }

  /** リンクの文字の直後（空白なし）に見えない文言の <span> が1つだけあり、aria-label はない（U-4） */
  function expectNewTabText(a: HTMLAnchorElement, lang: 'ko' | 'ja'): void {
    expect(a.hasAttribute('aria-label')).toBe(false);
    expect(a.children.length).toBe(1);
    const hidden = a.children[0] as HTMLElement;
    expect(hidden.tagName).toBe('SPAN');
    expect(hidden.classList.contains('cdk-visually-hidden')).toBe(true);
    expect(hidden.textContent).toBe(NEW_TAB[lang]);
    // 外側の lang="ko" の中で表示言語の発音で読ませる
    expect(hidden.getAttribute('lang')).toBe(lang);
    expect(a.lastChild).toBe(hidden);
    // 読み上げの名前は「リンクの文字 + 文言」（間に空白を入れない。02 の 3.2 の表記どおり）
    expect(a.textContent).toBe(visibleText(a) + NEW_TAB[lang]);
  }

  it('data joins to the original strings without the new-tab text', () => {
    expect(ATTRIBUTION_LINES.map(attributionText)).toEqual([LINE1, LINE2]);
    const data = JSON.stringify(ATTRIBUTION_LINES);
    for (const text of Object.values(NEW_TAB)) {
      expect(data.includes(text)).toBe(false);
    }
  });

  it('shows the original text and links only the two site names (Korean)', async () => {
    const el = (await render()).nativeElement as HTMLElement;
    expectOriginalTextAndLinks(el, 'ko');
    expect(el.getAttribute('lang')).toBe('ko');
  });

  it('does not link the domains in parentheses', async () => {
    const el = (await render()).nativeElement as HTMLElement;
    const linked = links(el).map(visibleText);
    for (const domain of ['www.kopis.or.kr', 'www.data.go.kr', 'data.seoul.go.kr']) {
      expect(linked.some((text) => text.includes(domain))).toBe(false);
    }
  });

  it('is not translated in Japanese, but the new-tab text is (02 3.2)', async () => {
    withoutStorage();
    TestBed.inject(LanguageService).switchTo('ja');
    expect(document.documentElement.lang).toBe('ja');
    const el = (await render()).nativeElement as HTMLElement;
    expectOriginalTextAndLinks(el, 'ja');
    expect(el.getAttribute('lang')).toBe('ko');
  });

  it('changes the new-tab text when the language is switched', async () => {
    withoutStorage();
    const fixture = await render();
    const el = fixture.nativeElement as HTMLElement;
    expect(links(el).map((a) => a.textContent)).toEqual([
      '공공데이터포털' + NEW_TAB.ko,
      '서울 열린데이터 광장' + NEW_TAB.ko,
    ]);

    TestBed.inject(LanguageService).switchTo('ja');
    await fixture.whenStable();
    expect(links(el).map((a) => a.textContent)).toEqual([
      '공공데이터포털' + NEW_TAB.ja,
      '서울 열린데이터 광장' + NEW_TAB.ja,
    ]);
    for (const a of links(el)) {
      expectNewTabText(a, 'ja');
    }
    // 原文は変わらない
    expect(Array.from(el.querySelectorAll('p')).map(visibleText)).toEqual([LINE1, LINE2]);
  });

  it('loads the cdk-visually-hidden class globally (07 4.2.1)', () => {
    // v22 の mat.core() は空で CDK のクラスは部品を使ったときしか読み込まれないため、全体のスタイルで出していることを確かめる。
    // 実際に画面に出ないことはヘッドレス Chrome で確かめた
    expect(globalStyles).toMatch(/^@use '@angular\/cdk' as cdk;$/m);
    expect(globalStyles).toMatch(/^@include cdk\.a11y-visually-hidden\(\);$/m);
  });
});
