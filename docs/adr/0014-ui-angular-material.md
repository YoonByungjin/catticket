# 0014. UI コンポーネントライブラリは Angular Material（+ Angular CDK）を使う

- 日付: 2026-11-05
- 状態: 提案
- 関係する要件: F-01〜F-35 の画面全般, N-02, N-07（U-12）。Issue #14 の追加項目（2026-11-05 人間の決定）

## 背景

- 画面は Angular で作る（CLAUDE.md の確定事項）。F-01〜F-35 の画面には、地図・一覧・検索の入力・ダイアログ・フォーム（ログイン・レビュー・管理者の機能）などが必要になる。
- N-02: PC（最新の Chrome・Edge・Safari）とスマートフォン（最新の iOS Safari・Android Chrome）の両方で、管理者の機能を含む**すべての機能**が使える。
- 運用コストは月0円（U-12, N-07）。ライブラリにも有料のライセンスを使わない。
- 地図は `@angular/google-maps` を使う（0006、採用）。
- Issue #14 の追加項目で、比較にスマートフォン対応とアクセシビリティを含めることになった。**要件定義書（01）にはアクセシビリティの要件はない**（N-01〜N-08 に項目がない）。そのため、この ADR ではアクセシビリティを「選ぶときの比較の観点」として扱い、達成する基準（WCAG の等級など）は決めない。
- Angular の最新のメジャーバージョンは v22（公式のリリース一覧、確認日 2026-11-05）。

## 決定

1. UI コンポーネントは **Angular Material（`@angular/material`）と Angular CDK（`@angular/cdk`）** を使う。バージョンは Angular 本体と同じメジャーバージョンにそろえる（`@angular/google-maps` も同じリポジトリ・同じ方針で出るため、3つを一緒に更新する）。
2. テーマは Angular Material の **Material 3 のテーマ（`mat.theme` の Sass mixin）** で1か所にまとめる。色・文字・密度はこのテーマで決め、部品の CSS を直接上書きしない（公式が推奨しない方法のため）。具体的な色・文字の値は画面のデザイン（0015）と画面設計（02）で決める。
3. 密度（density）は 0（標準）のままにする。公式に「マイナスの値はアクセシビリティを下げることがある」とあり、スマートフォンでの押しやすさ（N-02）も下がるため。
4. スマートフォン対応（N-02）は、CSS のメディアクエリと CDK の `BreakpointObserver`（画面の幅による切り替え）で行う。**別の CSS フレームワーク（Tailwind CSS など）やレイアウト用のライブラリは入れない。** 必要になったときは新しい ADR を書く。
5. Angular Material にない部品が必要なときは、CDK の機能（オーバーレイ・フォーカス管理など）を使って自作する。ほかの UI ライブラリを混ぜない。
6. アクセシビリティは、Angular Material・CDK に組み込まれた機能（キーボード操作・ARIA 属性・フォーカス管理）をそのまま使い、フォーカスが見えにくいときは公式の `mat.strong-focus-indicators()` を使う。達成する基準は決めない（背景のとおり要件にないため）。

## 検討した選択肢

