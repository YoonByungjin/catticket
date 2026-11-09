import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { MessageKey } from '../../core/i18n/messages';
import { TranslatePipe } from '../../core/i18n/translate.pipe';

/** SC-99 の場合（02 の SC-99） */
export type ErrorReason = 'page-not-found' | 'performance-not-found' | 'forbidden';

const TITLE_KEYS: Record<ErrorReason, MessageKey> = {
  'page-not-found': 'error.pageNotFound',
  'performance-not-found': 'error.performanceNotFound',
  forbidden: 'error.forbidden',
};

/**
 * SC-99 エラー（02 の SC-99、07 の 5.）。
 * 専用の URL はなく、ない URL（ルートの '**'）や、各画面が「見つからない・権限がない」ときに表示する。
 */
@Component({
  selector: 'app-error-page',
  imports: [MatButtonModule, RouterLink, TranslatePipe],
  template: `
    <section class="ct-error">
      <h1>{{ titleKey() | translate }}</h1>
      <a mat-flat-button routerLink="/">{{ 'error.goToMap' | translate }}</a>
    </section>
  `,
  styles: `
    .ct-error {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 24px;
      padding: 48px 16px;
      text-align: center;
    }
    h1 {
      margin: 0;
      font: var(--mat-sys-headline-small);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErrorPage {
  readonly reason = input<ErrorReason>('page-not-found');

  protected readonly titleKey = computed(() => TITLE_KEYS[this.reason()]);
}
