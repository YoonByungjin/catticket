import { TestBed } from '@angular/core/testing';
import { HANDSET_MEDIA_QUERY } from '../../core/viewport/viewport.service';
import { Footer } from './footer';

/** 見えない文言（cdk-visually-hidden）を除いた、画面に見える文字 */
function visibleText(el: Element): string {
  const clone = el.cloneNode(true) as Element;
  clone.querySelectorAll('.cdk-visually-hidden').forEach((hidden) => hidden.remove());
  return clone.textContent ?? '';
}

describe('Footer (CM-02)', () => {
  async function render(): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(Footer);
    await fixture.whenStable();
    return (fixture.nativeElement as HTMLElement).querySelector('footer') as HTMLElement;
  }

  it('shows the two attribution lines with the shared component (02 3.2)', async () => {
    const footer = await render();
    const attribution = footer.querySelector('app-attribution') as HTMLElement;
    expect(attribution).not.toBeNull();
    expect(attribution.getAttribute('lang')).toBe('ko');
    const lines = Array.from(footer.querySelectorAll('p')).map(visibleText);
    expect(lines[0]).toBe('출처: (재)예술경영지원센터 공연예술통합전산망(www.kopis.or.kr)');
    expect(lines[1]).toBe(
      '출처: 서울교통공사 공공데이터(역 좌표·주소, 물품보관함 위치, 역명 다국어 표기) - 공공데이터포털(www.data.go.kr), 서울 열린데이터 광장(data.seoul.go.kr)',
    );
    const links = Array.from(footer.querySelectorAll('a.ct-source-link'));
    expect(links.length).toBe(2);
    // 新しいタブの見えない文言（02 の 3.2、U-4）
    for (const a of links) {
      expect(a.querySelector('.cdk-visually-hidden')?.textContent).toBe('(새 탭에서 열림)');
    }
  });

  // jsdom はスタイルの優先順位をブラウザと同じには計算しない。実際の画面の値はヘッドレス Chrome で
  // 幅 959px・960px を実測して確かめた（ヘッダーと同じ。07 の 4.1.1）
  it('side padding: PC 24px (07 2.4)', async () => {
    const footer = await render();
    const style = getComputedStyle(footer);
    expect(style.paddingLeft).toBe('24px');
    expect(style.paddingRight).toBe('24px');
  });

  it('side padding on handset is 16px at the shared breakpoint (07 2.4)', async () => {
    await render();
    // jsdom はメディアクエリを評価しないため、境目の @media の中の .ct-footer の規則を読む
    const rules = Array.from(document.styleSheets)
      .flatMap((sheet) => Array.from(sheet.cssRules))
      .filter((rule): rule is CSSMediaRule => rule instanceof CSSMediaRule)
      .filter((rule) => rule.media.mediaText === HANDSET_MEDIA_QUERY)
      .flatMap((rule) => Array.from(rule.cssRules))
      .filter(
        (rule): rule is CSSStyleRule =>
          rule instanceof CSSStyleRule && /^\.ct-footer(\[|$)/.test(rule.selectorText),
      );
    expect(rules.length).toBe(1);
    expect(rules[0].style.paddingLeft).toBe('16px');
    expect(rules[0].style.paddingRight).toBe('16px');
  });
});