| 選択肢 | ライセンス・費用 | スマートフォン対応（N-02） | アクセシビリティ | 0006（`@angular/google-maps`）との相性 | 判断 |
|---|---|---|---|---|---|
| **Angular Material + CDK** | MIT。無料 | 公式のサポート対象に iOS Safari・Android の Chrome を含む。CDK の `BreakpointObserver` に Handset・Tablet などの区切りがある | 公式の目標に「アクセシブル」を掲げ、スクリーンリーダー（VoiceOver（iOS）など）を対象に挙げている | **同じリポジトリ（angular/components）のパッケージ。** Angular 本体と同じ方針で出るので、更新の時期がそろう | **採用を提案。** 公式で、費用がなく、N-02 の対象ブラウザを公式がサポートしている |
| PrimeNG | v22 から PrimeUI License。Community ライセンスは無料だが、**ライセンスキーが必要**で、**毎年の更新（条件を満たすことの確認）**が必要。v21 以前は MIT のままだが、GitHub のリポジトリは「セキュリティの修正だけ」と案内している | 部品は多い | 公式のリポジトリの README では確認できなかった | 別の提供元。更新の時期は別 | ライセンスキーと毎年の更新の手間があり、MIT 版は新しい開発がないため採用しない |
| Taiga UI | Apache 2.0。無料 | README では確認できなかった | README では確認できなかった | 別の提供元。v5 は Angular ^19 以降に対応 | 使えるが、Angular Material より優れている点を確認できなかった |
| spartan/ui | MIT。無料 | README では確認できなかった | 「アクセシブル」とあるが、基準・テストの記載はない | 別の提供元。Angular CDK の上に作られている | 見た目の部品はコードを自分のプロジェクトにコピーして持つ方式で、更新を自分で取り込む必要がある |
| Angular Aria（`@angular/aria`）+ 自作の見た目 | MIT（angular/components）。無料 | 見た目はすべて自作 | WAI-ARIA のパターンを実装したヘッドレスの部品（キーボード・ARIA・フォーカス・スクリーンリーダー） | 同じリポジトリ | 公式が「見た目付きの部品が欲しいなら Material」と案内している。見た目を全部作る手間が大きい |
| すべて自作 | 無料 | すべて自作 | キーボード操作・ARIA をすべて自分で作る | — | 手間が大きく、品質を確かめる方法もない |

## 結果（良い点・注意点）

### 費用（月0円の範囲か）

| 項目 | 内容 |
|---|---|
| 無料枠 | オープンソース（MIT）。料金はない |
| カード登録 | 不要 |
| 上限を超えたとき | 使用量の上限はない（ブラウザに配る JavaScript・CSS のライブラリのため） |
| 上限・アラートの設定（T-01） | 課金がないため不要 |

### 良い点

- Angular の公式のライブラリで、Angular・`@angular/google-maps` と同じ方針で更新される。バージョンの組み合わせで困りにくい。
- N-02 の対象ブラウザ（iOS Safari・Android の Chrome を含む）を公式がサポートしている。
- テーマの変数（`--mat-sys-*`）を自作の部品でも使えるので、自作の部分と見た目をそろえやすい。
- Angular チームが作る公式のライブラリなので、選んだ理由を面談で説明しやすい（G-04）。

### 注意点

- Material Design の見た目になる。0015 のデザインツールで作った画面が Material の部品で作れない形のときは、デザインを Material の部品に合わせて直す（実装に合わせてデザインを変える）。0015 の生成物の HTML・CSS は、そのまま `frontend/` に貼らない。
- Roboto などの英字のフォントは韓国語・日本語の文字を含まない。韓国語・日本語のフォントをどうするか（端末のフォントを使うか、Web フォントを読み込むか）は画面のデザイン（0015）と実装のときに決める。Web フォントを外部から読み込む場合は、費用と利用条件を確かめる（新しい外部サービスになる場合は ADR を書く）。
- アクセシビリティの基準は要件にない。基準（例: WCAG 2.2 の AA）を目標にするなら、要件定義書（01）に非機能要件を追加する必要がある。人間が決める。
- 地図（Google Maps）の中の操作のアクセシビリティは Google Maps の側に依存し、Angular Material では扱わない。

### 人間が決めること

- この ADR を採用するか（状態を「採用」にするか）。
- アクセシビリティの基準を要件に加えるか（加える場合は requirements-analyst が 01 を更新する）。

### 確認した公式ドキュメント（確認日: 2026-11-05）

