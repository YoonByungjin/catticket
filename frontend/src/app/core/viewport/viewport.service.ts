import { BreakpointObserver } from '@angular/cdk/layout';
import { Injectable, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

/**
 * スマホ表示の条件（02 の 1.、07 の 2.4）。幅 960px 未満がスマホ表示、960px 以上が PC 表示。
 * 向きで境目が変わる Breakpoints.Handset は使わない。CSS の src/theme/_breakpoints.scss と同じ文字列にする
 */
export const HANDSET_MEDIA_QUERY = '(max-width: 959.98px)';

/**
 * スマホ表示と PC 表示の判定（02 の 1.、07 の 2.4、ADR 0014 決定4）。
 * 端末の種類・画面の向きは見ず、画面の幅だけで決める。
 */
@Injectable({ providedIn: 'root' })
export class ViewportService {
  private readonly breakpoints = inject(BreakpointObserver);

  readonly handset = toSignal(
    this.breakpoints.observe(HANDSET_MEDIA_QUERY).pipe(map((state) => state.matches)),
    { initialValue: this.breakpoints.isMatched(HANDSET_MEDIA_QUERY) },
  );
}
