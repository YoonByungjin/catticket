import { TestBed } from '@angular/core/testing';
import { DateRange } from './date-range';

describe('DateRange (P-18, 07 4.6)', () => {
  async function render(start: string, end: string): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(DateRange);
    fixture.componentRef.setInput('start', start);
    fixture.componentRef.setInput('end', end);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('renders the P-18 form and keeps each date on one line (07 4.6)', async () => {
    const el = await render('2026-10-26', '2026-11-01');
    // 区切りの前後に空白を入れない（07 の 4.6）
    expect(el.textContent).toBe('2026-10-26~2026-11-01');
    expect(el.querySelectorAll('.ct-date').length).toBe(2);
  });

  it('renders one date for a single day', async () => {
    const el = await render('2026-11-04', '2026-11-04');
    expect(el.textContent?.trim()).toBe('2026-11-04');
  });
});
