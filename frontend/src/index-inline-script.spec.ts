import { TestBed } from '@angular/core/testing';
import { LanguageService, RELOAD_PAGE } from './app/core/i18n/language.service';
import { BROWSER_STORAGE } from './app/core/storage/safe-storage';
import { STORAGE_KEYS } from './app/core/storage/storage-keys';
import { ThemeService } from './app/core/theme/theme.service';
import indexHtml from './index.html' with { loader: 'text' };

/**
 * index.html のインラインスクリプト（最初の描画の前に lang と color-scheme を決める。F-10・F-36、02 の 3.8）のテスト。
 * 同じ判定が ThemeService・LanguageService にも予備としてあるので、両者が同じ結果になることも確かめ、
 * キー名・判定の順番のずれを検出する。
 */

interface Case {
  /** localStorage に入れる値（キーは STORAGE_KEYS。ずれていれば読めずに失敗する） */
  stored: { lang?: string; colorScheme?: string };
  prefersDark: boolean;
  /** 'getter': localStorage へのアクセス自体が例外 / 'getItem': 読み出しが例外 */
  storageThrows?: 'getter' | 'getItem';
}

function inlineScriptSource(): string {
  const doc = new DOMParser().parseFromString(indexHtml, 'text/html');
  const scripts = Array.from(doc.querySelectorAll('head > script:not([src])'));
  expect(scripts.length).toBe(1);
  return scripts[0].textContent ?? '';
}

function fakeStorage(c: Case): Storage {
  const values = new Map<string, string>();
  if (c.stored.lang !== undefined) {
    values.set(STORAGE_KEYS.language, c.stored.lang);
  }
  if (c.stored.colorScheme !== undefined) {
    values.set(STORAGE_KEYS.colorScheme, c.stored.colorScheme);
  }
  return {
    length: values.size,
    clear: () => values.clear(),
    key: () => null,
    removeItem: (k: string) => void values.delete(k),
    setItem: (k: string, v: string) => void values.set(k, v),
    getItem: (k: string) => {
      if (c.storageThrows === 'getItem') {
        throw new Error('SecurityError');
      }
      return values.get(k) ?? null;
    },
  };
}

function matchMediaFor(prefersDark: boolean) {
  return (query: string) =>
    ({
      matches: query === '(prefers-color-scheme: dark)' && prefersDark,
      media: query,
      addListener: () => undefined,
      removeListener: () => undefined,
    }) as unknown as MediaQueryList;
}

/** インラインスクリプトを、偽の window・document で実行する */
function runInlineScript(c: Case): { lang: string; colorScheme: string } {
  const root = document.implementation.createHTMLDocument('').documentElement;
  const storage = fakeStorage(c);
  const fakeWindow = {
    get localStorage(): Storage {
      if (c.storageThrows === 'getter') {
        throw new Error('SecurityError');
      }
      return storage;
    },
    matchMedia: matchMediaFor(c.prefersDark),
  };
  new Function('window', 'document', inlineScriptSource())(fakeWindow, { documentElement: root });
  return { lang: root.lang, colorScheme: root.style.colorScheme };
}

/** 予備の判定（インラインスクリプトが動かなかったとき）でサービスが出す結果 */
function runServices(c: Case): { lang: string; colorScheme: string } {
  const storage: Storage | null = c.storageThrows === 'getter' ? null : fakeStorage(c);
  TestBed.configureTestingModule({
    providers: [
      { provide: BROWSER_STORAGE, useValue: storage },
      { provide: RELOAD_PAGE, useValue: () => undefined },
    ],
  });
  return {
    lang: TestBed.inject(LanguageService).lang(),
    colorScheme: TestBed.inject(ThemeService).scheme(),
  };
}

const cases: [string, Case, { lang: string; colorScheme: string }][] = [
  // 保存値なし → 端末の設定（F-36 ②）、言語は韓国語（F-10 ①）
  [
    'nothing stored, device light',
    { stored: {}, prefersDark: false },
    { lang: 'ko', colorScheme: 'light' },
  ],
  [
    'nothing stored, device dark',
    { stored: {}, prefersDark: true },
    { lang: 'ko', colorScheme: 'dark' },
  ],
  // 保存値あり → 端末の設定より優先（F-36 ③、F-10 ③）
  [
    'stored light, device dark',
    { stored: { colorScheme: 'light' }, prefersDark: true },
    { lang: 'ko', colorScheme: 'light' },
  ],
  [
    'stored dark, device light',
    { stored: { colorScheme: 'dark' }, prefersDark: false },
    { lang: 'ko', colorScheme: 'dark' },
  ],
  [
    'stored ja and light, device dark',
    { stored: { lang: 'ja', colorScheme: 'light' }, prefersDark: true },
    { lang: 'ja', colorScheme: 'light' },
  ],
  [
    'stored ko, device light',
    { stored: { lang: 'ko' }, prefersDark: false },
    { lang: 'ko', colorScheme: 'light' },
  ],
  // 不正な値は保存なしと同じ
  [
    'invalid values, device dark',
    { stored: { lang: 'en', colorScheme: 'blue' }, prefersDark: true },
    { lang: 'ko', colorScheme: 'dark' },
  ],
  [
    'invalid values, device light',
    { stored: { lang: '', colorScheme: 'DARK' }, prefersDark: false },
    { lang: 'ko', colorScheme: 'light' },
  ],
  // localStorage が例外を投げても止まらず、端末の設定・韓国語になる
  [
    'storage access throws, device dark',
    { stored: { lang: 'ja', colorScheme: 'light' }, prefersDark: true, storageThrows: 'getter' },
    { lang: 'ko', colorScheme: 'dark' },
  ],
  [
    'storage access throws, device light',
    { stored: {}, prefersDark: false, storageThrows: 'getter' },
    { lang: 'ko', colorScheme: 'light' },
  ],
  [
    'getItem throws, device dark',
    { stored: { lang: 'ja', colorScheme: 'light' }, prefersDark: true, storageThrows: 'getItem' },
    { lang: 'ko', colorScheme: 'dark' },
  ],
];

describe('index.html inline script (F-10, F-36)', () => {
  const root = document.documentElement;
  const originalMatchMedia = Object.getOwnPropertyDescriptor(window, 'matchMedia');

  afterEach(() => {
    root.style.colorScheme = '';
    root.lang = '';
    if (originalMatchMedia) {
      Object.defineProperty(window, 'matchMedia', originalMatchMedia);
    } else {
      delete (window as { matchMedia?: unknown }).matchMedia;
    }
  });

  it.each(cases)('%s', (_name, c, expected) => {
    expect(runInlineScript(c)).toEqual(expected);
  });

  // キー名・判定の順番が index.html とサービスでずれていないこと
  it.each(cases)('services agree with the inline script: %s', (_name, c, expected) => {
    // インラインスクリプトが動かなかった状態（<html> に値がない）からサービスに判定させる
    root.style.colorScheme = '';
    Object.defineProperty(window, 'matchMedia', {
      configurable: true,
      writable: true,
      value: matchMediaFor(c.prefersDark),
    });
    expect(runServices(c)).toEqual(expected);
  });

  it('reads the same storage keys as the services', () => {
    const source = inlineScriptSource();
    expect(source).toContain(`'${STORAGE_KEYS.language}'`);
    expect(source).toContain(`'${STORAGE_KEYS.colorScheme}'`);
  });
});
