import { TestBed } from '@angular/core/testing';
import { ATTRIBUTIONS, Footer } from './footer';

describe('Footer (CM-02)', () => {
  it('shows both attributions in Korean as is (02 3.2)', async () => {
    const fixture = TestBed.createComponent(Footer);
    await fixture.whenStable();
    const footer = fixture.nativeElement.querySelector('footer') as HTMLElement;
    const lines = Array.from(footer.querySelectorAll('p')).map((p) => p.textContent?.trim());
    expect(lines).toEqual([...ATTRIBUTIONS]);
    expect(lines[0]).toBe('출처: (재)예술경영지원센터 공연예술통합전산망(www.kopis.or.kr)');
    expect(footer.getAttribute('lang')).toBe('ko');
  });
});