- Angular のリリース・サポート期間（最新は v22）: https://angular.dev/reference/releases
- angular/components の README（`@angular/material`・`@angular/cdk`・`@angular/aria`・`@angular/google-maps` が同じリポジトリ、サポートするブラウザ・スクリーンリーダー、Angular と同じサポート方針）: https://github.com/angular/components
- angular/components のライセンス（MIT）: https://github.com/angular/components/blob/main/LICENSE
- Angular Material のテーマ（Material 3、`mat.theme`、密度とアクセシビリティ、`mat.strong-focus-indicators()`、CSS を直接上書きしない）: https://github.com/angular/components/blob/main/guides/theming.md （公開サイト https://material.angular.dev/guide/theming と同じ内容のソース）
- CDK の Layout（`BreakpointObserver`・`Breakpoints`）: https://github.com/angular/components/blob/main/src/cdk/layout/layout.md
- Angular Aria の概要: https://angular.dev/guide/aria/overview
- PrimeNG のリポジトリ（セキュリティの修正だけ、MIT 版は MIT のまま）: https://github.com/primefaces/primeng
- PrimeNG v22 のライセンス（PrimeUI License、Community ライセンスの条件・ライセンスキー・毎年の更新）: https://cdn.jsdelivr.net/npm/primeng@22.1.1/LICENSE.md （npm で配布されているファイル。全文は https://primeui.dev/licenses/community とされているが、そのページは開いて確認していない）
- Taiga UI の README（Apache 2.0、Angular の対応バージョン）: https://github.com/taiga-family/taiga-ui
- spartan/ui の README（MIT、Angular CDK の上に作る、見た目のコードをコピーする方式）: https://github.com/spartan-ng/spartan

---

## 한국어

### 0014. UI 컴포넌트 라이브러리는 Angular Material(+ Angular CDK)을 쓴다

- 날짜: 2026-11-05
- 상태: 제안
- 관련 요건: F-01~F-35의 화면 전반, N-02, N-07 (U-12). Issue #14의 추가 항목(2026-11-05 사람의 결정)

### 배경

- 화면은 Angular로 만든다(CLAUDE.md의 확정 사항). F-01~F-35의 화면에는 지도·목록·검색 입력·대화상자·폼(로그인·리뷰·관리자 기능) 등이 필요하다.
- N-02: PC(최신 Chrome·Edge·Safari)와 스마트폰(최신 iOS Safari·Android Chrome) 둘 다에서 관리자 기능을 포함한 **모든 기능**을 쓸 수 있다.
- 운영 비용은 월 0원(U-12, N-07). 라이브러리에도 유료 라이선스를 쓰지 않는다.
- 지도는 `@angular/google-maps`를 쓴다(0006, 채택).
- Issue #14의 추가 항목에서 비교에 스마트폰 대응과 접근성을 넣기로 했다. **요건정의서(01)에는 접근성 요건이 없다**(N-01~N-08에 항목이 없음). 그래서 이 ADR에서는 접근성을 "고를 때의 비교 관점"으로 다루고, 달성할 기준(WCAG 등급 등)은 정하지 않는다.
- Angular의 최신 메이저 버전은 v22(공식 릴리스 목록, 확인일 2026-11-05).

### 결정

1. UI 컴포넌트는 **Angular Material(`@angular/material`)과 Angular CDK(`@angular/cdk`)**를 쓴다. 버전은 Angular 본체와 같은 메이저 버전으로 맞춘다(`@angular/google-maps`도 같은 저장소·같은 방침으로 나오므로 셋을 함께 갱신한다).
2. 테마는 Angular Material의 **Material 3 테마(`mat.theme` Sass mixin)**로 한 곳에 모은다. 색·글꼴·밀도는 이 테마로 정하고 부품의 CSS를 직접 덮어쓰지 않는다(공식이 권장하지 않는 방법이기 때문). 구체적인 색·글꼴 값은 화면 디자인(0015)과 화면 설계(02)에서 정한다.
3. 밀도(density)는 0(표준)으로 둔다. 공식에 "음수 값은 접근성을 떨어뜨릴 수 있다"고 되어 있고, 스마트폰에서 누르기 쉬운 정도(N-02)도 떨어지기 때문이다.
4. 스마트폰 대응(N-02)은 CSS 미디어 쿼리와 CDK의 `BreakpointObserver`(화면 폭에 따른 전환)로 한다. **다른 CSS 프레임워크(Tailwind CSS 등)나 레이아웃용 라이브러리는 넣지 않는다.** 필요해지면 새 ADR을 쓴다.
5. Angular Material에 없는 부품이 필요하면 CDK 기능(오버레이·포커스 관리 등)을 써서 직접 만든다. 다른 UI 라이브러리를 섞지 않는다.
6. 접근성은 Angular Material·CDK에 들어 있는 기능(키보드 조작·ARIA 속성·포커스 관리)을 그대로 쓰고, 포커스가 잘 안 보일 때는 공식 `mat.strong-focus-indicators()`를 쓴다. 달성할 기준은 정하지 않는다(배경대로 요건에 없기 때문).

