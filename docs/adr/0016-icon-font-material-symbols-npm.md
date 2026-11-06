# 0016. アイコンのフォント（Material Symbols）は npm のパッケージ `@material-symbols/font-400` でアプリに同梱する

- 日付: 2026-11-06
- 状態: 採用（2026-11-06、人間が決定。Issue #15 コメント 6676943465）
- 関係する要件: F-01〜F-36 の画面全般（07 の 2.7 のアイコン）, N-02, N-07（U-12）。07 の未決事項 U-1。Issue #15 コメント 6676583842 の 5.（人間の決定: Material Symbols を Google Fonts から読み込まず、npm のパッケージで同梱する。ADR は architect が書き、人間が承認する）

## 背景

- 07（UI デザインガイド）の 2.7 は、アイコンを Angular Material の `mat-icon` と **Material Symbols（Outlined）** で表すと決めている。使うアイコンは意味の決まった約10個（`menu`・`star`・`open_in_new`・`my_location`・`tune`・`close`・`train`・`lock`・`favorite` など）と、F-36 のライト表示・ダーク表示の切り替え（02 の 3.1。アイコンは 07 で決める）。
- 07 の 2.2.1 は「Web フォントを外部から読み込まない」としている（外部の読み込みは新しい外部サービスになり、費用・利用条件の確認と ADR が要るため。ADR 0014 注意点）。
- アイコンのフォントの配り方は ADR 0014 に書かれておらず、07 の未決事項 U-1 になっていた。人間は「npm のパッケージでアプリに同梱する」と決めた（Issue #15 コメント 6676583842 の 5.）。この ADR は、**どのパッケージを使うか**と**ライセンス**を決める。
- Angular Material の公式の説明では、`mat-icon` は合字（文字列 `home` を家の絵で表示する）のフォントでアイコンを表示し、**フォントとその CSS は利用者が読み込む**。既定のフォントのクラスは、読み込まれたフォントから推測するか、`MatIconRegistry.setDefaultFontSetClass` で指定する（公式の説明とソース。確認日 2026-11-06）。

## 決定

1. アイコンのフォントは npm のパッケージ **`@material-symbols/font-400`** を使い、**Outlined の CSS（`@material-symbols/font-400/outlined.css`）だけ**を全体のスタイルに加える（Rounded・Sharp は読み込まない）。
2. フォントのファイル（woff2）は Angular のビルドで配布物に含まれ、本サービスのサーバー（ADR 0010 の Nginx）から配る。**Google Fonts（`fonts.googleapis.com` など）には接続しない。**
3. アプリの起動のときに `MatIconRegistry.setDefaultFontSetClass('material-symbols-outlined', 'mat-ligature-font')` を呼び、`<mat-icon>名前</mat-icon>` だけで Outlined を使う。読み込まれたフォントからの推測には頼らない（読み込みの時点によって結果が変わりうるため）。
4. バージョンは**完全に固定**する（`package.json` で `^`・`~` を付けない。0.x の版で、提供元が毎週のように自動で更新しているため）。上げるときは PR で行い、使っているアイコンの表示を確かめる。
5. ライセンスは **Apache License 2.0**（アイコンは Google が作成。Google の公式のリポジトリとパッケージの記載）。配布物にライセンスの文を含める: 本番のビルドが出すサードパーティのライセンスの一覧（Angular の `extractLicenses`）にこのパッケージが含まれるかを実装で確かめ、含まれなければ配布物にこのパッケージの `LICENSE` を置く（Apache 2.0 第4条）。画面への表記（クレジット）は必須ではない（Google の README:「アプリの about 画面に表記してもらえるとうれしいが、必須ではない」）。
6. 塗りつぶしの有無（評価の星・お気に入り）は、Material Symbols の **FILL 軸**で切り替える（このパッケージは FILL 軸を変えられる）。指定の方法（どの要素にどの CSS を付けるか）は 07 と実装で、ADR 0014 決定2（部品の CSS を直接上書きしない）に反しない形で決める。
7. 担当: 依存関係の追加と設定は frontend-dev が工程5で行う（この ADR の承認の後）。07 の 2.7・U-1 の書き換えは ui-designer が行う。

