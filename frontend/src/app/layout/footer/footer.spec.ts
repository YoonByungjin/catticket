import { TestBed } from '@angular/core/testing';
import { Footer } from './footer';

describe('Footer (CM-02)', () => {
  it('shows the two attribution lines with the shared component (02 3.2)', async () => {
    const fixture = TestBed.createComponent(Footer);
    await fixture.whenStable();
    const footer = (fixture.nativeElement as HTMLElement).querySelector('footer') as HTMLElement;
    const attribution = footer.querySelector('app-attribution') as HTMLElement;
    expect(attribution).not.toBeNull();
    expect(attribution.getAttribute('lang')).toBe('ko');
    const lines = Array.from(footer.querySelectorAll('p')).map((p) => p.textContent);
    expect(lines[0]).toBe('출처: (재)예술경영지원센터 공연예술통합전산망(www.kopis.or.kr)');
    expect(lines[1]).toBe(
      '출처: 서울교통공사 공공데이터(역 좌표·주소, 물품보관함 위치, 역명 다국어 표기) - 공공데이터포털(www.data.go.kr), 서울 열린데이터 광장(data.seoul.go.kr)',
    );
    expect(footer.querySelectorAll('a.ct-source-link').length).toBe(2);
  });
});
