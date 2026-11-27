import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatChipSelectionChange, MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { ActivatedRoute, Router } from '@angular/router';
import { ALL_GENRES, GENRE_CODES, isGenreCode } from '../../core/code/genre';
import { LanguageService } from '../../core/i18n/language.service';
import { MessageKey } from '../../core/i18n/messages';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ViewportService } from '../../core/viewport/viewport.service';
import { DateRange } from '../../shared/date-range/date-range';
import { PerformanceCard } from '../../shared/performance-card/performance-card';
import { RankingService } from './ranking.service';
import {
  RANKING_PERIODS,
  RankingGenre,
  RankingPeriod,
  RankingResponse,
  isRankingPeriod,
} from './ranking.types';

/** URL の tab の値。人気急上昇・ジャンル別トレンドのタブは、その機能を実装するまで出さない（02 の SC-03） */
export const RANKING_TAB = 'ranking';

/** URL に期間がないときの期間（試作 SC-03 の選択の状態に合わせる） */
export const DEFAULT_RANKING_PERIOD: RankingPeriod = 'weekly';

/** 一覧の状態（02 の 3.6・SC-03） */
export type RankingViewState = 'loading' | 'error' | 'empty' | 'ready';

/**
 * SC-03 人気・トレンド（02 の SC-03、07 の 3.・4.6）。いまは「人気順位」のタブだけ（F-29）。
 * タブ・期間・ジャンルは URL のクエリ（tab・period・genre）に持ち、ルートの入力として受け取る
 * （withComponentInputBinding）。知らない値は既定の値として扱う。
 */
@Component({
  selector: 'app-rankings-page',
  imports: [
    MatTabsModule,
    MatButtonToggleModule,
    MatChipsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    TranslatePipe,
    DateRange,
    PerformanceCard,
  ],
  templateUrl: './rankings-page.html',
  styleUrl: './rankings-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RankingsPage {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly language = inject(LanguageService);
  private readonly rankingService = inject(RankingService);

  protected readonly handset = inject(ViewportService).handset;

  /** URL のクエリ period・genre の生の値（ルートの入力。知らない値は selectedPeriod・selectedGenre で既定にする） */
  readonly period = input<string>();
  readonly genre = input<string>();

  protected readonly selectedPeriod = computed<RankingPeriod>(() => {
    const value = this.period();
    return isRankingPeriod(value) ? value : DEFAULT_RANKING_PERIOD;
  });
  protected readonly selectedGenre = computed<RankingGenre>(() => {
    const value = this.genre();
    return isGenreCode(value) ? value : ALL_GENRES;
  });

  protected readonly periods = RANKING_PERIODS;
  protected readonly genres: readonly RankingGenre[] = [ALL_GENRES, ...GENRE_CODES];

  /** API-20。期間・ジャンル・言語が変わるたびに取り直す（集計期間もそれに合わせて変わる。02 の SC-03） */
  protected readonly ranking = rxResource({
    params: () => ({
      period: this.selectedPeriod(),
      genre: this.selectedGenre(),
      lang: this.language.lang(),
    }),
    stream: ({ params }) => this.rankingService.getRanking(params),
  });

  private readonly response = computed<RankingResponse | undefined>(() =>
    this.ranking.hasValue() ? this.ranking.value() : undefined,
  );

  protected readonly state = computed<RankingViewState>(() => {
    if (this.ranking.isLoading()) {
      return 'loading';
    }
    if (this.ranking.status() === 'error') {
      return 'error';
    }
    return (this.response()?.items.length ?? 0) > 0 ? 'ready' : 'empty';
  });

  protected readonly items = computed(() => this.response()?.items ?? []);

  /** 表示している順位の集計期間（F-29 ④）。表示できる期間がない・読み込み中・エラーのときは null */
  protected readonly aggregationPeriod = computed(() => {
    const state = this.state();
    return state === 'ready' || state === 'empty' ? (this.response()?.period ?? null) : null;
  });

  protected periodKey(period: RankingPeriod): MessageKey {
    return `rankings.period.${period}`;
  }

  protected genreLabel(genre: RankingGenre): string {
    return this.language.t(`genre.${genre}`);
  }

  protected selectPeriod(period: RankingPeriod): void {
    this.updateQuery(period, this.selectedGenre());
  }

  protected selectGenre(genre: RankingGenre): void {
    this.updateQuery(this.selectedPeriod(), genre);
  }

  /** PC 表示のチップ。利用者が選んだときだけ反応する（ほかのチップの選択が外れた通知は無視する） */
  protected onChipSelection(event: MatChipSelectionChange): void {
    if (event.selected && event.isUserInput) {
      this.selectGenre(event.source.value as RankingGenre);
    }
  }

  protected reload(): void {
    this.ranking.reload();
  }

  private updateQuery(period: RankingPeriod, genre: RankingGenre): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab: RANKING_TAB, period, genre },
    });
  }
}