## 検討した選択肢

大きさは、Outlined のフォントのファイル（woff2）の大きさ（unpkg のファイル一覧、確認日 2026-11-06）。

| 選択肢 | 提供元・保守 | ライセンス | 大きさ（Outlined） | `mat-icon` での使い方 | 判断 |
|---|---|---|---|---|---|
| **`@material-symbols/font-400`**（0.48.3） | marella（Google の公式リポジトリの README が Material Symbols の npm パッケージとして案内している提供元。ただし「Google は監視・検証していない」と明記）。Google の最新のアイコンに自動で更新され、最新は 2026-11-04 公開、10月にも 0.48.0〜0.48.2 を公開 | Apache-2.0 | **約 494 KB**（太さ 400・grade 0・optical size 48 に固定。FILL 軸は可変） | 合字。`fontSet="material-symbols-outlined"` または既定のクラスに指定 | **採用を提案。** 07 は太さを変えないので、固定の太さで十分。全部の軸を持つ版の約8分の1の大きさ |
| `material-symbols`（0.48.3） | 同じ提供元（marella）。GitHub Actions から公開され、出どころの証明（provenance）が付いている | Apache-2.0 | 約 4.0 MB（すべての軸が可変） | 同じ | 太さ・grade・optical size を変えない（07）ので、不要に大きい |
| `@fontsource/material-symbols-outlined` / `@fontsource-variable/material-symbols-outlined`（5.3.9） | Fontsource | パッケージの記載は **OFL-1.1**。Google の公式リポジトリの記載（Apache 2.0）と食い違い、どちらが正しいかを公式の情報で確かめられない | パッケージ全体で 5.3 MB / 10.4 MB（展開後） | `fontSet` に Fontsource の CSS のクラスを指定 | ライセンスの記載が公式と食い違うため採用しない |
| `@material-symbols/svg-400`（0.48.3） | marella | Apache-2.0 | 使うアイコンだけ（1つ数百バイト〜数 KB） | SVG。アイコンごとに `MatIconRegistry` へ登録（`addSvgIcon` などと HTTP の読み込み、または文字列の登録）が必要 | 通信量は最も小さいが、アイコンを足すたびに登録が要り、07 の「名前で指定する」使い方と違う。フォントが大きすぎると分かったときの代わりの案とする（変えるときは新しい ADR） |
| Google の公式リポジトリ（google/material-design-icons の `variablefont`）からファイルを手で複製 | Google（公式）。Google の README は自分で置くより Google Fonts を勧めている | Apache 2.0 | すべての軸が可変（数 MB） | 合字。CSS（`@font-face` とクラス）は自作 | 更新を手で行う必要がある。人間の決定（npm で同梱）に合わない |
| Google Fonts から読み込む | Google | Apache 2.0 | 使うアイコンだけに絞れる（`icon_names`） | 合字 | 07 の 2.2.1 と人間の決定（外部から読み込まない）により採用しない |

## 結果（良い点・注意点）

### 費用（月0円の範囲か）

| 項目 | 内容 |
|---|---|
| 無料枠 | オープンソース（Apache-2.0）。料金はない |
| カード登録 | 不要 |
| 上限を超えたとき | 使用量の上限はない（自分のサーバーから配るファイルのため） |
| 上限・アラートの設定（T-01） | 課金がないため不要 |

### 良い点

- 画面を開くときに Google のサーバーへ接続しない。07 の 2.2.1 と一致し、利用者の情報（IP アドレスなど）を新しい外部サービスに送らない。
- `mat-icon` の公式の使い方（合字・`fontSet`・`setDefaultFontSetClass`）のまま使える。
- 太さを固定した版で、全部の軸を持つ版より大きさが約8分の1になる。

### 注意点

