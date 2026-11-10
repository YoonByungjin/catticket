import { TestBed } from '@angular/core/testing';
import { BROWSER_STORAGE } from '../storage/safe-storage';
import { STORAGE_KEYS } from '../storage/storage-keys';
import { LanguageService, RELOAD_PAGE, formatMessage } from './language.service';
import { ja } from './messages.ja';
import { ko } from './messages.ko';

describe('LanguageService', () => {
  let reload: ReturnType<typeof vi.fn>;

  function setup(storage: Storage | null = localStorage): LanguageService {
    reload = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        { provide: RELOAD_PAGE, useValue: reload },
        { provide: BROWSER_STORAGE, useValue: storage },
      ],
    });
    return TestBed.inject(LanguageService);
  }

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.lang = '';
  });
  afterEach(() => localStorage.clear());

  it('starts in Korean when nothing is stored (F-10 ①)', () => {
    const service = setup();
    expect(service.lang()).toBe('ko');
    expect(document.documentElement.lang).toBe('ko');
    expect(service.t('header.login')).toBe('로그인');
  });

  it('uses the stored language (F-10 ③)', () => {
    localStorage.setItem(STORAGE_KEYS.language, 'ja');
    const service = setup();
    expect(service.lang()).toBe('ja');
    expect(document.documentElement.lang).toBe('ja');
    expect(service.t('header.login')).toBe('ログイン');
  });

  it('ignores an unknown stored value', () => {
    localStorage.setItem(STORAGE_KEYS.language, 'en');
    expect(setup().lang()).toBe('ko');
  });

  it('stores the language and reloads the page on switch (02 3.1)', () => {
    const service = setup();
    service.switchTo('ja');
    expect(localStorage.getItem(STORAGE_KEYS.language)).toBe('ja');
    expect(reload).toHaveBeenCalledTimes(1);
  });

  it('does nothing when switching to the current language', () => {
    const service = setup();
    service.switchTo('ko');
    expect(reload).not.toHaveBeenCalled();
    expect(localStorage.getItem(STORAGE_KEYS.language)).toBeNull();
  });

  it('switches in place without reloading when storage is unavailable', () => {
    const service = setup(null);
    service.switchTo('ja');
    expect(reload).not.toHaveBeenCalled();
    expect(service.lang()).toBe('ja');
    expect(document.documentElement.lang).toBe('ja');
    expect(service.t('header.logout')).toBe('ログアウト');
  });
});

describe('formatMessage', () => {
  it('replaces {name} placeholders', () => {
    expect(formatMessage('{count}건 / {name}', { count: 3, name: 'Cat' })).toBe('3건 / Cat');
  });

  it('keeps unknown placeholders and returns text as is without params', () => {
    expect(formatMessage('{count}건', { other: 1 })).toBe('{count}건');
    expect(formatMessage('{count}건')).toBe('{count}건');
  });
});

describe('message dictionaries', () => {
  it('have the same keys in Korean and Japanese', () => {
    expect(Object.keys(ja).sort()).toEqual(Object.keys(ko).sort());
  });

  it('have no empty text', () => {
    for (const dict of [ko, ja]) {
      for (const [key, text] of Object.entries(dict)) {
        expect(text.trim(), key).not.toBe('');
      }
    }
  });
});
