import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { BROWSER_STORAGE } from '../../core/storage/safe-storage';
import { ErrorPage, ErrorReason } from './error-page';

describe('ErrorPage (SC-99)', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: BROWSER_STORAGE, useValue: null }],
    });
  });

  async function render(reason?: ErrorReason): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(ErrorPage);
    if (reason) {
      fixture.componentRef.setInput('reason', reason);
    }
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows "page not found" with a link to SC-01 by default', async () => {
    const el = await render();
    expect(el.querySelector('h1')?.textContent?.trim()).toBe('페이지를 찾을 수 없습니다');
    expect(el.querySelector('a')?.getAttribute('href')).toBe('/');
  });

  it('shows the performance and forbidden cases', async () => {
    expect((await render('performance-not-found')).querySelector('h1')?.textContent?.trim()).toBe(
      '공연을 찾을 수 없습니다',
    );
    expect((await render('forbidden')).querySelector('h1')?.textContent?.trim()).toBe(
      '이 화면을 볼 권한이 없습니다',
    );
  });
});