- **大きさ**: Outlined のフォントは約 494 KB で、最初に開いたときに一度だけ読み込まれる（その後はブラウザのキャッシュ）。Google Fonts の `icon_names` で必要なアイコンだけに絞る方法より大きい。実装で問題になる場合は、SVG（`@material-symbols/svg-400`）か、使うアイコンだけにフォントを小さくする方法を新しい ADR で検討する。
- **提供元**: Google の公式のパッケージではない（Google は監視・検証していないと明記）。そのため、バージョンを固定し、更新は PR でレビューして行う。`package-lock.json` をコミットする。
- **optical size が 48 に固定**: 07 のアイコンの表示の大きさ（`mat-icon` の既定は 24px）では、optical size 24 の字形と線の太さの見え方が少し違う。実装で見た目を確かめ、気になる場合は ui-designer が 07 で扱う（全部の軸を持つ `material-symbols` に変える場合は新しい ADR）。
- **アイコンの名前**: このパッケージの名前の一覧（`index.d.ts`）で、07 の 2.7 にある `favorite_border` を確認できなかった（確認日 2026-11-06）。Material Symbols は塗りつぶしを FILL 軸で切り替えるので、`favorite` と FILL 軸（決定6）で表す。名前の確認と 07 の書き換えは ui-designer が行う。F-36 の切り替えに使える `dark_mode`・`light_mode` は一覧にあった。
- 0.x の版なので、版の番号から互換性を判断できない（決定4）。
- 07 の 2.7 の「未決（8. 未決事項）」と U-1 は、この ADR の採用の後に ui-designer が更新する。

### 人間が決めること

- この ADR を採用するか（状態を「採用」にするか）。

### 確認した公式ドキュメント（確認日: 2026-11-06）

- Google の公式リポジトリの README（ライセンスは Apache 2.0、表記は必須ではない、npm のパッケージは marella が提供し Google は監視・検証していない、`variablefont` のファイル、Google Fonts を勧めている）: https://github.com/google/material-design-icons （README: https://raw.githubusercontent.com/google/material-design-icons/master/README.md ）
- `@material-symbols/font-400` の情報（版 0.48.3、Apache-2.0、リポジトリ）: https://registry.npmjs.org/@material-symbols/font-400/latest 、公開日の一覧: https://registry.npmjs.org/@material-symbols/font-400
- `@material-symbols/font-400` の README（太さ 400・grade 0・optical size 48、FILL 軸、`outlined.css`、`mat-icon` の `fontSet`、ライセンス）: https://unpkg.com/@material-symbols/font-400@0.48.3/README.md 、ファイルの大きさ: https://unpkg.com/@material-symbols/font-400@0.48.3/?meta 、アイコンの名前の一覧: https://unpkg.com/@material-symbols/font-400@0.48.3/index.d.ts
- `material-symbols` の情報（Apache-2.0、provenance）: https://registry.npmjs.org/material-symbols/latest 、ファイルの大きさ: https://unpkg.com/material-symbols@0.48.3/?meta 、README: https://github.com/marella/material-symbols/tree/main/material-symbols
- `@material-symbols/svg-400` の情報: https://registry.npmjs.org/@material-symbols/svg-400/latest
- Fontsource のパッケージの情報（ライセンスの記載 OFL-1.1）: https://registry.npmjs.org/@fontsource/material-symbols-outlined/latest 、https://registry.npmjs.org/@fontsource-variable/material-symbols-outlined/latest
- Angular Material の `mat-icon`（合字のフォント、フォントと CSS は利用者が読み込む、`fontSet`、`setDefaultFontSetClass`）: https://github.com/angular/components/blob/main/src/material/icon/icon.md
- 既定のフォントのクラスの推測（`inferDefaultFontSetClass`）: https://github.com/angular/components/blob/main/src/material/icon/icon-registry.ts

---

## 한국어

### 0016. 아이콘 글꼴(Material Symbols)은 npm 패키지 `@material-symbols/font-400`으로 앱에 함께 넣는다

