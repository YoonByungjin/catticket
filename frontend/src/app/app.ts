import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthMeResponse } from './core/api/api.types';
import { TranslatePipe } from './core/i18n/translate.pipe';
import { ViewportService } from './core/viewport/viewport.service';
import { Footer } from './layout/footer/footer';
import { Header } from './layout/header/header';
import { SIGNED_OUT, accountLinksFor, navLinksFor } from './layout/nav-items';

/**
 * 全画面の枠: CM-01 ヘッダー・画面・CM-02 フッター。
 * スマホ表示のメニューは右から開く mat-sidenav（07 の 3.）。
 */
@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatListModule,
    MatButtonModule,
    TranslatePipe,
    Header,
    Footer,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  protected readonly handset = inject(ViewportService).handset;

  /** ログイン状態。API-30 の取得は Wave 2 の認証の Issue で入れる */
  protected readonly auth = signal<AuthMeResponse>(SIGNED_OUT);
  protected readonly navLinks = computed(() => navLinksFor(this.auth()));
  protected readonly accountLinks = computed(() => accountLinksFor(this.auth()));

  protected readonly menuOpen = signal(false);

  constructor() {
    // 画面を移ったらメニューを閉じる
    inject(Router)
      .events.pipe(
        filter((e) => e instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.menuOpen.set(false));

    // PC 表示に変わったらメニューは不要（ヘッダーに項目が出る）
    effect(() => {
      if (!this.handset()) {
        this.menuOpen.set(false);
      }
    });
  }

  protected toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  protected onLogout(): void {
    // ログアウト（API-32）は Wave 2 の認証の Issue で実装する
  }
}
