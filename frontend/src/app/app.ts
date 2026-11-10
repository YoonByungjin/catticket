import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
  untracked,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
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

  private readonly menu = viewChild(MatSidenav);
  /**
   * 開閉の状態は mat-sidenav だけが持つ（opened は signal なので computed で追える）。
   * [opened] と (openedChange) で別の signal と双方向に結ぶと、アニメーションの後に遅れて届く
   * openedChange が、閉じた直後の状態を「開いている」に上書きすることがあるため。
   */
  protected readonly menuOpen = computed(() => this.menu()?.opened ?? false);

  constructor() {
    // 画面を移ったらメニューを閉じる
    inject(Router)
      .events.pipe(
        filter((e) => e instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.closeMenu());

    // PC 表示に変わったらメニューは不要（ヘッダーに項目が出る）
    effect(() => {
      if (!this.handset()) {
        untracked(() => this.closeMenu());
      }
    });
  }

  protected toggleMenu(): void {
    void this.menu()?.toggle();
  }

  private closeMenu(): void {
    void this.menu()?.close();
  }

  protected onLogout(): void {
    // ログアウト（API-32）は Wave 2 の認証の Issue で実装する
  }
}
