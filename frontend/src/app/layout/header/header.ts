import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthMeResponse } from '../../core/api/api.types';
import { LanguageService } from '../../core/i18n/language.service';
import { Lang, MessageKey, isLang } from '../../core/i18n/messages';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ThemeService } from '../../core/theme/theme.service';
import { ViewportService } from '../../core/viewport/viewport.service';
import { SIGNED_OUT, accountLinksFor, navLinksFor } from '../nav-items';

/** サービス名（固有名詞なので辞書に入れない） */
export const SERVICE_NAME = 'CatTicket';

/** 言語の切り替えの表示名は各言語で書き、切り替えても変えない（07 の 3.） */
export const LANGUAGE_OPTIONS: readonly { value: Lang; label: string }[] = [
  { value: 'ko', label: '한국어' },
  { value: 'ja', label: '日本語' },
];

/**
 * CM-01 ヘッダー（02 の 3.1、07 の 3.・4.1）。
 * スマホ表示ではサービス名・言語・ライト/ダークの切り替え以外をメニュー（menuToggle）に回す。
 */
@Component({
  selector: 'app-header',
  imports: [
    MatToolbarModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatIconModule,
    RouterLink,
    RouterLinkActive,
    TranslatePipe,
  ],
  templateUrl: './header.html',
  styleUrl: './header.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Header {
  private readonly language = inject(LanguageService);
  private readonly theme = inject(ThemeService);

  /** ログイン状態（API-30）。取得は Wave 2 の認証の Issue で行う */
  readonly auth = input<AuthMeResponse>(SIGNED_OUT);
  /** スマホ表示のメニューが開いているか（aria-expanded） */
  readonly menuOpen = input(false);

  readonly menuToggle = output<void>();
  readonly logout = output<void>();

  protected readonly serviceName = SERVICE_NAME;
  protected readonly languageOptions = LANGUAGE_OPTIONS;
  protected readonly handset = inject(ViewportService).handset;
  protected readonly lang = this.language.lang;
  protected readonly scheme = this.theme.scheme;

  protected readonly navLinks = computed(() => navLinksFor(this.auth()));
  protected readonly accountLinks = computed(() => accountLinksFor(this.auth()));

  /** アイコンとラベルは押した後の表示（07 の 3.） */
  protected readonly themeIcon = computed(() =>
    this.scheme() === 'dark' ? 'light_mode' : 'dark_mode',
  );
  protected readonly themeLabelKey = computed<MessageKey>(() =>
    this.scheme() === 'dark' ? 'header.theme.toLight' : 'header.theme.toDark',
  );

  protected switchLanguage(value: unknown): void {
    if (isLang(value)) {
      this.language.switchTo(value);
    }
  }

  protected toggleTheme(): void {
    this.theme.toggle();
  }
}
