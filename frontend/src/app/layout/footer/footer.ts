import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * 出典の文言。表示言語にかかわらず韓国語の原文のまま出す（02 の 3.2）ため、辞書に入れない。
 * 1行目は ADR 0008、2行目は ADR 0009 5.・02 の 3.2 のデータの範囲。
 */
export const ATTRIBUTIONS: readonly string[] = [
  '출처: (재)예술경영지원센터 공연예술통합전산망(www.kopis.or.kr)',
  '출처: 서울교통공사 공개 데이터(역 좌표·주소, 물품보관함 위치, 역명 다국어 표기(공공누리 제1유형))',
];

/** CM-02 フッター（02 の 3.2、07 の 3.・4.2） */
@Component({
  selector: 'app-footer',
  template: `
    <footer class="ct-footer" lang="ko">
      @for (line of attributions; track line) {
        <p>{{ line }}</p>
      }
    </footer>
  `,
  styles: `
    .ct-footer {
      padding: 16px 24px;
      background: var(--mat-sys-surface-container-low);
      color: var(--mat-sys-on-surface-variant);
      font: var(--mat-sys-body-small);
    }
    p {
      margin: 0;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Footer {
  protected readonly attributions = ATTRIBUTIONS;
}
