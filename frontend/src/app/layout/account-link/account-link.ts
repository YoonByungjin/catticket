import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  afterRenderEffect,
  computed,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { LanguageService } from '../../core/i18n/language.service';

/** 文言の `{name}` の前後（「계정 (」と「)」）。ニックネームの部分だけを切るために分ける */
export function splitAroundName(text: string): { prefix: string; suffix: string } {
  const at = text.indexOf('{name}');
  if (at < 0) {
    return { prefix: text, suffix: '' };
  }
  return { prefix: text.slice(0, at), suffix: text.slice(at + '{name}'.length) };
}

/**
 * 一般ユーザーのアカウントのリンク「계정 (ニックネーム)」（02 の 3.1、07 の 4.1.1）。
 * PC 表示のヘッダーとスマホ表示のメニューで同じ部品を使う。
 * mat-button は内部のラベル（flex の項目で min-width: auto）が縮まず、そこへ CSS を書けないため、
 * 07 の 4.1.1「それでも縮まないとき」のとおり普通の <a> にする。
 */
@Component({
  selector: 'app-account-link',
  imports: [RouterLink, RouterLinkActive, MatTooltipModule],
  templateUrl: './account-link.html',
  styleUrl: './account-link.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'ct-account',
    '[class.ct-account--menu]': "place() === 'menu'",
  },
})
export class AccountLink {
  private readonly language = inject(LanguageService);

  /** 全体のニックネーム（03 の 7.1 の正規化した形） */
  readonly nickname = input.required<string>();
  readonly path = input('/account');
  /** 置く場所。メニューの中では項目の余白をそろえる */
  readonly place = input<'header' | 'menu'>('header');

  private readonly name = viewChild.required<ElementRef<HTMLElement>>('name');

  /** ニックネームの部分が「…」で切れているか。切れたときだけツールチップを出す（決定 No.7） */
  protected readonly truncated = signal(false);

  protected readonly parts = computed(() => splitAroundName(this.language.t('header.nav.account')));
  /** 切れていても全体のニックネームを入れる（決定 No.6） */
  protected readonly ariaLabel = computed(() =>
    this.language.t('header.nav.account', { name: this.nickname() }),
  );

  constructor() {
    // ニックネーム・表示言語（文言とフォントが変わる）が変わったら、描画の後に判定し直す
    afterRenderEffect(() => {
      this.nickname();
      this.language.lang();
      this.measure();
    });

    // 画面の幅が変わると <span> の幅が変わる。jsdom など ResizeObserver のない環境では見張らない
    const destroyRef = inject(DestroyRef);
    afterNextRender(() => {
      if (typeof ResizeObserver === 'undefined') {
        return;
      }
      const observer = new ResizeObserver(() => this.measure());
      observer.observe(this.name().nativeElement);
      destroyRef.onDestroy(() => observer.disconnect());
    });
  }

  /** 文字数ではなく、表示の幅と中身の幅で判定する（07 の 4.1.1） */
  private measure(): void {
    const el = this.name().nativeElement;
    this.truncated.set(el.scrollWidth > el.clientWidth);
  }
}
