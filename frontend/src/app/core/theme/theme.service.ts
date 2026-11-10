import { DOCUMENT, Injectable, inject, signal } from '@angular/core';
import { SafeStorage } from '../storage/safe-storage';
import { STORAGE_KEYS } from '../storage/storage-keys';

/** ライト表示・ダーク表示（F-36） */
export type ColorScheme = 'light' | 'dark';

export function isColorScheme(value: unknown): value is ColorScheme {
  return value === 'light' || value === 'dark';
}

/**
 * ライト表示・ダーク表示の切り替え（F-36、02 の 3.8、07 の 2.1.5）。
 * 最初の表示は src/index.html のインラインスクリプトが描画の前に <html> の color-scheme に決める。
 * このサービスはその値を引き継ぎ、端末の設定をもう一度見ない（開いたときに1回だけ見る。02 の 3.8）。
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly doc = inject(DOCUMENT);
  private readonly storage = inject(SafeStorage);

  private readonly current = signal<ColorScheme>(this.resolveInitial());
  /** いまの表示 */
  readonly scheme = this.current.asReadonly();

  constructor() {
    this.applyToDocument(this.current());
  }

  /** 押すたびにライト表示とダーク表示を入れ替え、記憶する。ページは読み込み直さない */
  toggle(): void {
    const next: ColorScheme = this.current() === 'dark' ? 'light' : 'dark';
    this.current.set(next);
    this.applyToDocument(next);
    // 記憶できなくても、この画面の間は切り替えた表示のまま使える
    this.storage.set(STORAGE_KEYS.colorScheme, next);
  }

  private resolveInitial(): ColorScheme {
    const applied = this.doc.documentElement.style.colorScheme;
    if (isColorScheme(applied)) {
      return applied;
    }
    // インラインスクリプトが動かなかったときの予備（同じ判定）
    const stored = this.storage.get(STORAGE_KEYS.colorScheme);
    if (isColorScheme(stored)) {
      return stored;
    }
    const prefersDark =
      this.doc.defaultView?.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
    return prefersDark ? 'dark' : 'light';
  }

  private applyToDocument(scheme: ColorScheme): void {
    // 07 の 2.1.5: Material のテーマは light-dark() で出すので、color-scheme だけで色が変わる
    this.doc.documentElement.style.colorScheme = scheme;
  }
}
