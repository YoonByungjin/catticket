import { DOCUMENT, Injectable, InjectionToken, inject, signal } from '@angular/core';
import { SafeStorage } from '../storage/safe-storage';
import { STORAGE_KEYS } from '../storage/storage-keys';
import { DEFAULT_LANG, Lang, MessageKey, Messages, isLang } from './messages';
import { ja } from './messages.ja';
import { ko } from './messages.ko';

const DICTIONARIES: Record<Lang, Messages> = { ko, ja };

/** `{name}` を params の値で置き換える。params にない名前はそのまま残す（抜けに気づけるように） */
export function formatMessage(
  text: string,
  params?: Readonly<Record<string, string | number>>,
): string {
  if (!params) {
    return text;
  }
  return text.replace(/\{(\w+)\}/g, (match, name: string) =>
    name in params ? String(params[name]) : match,
  );
}

/** ページの読み込み直し。テストで差し替えるためトークンにする */
export const RELOAD_PAGE = new InjectionToken<() => void>('RELOAD_PAGE', {
  providedIn: 'root',
  factory: () => {
    const doc = inject(DOCUMENT);
    return () => doc.defaultView?.location.reload();
  },
});

/**
 * 表示言語（F-10）と画面の文言の辞書。
 * 記憶した言語は src/index.html のインラインスクリプトが最初の描画の前に <html lang> へ反映する。
 */
@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly doc = inject(DOCUMENT);
  private readonly storage = inject(SafeStorage);
  private readonly reloadPage = inject(RELOAD_PAGE);

  private readonly current = signal<Lang>(this.resolveInitial());
  /** いまの表示言語 */
  readonly lang = this.current.asReadonly();

  constructor() {
    this.applyToDocument(this.current());
  }

  /** 辞書の文言。`{name}` を params の値で置き換える */
  t(key: MessageKey, params?: Readonly<Record<string, string | number>>): string {
    return formatMessage(DICTIONARIES[this.current()][key], params);
  }

  /**
   * 表示言語を切り替える。記憶してからページを読み込み直す（02 の 3.1、ADR 0006 決定3）。
   * 記憶できないブラウザでは読み込み直すと韓国語に戻るため、その場で文言だけ切り替える。
   */
  switchTo(lang: Lang): void {
    if (lang === this.current()) {
      return;
    }
    if (this.storage.set(STORAGE_KEYS.language, lang)) {
      this.reloadPage();
      return;
    }
    this.current.set(lang);
    this.applyToDocument(lang);
  }

  private resolveInitial(): Lang {
    const stored = this.storage.get(STORAGE_KEYS.language);
    return isLang(stored) ? stored : DEFAULT_LANG;
  }

  private applyToDocument(lang: Lang): void {
    // 07 の 2.2.1: フォントの並びは html:lang() で切り替わる
    this.doc.documentElement.lang = lang;
  }
}