### 검토한 선택지

| 선택지 | 라이선스·비용 | 스마트폰 대응(N-02) | 접근성 | 0006(`@angular/google-maps`)과의 궁합 | 판단 |
|---|---|---|---|---|---|
| **Angular Material + CDK** | MIT. 무료 | 공식 지원 대상에 iOS Safari·Android Chrome 포함. CDK `BreakpointObserver`에 Handset·Tablet 등의 구분이 있음 | 공식 목표에 "접근 가능"을 내걸고, 스크린 리더(VoiceOver(iOS) 등)를 대상으로 든다 | **같은 저장소(angular/components)의 패키지.** Angular 본체와 같은 방침으로 나와 갱신 시기가 맞는다 | **채택 제안.** 공식이고 비용이 없으며 N-02 대상 브라우저를 공식이 지원한다 |
| PrimeNG | v22부터 PrimeUI License. Community 라이선스는 무료지만 **라이선스 키가 필요**하고 **매년 갱신(조건 충족 확인)**이 필요. v21 이하는 MIT 그대로지만 GitHub 저장소는 "보안 수정만"이라고 안내 | 부품이 많다 | 공식 저장소 README로는 확인하지 못함 | 다른 제공처. 갱신 시기가 다름 | 라이선스 키와 매년 갱신의 수고가 있고, MIT 판은 새 개발이 없어서 채택하지 않음 |
| Taiga UI | Apache 2.0. 무료 | README로는 확인하지 못함 | README로는 확인하지 못함 | 다른 제공처. v5는 Angular ^19 이상 지원 | 쓸 수는 있지만 Angular Material보다 나은 점을 확인하지 못함 |
| spartan/ui | MIT. 무료 | README로는 확인하지 못함 | "접근 가능"이라고 되어 있지만 기준·테스트 기재는 없음 | 다른 제공처. Angular CDK 위에 만들어짐 | 겉모습 부품은 코드를 자기 프로젝트에 복사해 갖는 방식이라, 갱신을 직접 반영해야 함 |
| Angular Aria(`@angular/aria`) + 겉모습 직접 구현 | MIT(angular/components). 무료 | 겉모습은 전부 직접 구현 | WAI-ARIA 패턴을 구현한 헤드리스 부품(키보드·ARIA·포커스·스크린 리더) | 같은 저장소 | 공식이 "겉모습이 있는 부품을 원하면 Material"이라고 안내. 겉모습을 전부 만드는 수고가 큼 |
| 전부 직접 구현 | 무료 | 전부 직접 구현 | 키보드 조작·ARIA를 전부 직접 만든다 | — | 수고가 크고 품질을 확인할 방법도 없음 |

### 결과 (좋은 점·주의점)

#### 비용 (월 0원 범위인가)

| 항목 | 내용 |
|---|---|
| 무료 한도 | 오픈 소스(MIT). 요금 없음 |
| 카드 등록 | 불필요 |
| 한도를 넘었을 때 | 사용량 상한이 없다(브라우저에 배포하는 JavaScript·CSS 라이브러리이기 때문) |
| 상한·알림 설정(T-01) | 과금이 없으므로 불필요 |