- 날짜: 2026-11-06
- 상태: 채택(2026-11-06, 사람이 결정. Issue #15 코멘트 6676943465)
- 관련 요건: F-01~F-36의 화면 전반(07의 2.7 아이콘), N-02, N-07 (U-12). 07의 미결 사항 U-1. Issue #15 코멘트 6676583842의 5.(사람의 결정: Material Symbols를 Google Fonts에서 불러오지 않고 npm 패키지로 함께 넣는다. ADR은 architect가 쓰고 사람이 승인한다)

### 배경

- 07(UI 디자인 가이드)의 2.7은 아이콘을 Angular Material의 `mat-icon`과 **Material Symbols(Outlined)**로 나타낸다고 정했다. 쓰는 아이콘은 의미가 정해진 약 10개(`menu`·`star`·`open_in_new`·`my_location`·`tune`·`close`·`train`·`lock`·`favorite` 등)와 F-36의 라이트 표시·다크 표시 전환(02의 3.1. 아이콘은 07에서 정한다).
- 07의 2.2.1은 "웹 폰트를 외부에서 불러오지 않는다"고 한다(외부 로드는 새 외부 서비스가 되어 비용·이용 조건 확인과 ADR이 필요하기 때문. ADR 0014 주의점).
- 아이콘 글꼴의 배포 방법은 ADR 0014에 쓰여 있지 않아 07의 미결 사항 U-1이 되어 있었다. 사람은 "npm 패키지로 앱에 함께 넣는다"고 정했다(Issue #15 코멘트 6676583842의 5.). 이 ADR은 **어느 패키지를 쓸지**와 **라이선스**를 정한다.
- Angular Material 공식 설명에서 `mat-icon`은 합자(문자열 `home`을 집 그림으로 표시) 글꼴로 아이콘을 표시하며, **글꼴과 그 CSS는 이용자가 불러온다**. 기본 글꼴 클래스는 불러온 글꼴에서 추측하거나 `MatIconRegistry.setDefaultFontSetClass`로 지정한다(공식 설명과 소스. 확인일 2026-11-06).

### 결정

1. 아이콘 글꼴은 npm 패키지 **`@material-symbols/font-400`**을 쓰고, **Outlined CSS(`@material-symbols/font-400/outlined.css`)만** 전체 스타일에 더한다(Rounded·Sharp는 불러오지 않는다).
2. 글꼴 파일(woff2)은 Angular 빌드로 배포물에 포함되고, 이 서비스의 서버(ADR 0010의 Nginx)에서 배포한다. **Google Fonts(`fonts.googleapis.com` 등)에는 접속하지 않는다.**
3. 앱 기동 때 `MatIconRegistry.setDefaultFontSetClass('material-symbols-outlined', 'mat-ligature-font')`를 불러 `<mat-icon>이름</mat-icon>`만으로 Outlined를 쓴다. 불러온 글꼴에서의 추측에는 기대지 않는다(불러오는 시점에 따라 결과가 바뀔 수 있기 때문).
4. 버전은 **완전히 고정**한다(`package.json`에서 `^`·`~`를 붙이지 않는다. 0.x 판이고 제공처가 거의 매주 자동으로 갱신하기 때문). 올릴 때는 PR로 하고, 쓰는 아이콘의 표시를 확인한다.
5. 라이선스는 **Apache License 2.0**(아이콘은 Google이 만듦. Google 공식 저장소와 패키지의 기재). 배포물에 라이선스 문서를 포함한다: 운영 빌드가 내는 서드파티 라이선스 목록(Angular의 `extractLicenses`)에 이 패키지가 들어가는지 구현에서 확인하고, 들어가지 않으면 배포물에 이 패키지의 `LICENSE`를 둔다(Apache 2.0 제4조). 화면에 표기(크레디트)는 필수가 아니다(Google README: "앱의 about 화면에 표기해 주면 기쁘지만 필수는 아니다").
6. 채움 여부(평점의 별·즐겨찾기)는 Material Symbols의 **FILL 축**으로 바꾼다(이 패키지는 FILL 축을 바꿀 수 있다). 지정 방법(어느 요소에 어떤 CSS를 붙일지)은 07과 구현에서, ADR 0014 결정 2(부품 CSS를 직접 덮어쓰지 않음)에 어긋나지 않는 형태로 정한다.
7. 담당: 의존성 추가와 설정은 frontend-dev가 공정 5에서 한다(이 ADR 승인 뒤). 07의 2.7·U-1 수정은 ui-designer가 한다.

### 검토한 선택지

크기는 Outlined 글꼴 파일(woff2)의 크기(unpkg 파일 목록, 확인일 2026-11-06).

| 선택지 | 제공처·유지보수 | 라이선스 | 크기(Outlined) | `mat-icon`에서 쓰는 법 | 판단 |
|---|---|---|---|---|---|
| **`@material-symbols/font-400`**(0.48.3) | marella(Google 공식 저장소 README가 Material Symbols npm 패키지로 안내하는 제공처. 단 "Google은 감시·검증하지 않는다"고 명시). Google의 최신 아이콘으로 자동 갱신되며, 최신은 2026-11-04 공개, 10월에도 0.48.0~0.48.2를 공개 | Apache-2.0 | **약 494 KB**(굵기 400·grade 0·optical size 48로 고정. FILL 축은 가변) | 합자. `fontSet="material-symbols-outlined"` 또는 기본 클래스로 지정 | **채택 제안.** 07은 굵기를 바꾸지 않으므로 고정 굵기로 충분. 모든 축을 가진 판의 약 8분의 1 크기 |
| `material-symbols`(0.48.3) | 같은 제공처(marella). GitHub Actions에서 공개되고 출처 증명(provenance)이 붙어 있음 | Apache-2.0 | 약 4.0 MB(모든 축 가변) | 같음 | 굵기·grade·optical size를 바꾸지 않으므로(07) 쓸데없이 크다 |
| `@fontsource/material-symbols-outlined` / `@fontsource-variable/material-symbols-outlined`(5.3.9) | Fontsource | 패키지 기재는 **OFL-1.1**. Google 공식 저장소의 기재(Apache 2.0)와 어긋나며, 어느 쪽이 맞는지 공식 정보로 확인할 수 없다 | 패키지 전체 5.3 MB / 10.4 MB(압축 해제 후) | `fontSet`에 Fontsource CSS 클래스를 지정 | 라이선스 기재가 공식과 어긋나므로 채택하지 않음 |
| `@material-symbols/svg-400`(0.48.3) | marella | Apache-2.0 | 쓰는 아이콘만(1개 수백 바이트~몇 KB) | SVG. 아이콘마다 `MatIconRegistry`에 등록(`addSvgIcon` 등과 HTTP 로드, 또는 문자열 등록)이 필요 | 통신량은 가장 작지만 아이콘을 더할 때마다 등록이 필요하고, 07의 "이름으로 지정" 방식과 다르다. 글꼴이 너무 크다고 판명될 때의 대안으로 한다(바꿀 때는 새 ADR) |
| Google 공식 저장소(google/material-design-icons의 `variablefont`)에서 파일을 손으로 복사 | Google(공식). Google README는 직접 두는 것보다 Google Fonts를 권한다 | Apache 2.0 | 모든 축 가변(몇 MB) | 합자. CSS(`@font-face`와 클래스)는 직접 작성 | 갱신을 손으로 해야 한다. 사람의 결정(npm으로 함께 넣기)과 맞지 않는다 |
| Google Fonts에서 불러오기 | Google | Apache 2.0 | 쓰는 아이콘만으로 줄일 수 있다(`icon_names`) | 합자 | 07의 2.2.1과 사람의 결정(외부에서 불러오지 않음)에 따라 채택하지 않음 |

### 결과 (좋은 점·주의점)

#### 비용 (월 0원 범위인가)

| 항목 | 내용 |
|---|---|
| 무료 한도 | 오픈 소스(Apache-2.0). 요금 없음 |
| 카드 등록 | 불필요 |
| 한도를 넘었을 때 | 사용량 상한이 없다(자기 서버에서 배포하는 파일이므로) |
| 상한·알림 설정(T-01) | 과금이 없으므로 불필요 |

#### 좋은 점

- 화면을 열 때 Google 서버에 접속하지 않는다. 07의 2.2.1과 일치하고, 이용자 정보(IP 주소 등)를 새 외부 서비스에 보내지 않는다.
- `mat-icon`의 공식 사용법(합자·`fontSet`·`setDefaultFontSetClass`) 그대로 쓸 수 있다.
- 굵기를 고정한 판이라 모든 축을 가진 판보다 크기가 약 8분의 1이다.

#### 주의점

- **크기**: Outlined 글꼴은 약 494 KB로, 처음 열 때 한 번만 불러온다(그 뒤는 브라우저 캐시). Google Fonts의 `icon_names`로 필요한 아이콘만 남기는 방법보다 크다. 구현에서 문제가 되면 SVG(`@material-symbols/svg-400`)나 쓰는 아이콘만으로 글꼴을 줄이는 방법을 새 ADR로 검토한다.
- **제공처**: Google 공식 패키지가 아니다(Google은 감시·검증하지 않는다고 명시). 그래서 버전을 고정하고, 갱신은 PR로 리뷰해서 한다. `package-lock.json`을 커밋한다.
- **optical size가 48로 고정**: 07의 아이콘 표시 크기(`mat-icon` 기본은 24px)에서는 optical size 24의 글자 모양·선 굵기와 보이는 것이 조금 다르다. 구현에서 겉모습을 확인하고, 신경 쓰이면 ui-designer가 07에서 다룬다(모든 축을 가진 `material-symbols`로 바꾸는 경우는 새 ADR).
- **아이콘 이름**: 이 패키지의 이름 목록(`index.d.ts`)에서 07의 2.7에 있는 `favorite_border`를 확인하지 못했다(확인일 2026-11-06). Material Symbols는 채움을 FILL 축으로 바꾸므로 `favorite`와 FILL 축(결정 6)으로 나타낸다. 이름 확인과 07 수정은 ui-designer가 한다. F-36 전환에 쓸 수 있는 `dark_mode`·`light_mode`는 목록에 있었다.
- 0.x 판이라 판 번호로 호환성을 판단할 수 없다(결정 4).
- 07의 2.7의 "미결(8. 미결 사항)"과 U-1은 이 ADR 채택 뒤 ui-designer가 갱신한다.

#### 사람이 정할 것

- 이 ADR을 채택할지(상태를 "채택"으로 할지).

#### 확인한 공식 문서 (확인일: 2026-11-06)

- Google 공식 저장소 README(라이선스는 Apache 2.0, 표기는 필수 아님, npm 패키지는 marella가 제공하며 Google은 감시·검증하지 않음, `variablefont` 파일, Google Fonts를 권함): https://github.com/google/material-design-icons (README: https://raw.githubusercontent.com/google/material-design-icons/master/README.md )
- `@material-symbols/font-400` 정보(판 0.48.3, Apache-2.0, 저장소): https://registry.npmjs.org/@material-symbols/font-400/latest , 공개일 목록: https://registry.npmjs.org/@material-symbols/font-400
- `@material-symbols/font-400` README(굵기 400·grade 0·optical size 48, FILL 축, `outlined.css`, `mat-icon`의 `fontSet`, 라이선스): https://unpkg.com/@material-symbols/font-400@0.48.3/README.md , 파일 크기: https://unpkg.com/@material-symbols/font-400@0.48.3/?meta , 아이콘 이름 목록: https://unpkg.com/@material-symbols/font-400@0.48.3/index.d.ts
- `material-symbols` 정보(Apache-2.0, provenance): https://registry.npmjs.org/material-symbols/latest , 파일 크기: https://unpkg.com/material-symbols@0.48.3/?meta , README: https://github.com/marella/material-symbols/tree/main/material-symbols
- `@material-symbols/svg-400` 정보: https://registry.npmjs.org/@material-symbols/svg-400/latest
- Fontsource 패키지 정보(라이선스 기재 OFL-1.1): https://registry.npmjs.org/@fontsource/material-symbols-outlined/latest , https://registry.npmjs.org/@fontsource-variable/material-symbols-outlined/latest
- Angular Material `mat-icon`(합자 글꼴, 글꼴과 CSS는 이용자가 불러옴, `fontSet`, `setDefaultFontSetClass`): https://github.com/angular/components/blob/main/src/material/icon/icon.md
- 기본 글꼴 클래스 추측(`inferDefaultFontSetClass`): https://github.com/angular/components/blob/main/src/material/icon/icon-registry.ts
