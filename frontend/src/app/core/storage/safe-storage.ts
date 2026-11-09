import { DOCUMENT, Injectable, InjectionToken, inject } from '@angular/core';

/** テストで差し替えられるように localStorage をトークンで受け取る */
export const BROWSER_STORAGE = new InjectionToken<Storage | null>('BROWSER_STORAGE', {
  providedIn: 'root',
  factory: () => {
    // プライベートブラウズや Cookie の拒否で localStorage へのアクセス自体が例外になることがある
    try {
      return inject(DOCUMENT).defaultView?.localStorage ?? null;
    } catch {
      return null;
    }
  },
});

/**
 * localStorage を例外を出さずに使うための薄いラッパー。
 * 使えないブラウザでも画面が壊れないよう、失敗は null / false で返す。
 */
@Injectable({ providedIn: 'root' })
export class SafeStorage {
  private readonly storage = inject(BROWSER_STORAGE);

  get(key: string): string | null {
    try {
      return this.storage?.getItem(key) ?? null;
    } catch {
      return null;
    }
  }

  /** 保存できたら true */
  set(key: string, value: string): boolean {
    if (!this.storage) {
      return false;
    }
    try {
      this.storage.setItem(key, value);
      return true;
    } catch {
      return false;
    }
  }
}