#### 좋은 점

- Angular 공식 라이브러리로, Angular·`@angular/google-maps`와 같은 방침으로 갱신된다. 버전 조합으로 곤란해지기 어렵다.
- N-02 대상 브라우저(iOS Safari·Android Chrome 포함)를 공식이 지원한다.
- 테마 변수(`--mat-sys-*`)를 직접 만든 부품에서도 쓸 수 있어 직접 만든 부분과 겉모습을 맞추기 쉽다.
- Angular 팀이 만드는 공식 라이브러리라서 고른 이유를 면접에서 설명하기 쉽다(G-04).

#### 주의점

- Material Design의 겉모습이 된다. 0015의 디자인 도구로 만든 화면이 Material 부품으로 만들 수 없는 형태라면, 디자인을 Material 부품에 맞게 고친다(구현에 맞춰 디자인을 바꾼다). 0015 생성물의 HTML·CSS는 그대로 `frontend/`에 붙이지 않는다.
- Roboto 등 영문 글꼴은 한국어·일본어 글자를 포함하지 않는다. 한국어·일본어 글꼴을 어떻게 할지(기기 글꼴을 쓸지, 웹 폰트를 불러올지)는 화면 디자인(0015)과 구현 때 정한다. 웹 폰트를 외부에서 불러오는 경우 비용과 이용 조건을 확인한다(새 외부 서비스가 되면 ADR을 쓴다).
- 접근성 기준은 요건에 없다. 기준(예: WCAG 2.2 AA)을 목표로 한다면 요건정의서(01)에 비기능 요건을 추가해야 한다. 사람이 정한다.
- 지도(Google Maps) 안의 조작의 접근성은 Google Maps 쪽에 달려 있고 Angular Material에서는 다루지 않는다.

#### 사람이 정할 것

- 이 ADR을 채택할지(상태를 "채택"으로 할지).
- 접근성 기준을 요건에 넣을지(넣는다면 requirements-analyst가 01을 갱신한다).

#### 확인한 공식 문서 (확인일: 2026-11-05)

- Angular 릴리스·지원 기간(최신은 v22): https://angular.dev/reference/releases
- angular/components README(`@angular/material`·`@angular/cdk`·`@angular/aria`·`@angular/google-maps`가 같은 저장소, 지원 브라우저·스크린 리더, Angular와 같은 지원 방침): https://github.com/angular/components
- angular/components 라이선스(MIT): https://github.com/angular/components/blob/main/LICENSE
- Angular Material 테마(Material 3, `mat.theme`, 밀도와 접근성, `mat.strong-focus-indicators()`, CSS를 직접 덮어쓰지 않음): https://github.com/angular/components/blob/main/guides/theming.md (공개 사이트 https://material.angular.dev/guide/theming 과 같은 내용의 소스)
- CDK Layout(`BreakpointObserver`·`Breakpoints`): https://github.com/angular/components/blob/main/src/cdk/layout/layout.md
- Angular Aria 개요: https://angular.dev/guide/aria/overview
- PrimeNG 저장소(보안 수정만, MIT 판은 MIT 그대로): https://github.com/primefaces/primeng
- PrimeNG v22 라이선스(PrimeUI License, Community 라이선스 조건·라이선스 키·매년 갱신): https://cdn.jsdelivr.net/npm/primeng@22.1.1/LICENSE.md (npm으로 배포되는 파일. 전문은 https://primeui.dev/licenses/community 라고 되어 있지만 그 페이지는 열어서 확인하지 않았다)
- Taiga UI README(Apache 2.0, Angular 지원 버전): https://github.com/taiga-family/taiga-ui
- spartan/ui README(MIT, Angular CDK 위에 만듦, 겉모습 코드를 복사하는 방식): https://github.com/spartan-ng/spartan
