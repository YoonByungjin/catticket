import { TestBed } from '@angular/core/testing';
import { BROWSER_STORAGE } from '../storage/safe-storage';
import { STORAGE_KEYS } from '../storage/storage-keys';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  const root = document.documentElement;
  let prefersDark: boolean;
  const original = Object.getOwnPropertyDescriptor(window, 'matchMedia');

  beforeEach(() => {
    localStorage.clear();
    root.style.colorScheme = '';
    prefersDark = false;
    // jsdom には matchMedia がないので、端末の設定を差し替えられる形で置く
    Object.defineProperty(window, 'matchMedia', {
      configurable: true,
      writable: true,
      value: vi.fn(
        (query: string) =>
          ({
            matches: query === '(prefers-color-scheme: dark)' && prefersDark,
            media: query,
            addListener: () => undefined,
            removeListener: () => undefined,
          }) as unknown as MediaQueryList,
      ),
    });
  });

  afterEach(() => {
    // ほかのテスト（CDK の BreakpointObserver）に差し替えを残さない
    if (original) {
      Object.defineProperty(window, 'matchMedia', original);
    } else {
      delete (window as { matchMedia?: unknown }).matchMedia;
    }
    localStorage.clear();
    root.style.colorScheme = '';
  });

  it('keeps the scheme decided before the first paint (index.html)', () => {
    root.style.colorScheme = 'dark';
    localStorage.setItem(STORAGE_KEYS.colorScheme, 'light');
    expect(TestBed.inject(ThemeService).scheme()).toBe('dark');
    expect(window.matchMedia).not.toHaveBeenCalled();
  });

  it('follows the device setting when never switched (F-36 ②)', () => {
    prefersDark = true;
    const service = TestBed.inject(ThemeService);
    expect(service.scheme()).toBe('dark');
    expect(root.style.colorScheme).toBe('dark');
  });

  it('uses light when the device setting is not dark (F-36 ②)', () => {
    expect(TestBed.inject(ThemeService).scheme()).toBe('light');
    expect(root.style.colorScheme).toBe('light');
  });

  it('prefers the stored scheme over the device setting (F-36 ③)', () => {
    prefersDark = true;
    localStorage.setItem(STORAGE_KEYS.colorScheme, 'light');
    expect(TestBed.inject(ThemeService).scheme()).toBe('light');
  });

  it('toggles, applies to <html> and stores the choice (F-36 ①③)', () => {
    const service = TestBed.inject(ThemeService);
    service.toggle();
    expect(service.scheme()).toBe('dark');
    expect(root.style.colorScheme).toBe('dark');
    expect(localStorage.getItem(STORAGE_KEYS.colorScheme)).toBe('dark');

    service.toggle();
    expect(service.scheme()).toBe('light');
    expect(root.style.colorScheme).toBe('light');
    expect(localStorage.getItem(STORAGE_KEYS.colorScheme)).toBe('light');
  });

  it('still toggles when storage is unavailable', () => {
    TestBed.configureTestingModule({ providers: [{ provide: BROWSER_STORAGE, useValue: null }] });
    const service = TestBed.inject(ThemeService);
    service.toggle();
    expect(service.scheme()).toBe('dark');
    expect(root.style.colorScheme).toBe('dark');
  });
});
