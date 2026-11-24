import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Attribution } from '../attribution/attribution';

/** CM-02 フッター（02 の 3.2、07 の 3.・4.2）。出典の2行は SC-01 のダイアログと共通の部品で出す */
@Component({
  selector: 'app-footer',
  imports: [Attribution],
  template: `
    <footer class="ct-footer">
      <app-attribution />
    </footer>
  `,
  styles: `
    @use '../../../theme/breakpoints' as bp;

    .ct-footer {
      // 画面の左右の余白: PC 表示 24px、スマホ表示 16px（07 の 2.4）。ヘッダーと同じ境目で端をそろえる
      padding: 16px 24px;
      background: var(--mat-sys-surface-container-low);
      color: var(--mat-sys-on-surface-variant);
      font: var(--mat-sys-body-small);

      @media #{bp.$handset} {
        padding-right: 16px;
        padding-left: 16px;
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Footer {}
