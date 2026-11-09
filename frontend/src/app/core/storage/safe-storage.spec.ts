import { TestBed } from '@angular/core/testing';
import { BROWSER_STORAGE, SafeStorage } from './safe-storage';

function throwingStorage(): Storage {
  return {
    length: 0,
    clear: () => undefined,
    key: () => null,
    removeItem: () => undefined,
    getItem: () => {
      throw new Error('SecurityError');
    },
    setItem: () => {
      throw new Error('QuotaExceededError');
    },
  };
}

describe('SafeStorage', () => {
  afterEach(() => localStorage.clear());

  it('reads and writes localStorage', () => {
    const storage = TestBed.inject(SafeStorage);
    expect(storage.set('k', 'v')).toBe(true);
    expect(storage.get('k')).toBe('v');
    expect(storage.get('missing')).toBeNull();
  });

  it('returns null / false instead of throwing when storage throws', () => {
    TestBed.configureTestingModule({
      providers: [{ provide: BROWSER_STORAGE, useValue: throwingStorage() }],
    });
    const storage = TestBed.inject(SafeStorage);
    expect(storage.get('k')).toBeNull();
    expect(storage.set('k', 'v')).toBe(false);
  });

  it('returns null / false when storage is unavailable', () => {
    TestBed.configureTestingModule({
      providers: [{ provide: BROWSER_STORAGE, useValue: null }],
    });
    const storage = TestBed.inject(SafeStorage);
    expect(storage.get('k')).toBeNull();
    expect(storage.set('k', 'v')).toBe(false);
  });
});
