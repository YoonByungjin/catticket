import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * 期間の表示（集計期間・公演期間。07 の 4.6）。日付の途中で折り返さず、折り返すなら `~` の後。
 */
@Component({
  selector: 'app-date-range',
  // 要素の間の空白だけのテキストは Angular が除くので、`~` の前後に空白は入らない
  template: `
    @for (part of parts(); track $index) {
      <span class="ct-date">{{ part }}</span>
    }
  `,
  styles: `
    :host {
      font-variant-numeric: tabular-nums;
    }
    .ct-date {
      white-space: nowrap;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DateRange {
  readonly start = input.required<string>();
  readonly end = input.required<string>();

  /** 折り返すなら区切りの記号の後になるよう、`~` は初日の側に付ける */
  protected readonly parts = computed(() =>
    this.start() === this.end() ? [this.start()] : [`${this.start()}~`, this.end()],
  );
}
