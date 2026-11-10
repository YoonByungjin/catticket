import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Injectable, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

/**
 * スマホ表示と PC 表示の判定（02 の 1.、07 の 2.4、ADR 0014 決定4）。
 * CDK の Breakpoints.Handset に当たる幅を「スマホ表示」とする。
 */
@Injectable({ providedIn: 'root' })
export class ViewportService {
  private readonly breakpoints = inject(BreakpointObserver);

  readonly handset = toSignal(
    this.breakpoints.observe(Breakpoints.Handset).pipe(map((state) => state.matches)),
    { initialValue: this.breakpoints.isMatched(Breakpoints.Handset) },
  );
}
