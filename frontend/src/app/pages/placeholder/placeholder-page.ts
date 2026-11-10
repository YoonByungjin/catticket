import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * 画面の中身ができるまでの仮の画面（Issue #24 はルーティングの枠だけ）。
 * 画面 ID はルートの data から受け取る。中身は Wave 2 の各 Issue で置き換える。
 */
@Component({
  selector: 'app-placeholder-page',
  template: `<section class="ct-placeholder" [attr.data-screen-id]="screenId()"></section>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlaceholderPage {
  readonly screenId = input.required<string>();
}
