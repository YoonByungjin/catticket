import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { RouterLink } from '@angular/router';
import { PerformanceSummary } from '../../core/api/api.types';
import { isGenreCode } from '../../core/code/genre';
import { LanguageService } from '../../core/i18n/language.service';
import { MessageKey } from '../../core/i18n/messages';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { DateRange } from '../date-range/date-range';
import { localizedDisplay } from '../localized-text/localized-text';

/**
 * 公演のカード（02 の 3.7、07 の 4.4）。SC-01・SC-03・SC-11 で共通に使う。
 * カード全体が SC-02 へのリンク。評価（F-30）は SC-03 では出さないため、まだ持たない
 * （評価を出す画面の Issue で足す）。
 */
@Component({
  selector: 'app-performance-card',
  imports: [MatCardModule, RouterLink, TranslatePipe, DateRange],
  templateUrl: './performance-card.html',
  styleUrl: './performance-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PerformanceCard {
  private readonly language = inject(LanguageService);

  readonly performance = input.required<PerformanceSummary>();

  protected readonly title = computed(() =>
    localizedDisplay(this.performance().title, this.language.lang(), true),
  );
  protected readonly venue = computed(() =>
    localizedDisplay(this.performance().venue.name, this.language.lang(), true),
  );

  /** 辞書にないコード（05 の 7 の確認で増えたときなど）は、コードをそのまま出す */
  protected readonly genreLabel = computed(() => {
    const code = this.performance().genre;
    return isGenreCode(code) ? this.language.t(`genre.${code}`) : code;
  });

  protected readonly statusKey = computed<MessageKey>(
    () => `performance.status.${this.performance().status}`,
  );
}
