import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { LanguageService } from '../../core/i18n/language.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';

/** 出典の1行の区切り。href があるところだけリンクにする（02 の 3.2） */
export interface AttributionSegment {
  readonly text: string;
  readonly href?: string;
}

export type AttributionLine = readonly AttributionSegment[];

/**
 * 出典の文言（02 の 3.2 の「フッターの2行の原文」）。表示言語にかかわらず韓国語の原文のまま出すため、辞書に入れない。
 * リンクはサイト名の2つだけ。括弧のドメインと1行目はリンクにしない（02 の 3.2）。
 * CM-02 フッターと SC-01 のスマホ表示の出典のダイアログで同じものを使う（07 の 4.2.1）。
 */
export const ATTRIBUTION_LINES: readonly AttributionLine[] = [
  [{ text: '출처: (재)예술경영지원센터 공연예술통합전산망(www.kopis.or.kr)' }],
  [
    {
      text: '출처: 서울교통공사 공공데이터(역 좌표·주소, 물품보관함 위치, 역명 다국어 표기) - ',
    },
    { text: '공공데이터포털', href: 'https://www.data.go.kr' },
    { text: '(www.data.go.kr), ' },
    {
      text: '서울 열린데이터 광장',
      href: 'https://data.seoul.go.kr/dataList/OA-2751/F/1/datasetView.do',
    },
    { text: '(data.seoul.go.kr)' },
  ],
];

/** 1行の表示の文字列（区切りをつないだもの） */
export function attributionText(line: AttributionLine): string {
  return line.map((segment) => segment.text).join('');
}

/**
 * 出典の2行（07 の 4.2・4.2.1）。外側の枠（背景・余白）は使う側（フッター、SC-01 のダイアログ）が持つ。
 * リンクの中には新しいタブで開くことを支援技術だけに知らせる文言を入れる（02 の 3.2、Issue #40 の U-4）。
 * リンクの名前を上書きしないよう aria-label は使わない。この文言は画面の文言なので辞書から出し、原文には含めない。
 */
@Component({
  selector: 'app-attribution',
  imports: [TranslatePipe],
  // 区切りの間に空白を足さない（空白だけの文字は Angular が消す。原文との完全一致はテストで確かめる）。
  // 見えない文言の <span> は、外側の lang="ko" の中で表示言語の文言を正しく読ませるため表示言語の lang を付ける
  template: `
    @for (line of lines; track $index) {
      <p class="ct-attribution__line">
        @for (segment of line; track $index) {
          @if (segment.href) {
            <a
              class="ct-source-link"
              [href]="segment.href"
              target="_blank"
              rel="noopener noreferrer"
              >{{ segment.text
              }}<span class="cdk-visually-hidden" [attr.lang]="language.lang()">{{
                'attribution.newTab' | translate
              }}</span></a
            >
          } @else {
            <span>{{ segment.text }}</span>
          }
        }
      </p>
    }
  `,
  styles: `
    :host {
      display: block;
      font: var(--mat-sys-body-small);
    }

    .ct-attribution__line {
      margin: 0;
    }

    // 07 の 4.2.1。普通の <a> と自作のクラス（Angular Material の部品のクラスには書かない）
    .ct-source-link {
      color: var(--mat-sys-primary);
      text-decoration: underline;
      text-decoration-thickness: 1px;
      // ハングルの字に線が重ならないように少し離す
      text-underline-offset: 2px;

      // コントラストを確かめた色だけを使うため、訪問済みでも変えない
      &:visited {
        color: var(--mat-sys-primary);
      }

      // 色・背景は変えず、下線だけを太くする
      &:hover {
        text-decoration-thickness: 2px;
      }

      // mat.strong-focus-indicators() と同じ形。変数がないときは 3px・solid・primary・4px
      &:focus-visible {
        outline-width: var(--mat-focus-indicator-border-width, 3px);
        outline-style: var(--mat-focus-indicator-border-style, solid);
        outline-color: var(--mat-focus-indicator-border-color, var(--mat-sys-primary));
        outline-offset: 2px;
        border-radius: var(--mat-focus-indicator-border-radius, 4px);
      }
    }
  `,
  host: { lang: 'ko' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Attribution {
  protected readonly lines = ATTRIBUTION_LINES;
  protected readonly language = inject(LanguageService);
}
