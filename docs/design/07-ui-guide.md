# 07. UI デザインガイド

- 状態: 承認（2026-11-06、Issue #15 コメント 6678921752）
- 担当: ui-designer
- 最終更新: 2026-11-06
- 前提: [02. 画面設計書](02-screens.md)（P-01〜P-18 は決定済み。Issue #15 コメント 6651238358・6676943465）、[01. 要件定義書](01-requirements.md)（N-01, N-02, F-36）、ADR 0014（Angular Material + CDK）、ADR 0015（Google Stitch）、ADR 0016（Material Symbols を npm で同梱）、ADR 0006（地図）
- 人間の決定の反映: Issue #15 コメント 6676583842（試作・07 の確認）、6676943465（ADR 0016・P-16〜P-18）、6678516145（ゲート3 の reviewer の指摘 6678542365 への対応。4.6 の期間の区切りの記号）
- 試作: [mockups/](mockups/README.md)

## 1. 位置づけ

- この文書は**見た目**（色・文字・形・余白・部品の選び方）を決める。画面の項目・操作・状態・遷移は 02 が正で、この文書と試作は 02 に従う（ADR 0015 決定3）。
- **トークンの正はこの文書**。Stitch のデザインシステムは色・角丸しか保存しない（stitch-workflow スキル）ため、試作の見た目と食い違うときはこの文書に従う。試作と 07 の差は「実装は 07 に従う」ことで人間が承認した（コメント 6676583842 の 1.）。
- 実装は Angular Material（Material 3）と CDK で行う（ADR 0014）。トークンは `mat.theme` の Sass mixin で1か所にまとめ、部品の CSS を直接上書きしない（ADR 0014 決定2）。試作の HTML（Tailwind）は実装に使わない。
- **ADR 0014 決定2 の解釈（人間の決定。コメント 6676583842 の 4.）**: `mat.button-toggle-overrides`・`mat.theme-overrides` など、Angular Material が用意した**公式のテーマの API** を使うことは「部品の CSS を直接上書きする」に当たらない。禁止するのは、部品のクラス（`.mat-mdc-*`・`.mdc-*`・`.mat-icon` など）に直接 CSS を書くこと。自作のクラス・自作の部品の CSS は書いてよい。
- 試作は見た目の参考で、Stitch の生成のため細部（並び・余白・サンプルの値）が 02・この文書とずれることがある。ずれたときは 02 とこの文書が正。
- ダーク表示（F-36）の試作は作らない（人間の決定。コメント 6676583842 の 6.）。色は 2.1.3〜2.1.5 で決め、実装で確かめる。

## 2. デザイントークン

### 2.1 色

色の値は Material 3 のテーマの**元になる色**（seed）と、自作の部分（地図の印・横棒・スケルトンなど）で使う値。Angular Material のパレットは、実装のときに公式の `ng generate @angular/material:theme-color` で下の primary・tertiary・error・neutral から作る。作った後、2.1.2・2.1.4 のコントラストを満たすことを実装で確かめる。

#### 2.1.1 色の一覧（ライト表示）

| トークン | HEX | Material 3 の役割 | 用途 |
|---|---|---|---|
| `primary` | `#4338CA` | primary | 主なボタン（検索・ログイン・保存）、リンク、選択中のタブ・チップ、地図の会場の印、「公演予定」のラベルの文字、選択中の切り替え（`mat-button-toggle`）の文字 |
| `on-primary` | `#FFFFFF` | on-primary | primary の上の文字・アイコン |
| `primary-container` | `#EEF2FF` | primary-container | 「公演予定」のラベルの背景、選択中の行・項目の背景、選択中の切り替え（`mat-button-toggle`）の背景 |
| `tertiary` | `#B45309` | tertiary | 評価の星、受賞作バッジの文字・枠、人気順位の表示 |
| `tertiary-container` | `#FFFBEB` | tertiary-container | 受賞作バッジの背景、「NEW」の背景 |
| `success` | `#166534` | （自作。M3 に役割がない） | 「公演中」のラベルの文字、「▲3」の文字 |
| `success-container` | `#F0FDF4` | （自作） | 「公演中」のラベルの背景 |
| `error` | `#B91C1C` | error | 入力の誤り、エラーの文言、削除のボタン |
| `on-error` | `#FFFFFF` | on-error | 削除のボタン（塗り）の文字 |
| `background` / `surface` | `#FFFFFF` | surface | 画面の背景、カード |
| `surface-container-low` | `#F9FAFB` | surface-container-low | SC-01 の左パネル・下のシート、フッターの背景 |
| `surface-container` | `#F3F4F6` | surface-container | 「終了」のラベルの背景、表の見出し行 |
| `placeholder` | `#E5E7EB` | （自作） | 画像（ポスター）がないときの枠、読み込み中のスケルトン、地図の読み込み前の枠 |
| `on-surface` | `#1F2937` | on-surface | 本文・見出しの文字 |
| `on-surface-variant` | `#4B5563` | on-surface-variant | 補足の文字（会場名・期間・件数・原文の併記・出典・集計期間）、「終了」のラベルの文字 |
| `outline` | `#D1D5DB` | outline-variant | カード・入力欄・表の境界線（文字には使わない） |

- 色だけで意味を伝えない。公演の状態は必ず文字（「公演中」「公演予定」「終了」）で示し、色は補助にする。
- ダーク表示（F-36）の色は 2.1.3。どの画面も、ライト表示・ダーク表示の両方で同じトークンの名前を使う（値だけが変わる）。

#### 2.1.2 コントラスト（ライト表示。文字と背景）

WCAG 2.x の計算式で求めた値（小数第2位を切り捨て）。すべて 4.5:1 以上。

| 文字 | 背景 | 比 |
|---|---|---|
| `on-surface` `#1F2937` | `#FFFFFF` | 14.6:1 |
| `on-surface-variant` `#4B5563` | `#FFFFFF` | 7.5:1 |
| `on-surface-variant` `#4B5563` | `surface-container` `#F3F4F6` | 6.8:1 |
| `on-surface-variant` `#4B5563` | `placeholder` `#E5E7EB` | 6.1:1 |
| `on-primary` `#FFFFFF` | `primary` `#4338CA` | 7.9:1 |
| `primary` `#4338CA` | `primary-container` `#EEF2FF` | 7.0:1 |
| `primary` `#4338CA`（リンク） | `#FFFFFF` | 7.9:1 |
| `tertiary` `#B45309` | `#FFFFFF` | 5.0:1 |
| `tertiary` `#B45309` | `tertiary-container` `#FFFBEB` | 4.8:1 |
| `success` `#166534` | `success-container` `#F0FDF4` | 6.8:1 |
| `success` `#166534`（「▲3」） | `#FFFFFF` | 7.1:1 |
| `error` `#B91C1C` | `#FFFFFF` | 6.4:1 |
| `on-error` `#FFFFFF` | `error` `#B91C1C` | 6.4:1 |

#### 2.1.3 色の一覧（ダーク表示。F-36・P-16）

**対象はすべての画面**: SC-01〜SC-14・SC-99（管理者の画面 SC-13・SC-14 を含む。P-16）、ダイアログ DL-01〜DL-04、共通部品 CM-01〜CM-03、地図の上の自作の部分（4.5）。地図（Google Maps）そのものの暗い表示は 02 の 3.8 に従う（`colorScheme`）。

トークンの名前・用途は 2.1.1 と同じ。値だけをダーク表示の値にする。

| トークン | ライト | ダーク | ダークでの注意 |
|---|---|---|---|
| `primary` | `#4338CA` | `#A5B4FC` | ダークでは明るい色になる。塗りのボタンの文字は `on-primary` |
| `on-primary` | `#FFFFFF` | `#1E1B4B` | 塗りのボタン・会場の印の数字 |
| `primary-container` | `#EEF2FF` | `#312E81` | 選択中の切り替え・「公演予定」・「修正済み」のラベルの背景 |
| `tertiary` | `#B45309` | `#FCD34D` | 評価の星・受賞作バッジ |
| `tertiary-container` | `#FFFBEB` | `#451A03` | 受賞作バッジ・「NEW」の背景 |
| `success` | `#166534` | `#86EFAC` | 「公演中」・「▲3」の文字 |
| `success-container` | `#F0FDF4` | `#052E16` | 「公演中」のラベルの背景 |
| `error` | `#B91C1C` | `#FCA5A5` | 入力の誤り・削除のボタン |
| `on-error` | `#FFFFFF` | `#450A0A` | 削除のボタン（塗り）の文字 |
| `background` / `surface` | `#FFFFFF` | `#111827` | 画面の背景・カード |
| `surface-container-low` | `#F9FAFB` | `#1F2937` | SC-01 の左パネル・下のシート、フッター |
| `surface-container` | `#F3F4F6` | `#374151` | 「終了」のラベル、表の見出し行、画面の上の案内の枠 |
| `placeholder` | `#E5E7EB` | `#4B5563` | ポスターがないとき・スケルトン・地図の読み込み前 |
| `on-surface` | `#1F2937` | `#F3F4F6` | 本文・見出し |
| `on-surface-variant` | `#4B5563` | `#D1D5DB` | 補足の文字 |
| `outline` | `#D1D5DB` | `#4B5563` | 境界線（文字には使わない） |

- ポスターの画像は明るさを変えない（写真に手を加えない）。
- 影（2.5）はダークでは見えにくいが、Angular Material の既定のまま（境界線 `outline` で区切りが分かる）。
- ダイアログ・メニュー・通知（`MatSnackBar`）の背景は、Angular Material のテーマが作る M3 の役割（`surface-container-high` など）の色をそのまま使う。
- reCAPTCHA（SC-09）は Google の部品で、見た目は変えない（ダーク表示でもライトのまま）。

#### 2.1.4 コントラスト（ダーク表示。文字と背景）

2.1.2 と同じ計算式（小数第2位を切り捨て）。すべて 4.5:1 以上。

| 文字 | 背景 | 比 |
|---|---|---|
| `on-surface` `#F3F4F6` | `surface` `#111827` | 16.1:1 |
| `on-surface` `#F3F4F6` | `surface-container-low` `#1F2937` | 13.3:1 |
| `on-surface` `#F3F4F6` | `surface-container` `#374151` | 9.3:1 |
| `on-surface-variant` `#D1D5DB` | `surface` `#111827` | 12.0:1 |
| `on-surface-variant` `#D1D5DB` | `surface-container-low` `#1F2937` | 9.9:1 |
| `on-surface-variant` `#D1D5DB` | `surface-container` `#374151` | 6.9:1 |
| `on-surface-variant` `#D1D5DB` | `placeholder` `#4B5563` | 5.1:1 |
| `on-primary` `#1E1B4B` | `primary` `#A5B4FC` | 8.0:1 |
| `primary` `#A5B4FC`（リンク・選択中のタブ） | `surface` `#111827` | 8.8:1 |
| `primary` `#A5B4FC` | `surface-container-low` `#1F2937` | 7.3:1 |
| `primary` `#A5B4FC` | `surface-container` `#374151` | 5.1:1 |
| `primary` `#A5B4FC`（選択中の切り替え） | `primary-container` `#312E81` | 5.7:1 |
| `tertiary` `#FCD34D` | `surface` `#111827` | 12.3:1 |
| `tertiary` `#FCD34D` | `surface-container-low` `#1F2937` | 10.1:1 |
| `tertiary` `#FCD34D` | `tertiary-container` `#451A03` | 10.3:1 |
| `success` `#86EFAC` | `success-container` `#052E16` | 10.6:1 |
| `success` `#86EFAC`（「▲3」） | `surface` `#111827` | 12.6:1 |
| `error` `#FCA5A5` | `surface` `#111827` | 9.3:1 |
| `error` `#FCA5A5` | `surface-container-low` `#1F2937` | 7.7:1 |
| `on-error` `#450A0A` | `error` `#FCA5A5` | 8.5:1 |

地図の上の自作の部分（4.5）:

| 部分 | 前景 | 背景 | 比 |
|---|---|---|---|
| 会場の印の数字 | `on-primary` `#1E1B4B` | `primary` `#A5B4FC` | 8.0:1 |
| 駅の印のアイコン | `surface` `#111827` | `on-surface` `#F3F4F6` | 16.1:1 |

- 印・円と地図の背景の見分けやすさは、地図の色が Google の表示で決まるため、ここでは数値にしない。実装で、暗い地図・明るい地図の両方で印・円が見分けられることを確かめる。

#### 2.1.5 ライト表示・ダーク表示の実装（Angular Material の公式のテーマ）

ADR 0014 決定2（`mat.theme` で1か所にまとめる）の範囲で作る。新しいライブラリは使わない。

1. `mat.theme` の `color` に、2.1.1 の色から作ったパレットと **`theme-type: color-scheme`** を指定する。こうすると Angular Material は M3 の役割の色（`--mat-sys-*`）を CSS の `light-dark()` で出し、ライト・ダークの両方の値を1つのテーマで持つ（公式のテーマのガイドの方法。名前は実装のときに公式のガイドで確かめる）。
2. 表示の切り替えは、`<html>` の CSS の **`color-scheme` を `light` または `dark`** にすることで行う。どちらにするかは 02 の 3.8 の判定（`localStorage` の値、なければ `prefers-color-scheme`）で決める。判定と設定の方法（最初の描画の前に行う）は 06・実装。ページは読み込み直さない（02 の 3.8）。
3. 画面のスタイルでは `@media (prefers-color-scheme: dark)` を使わない（ブラウザに記憶した表示を優先できないため。F-36 ③）。
4. M3 の役割の色は、生成したパレットのダークの値をそのまま使う。2.1.4 の比を満たさない組み合わせがあれば、公式の `mat.theme-overrides` で 2.1.3 の値にする（1. の公式のテーマの API）。
5. 自作の部分の色（`success`・`success-container`・`placeholder`、地図の上の印・円）は、自作の CSS 変数に `light-dark(ライトの値, ダークの値)` で定義する（例: `--ct-success: light-dark(#166534, #86EFAC)`）。M3 の役割の色を使う部分は `--mat-sys-*` を参照する。
6. `color-scheme` を変えると、ブラウザの標準の部分（スクロールバー・`<input>` の日付など）もその表示になる。

### 2.2 文字

#### 2.2.1 フォント

**端末に入っているフォントを使い、Web フォントは読み込まない。** Web フォントを外部から読み込むと新しい外部サービスになり、費用・利用条件の確認と ADR が要る（ADR 0014 注意点）ため。韓国語・日本語で別の並びにする（同じ漢字でも字形が違うため）。アイコンのフォントは例外で、npm のパッケージをアプリに同梱する（ADR 0016。2.7）。

| 表示言語 | `font-family` |
|---|---|
| 韓国語（`html[lang="ko"]`） | `"Apple SD Gothic Neo", "Malgun Gothic", "Noto Sans KR", "Noto Sans CJK KR", system-ui, sans-serif` |
| 日本語（`html[lang="ja"]`） | `"Hiragino Sans", "Hiragino Kaku Gothic ProN", "Yu Gothic UI", "Meiryo", "Noto Sans JP", "Noto Sans CJK JP", system-ui, sans-serif` |

- 実装: `mat.theme` の `typography` に韓国語の並びを指定し、`html:lang(ja)` のセレクタの中で `typography` だけを指定した `mat.theme` をもう一度含める（部品の CSS は上書きしない）。表示言語の切り替えで `<html lang>` を変える（切り替えるとページを読み込み直す。02 の 3.1）。
- Android の標準のフォント（Noto Sans CJK）は `sans-serif` で選ばれる。
- **試作のフォント**: Stitch は端末のフォントを指定できないため、試作は **Noto Sans KR**（Android の標準と同じ系統）で作る。試作のフォントの確認は「HTML の見出し・本文の `font-family` が Noto Sans（KR）であること」とする。

#### 2.2.2 文字の大きさ（Material 3 の型の尺度。既定の値のまま使う）

| 型（M3） | 大きさ / 行の高さ | 太さ | 使う場所 |
|---|---|---|---|
| `headline-small` | 24px / 32px | 400 | 画面の見出し（SC-02 の公演名、各画面のタイトル） |
| `title-large` | 22px / 28px | 400 | ヘッダーのサービス名、ダイアログの見出し |
| `title-medium` | 16px / 24px | 500 | 節の見出し（「주변 역」「리뷰」など）、カードの公演名 |
| `body-large` | 16px / 24px | 400 | 入力欄の文字、SC-02 の本文 |
| `body-medium` | 14px / 20px | 400 | カードの会場名・期間、表のセル、レビューの本文 |
| `body-small` | 12px / 16px | 400 | 出典、原文の併記、集計期間、投稿日 |
| `label-large` | 14px / 20px | 500 | ボタン、タブ |
| `label-medium` | 12px / 16px | 500 | 状態のラベル・バッジ・チップ |

- 本文の最小は 12px。12px は補足（出典・原文・日付・集計期間）だけに使う。
- 韓国語・日本語は英字より字面が大きいので、太さは 400 / 500 の2つだけ使う（700 は使わない）。
- 数字（価格・距離・件数・日付）は `font-variant-numeric: tabular-nums` で桁をそろえる（表・一覧・集計期間）。

### 2.3 形（角丸）

Material 3 の既定の形（`--mat-sys-corner-*`）をそのまま使う。

| 部品 | 角丸 |
|---|---|
| ボタン（塗り・枠・文字） | 全体を丸める（full） |
| 入力欄（outline） | 4px（extra-small） |
| ラベル・バッジ・チップ | 8px（small） |
| カード、SC-01 の左パネルの中の枠、地図の上のボタン | 12px（medium） |
| 下から出るシート（上の2つの角） | 28px（extra-large） |
| ダイアログ | 28px（extra-large） |
| 地図の会場の印 | 円 |
| ポスターの枠 | 8px（small） |

Stitch のデザインシステムには `ROUND_TWELVE`（カードの角丸）を指定する。

### 2.4 余白・配置

- **余白は 4px の倍数**: 4 / 8 / 12 / 16 / 24 / 32 / 48px。部品の中は 8・12・16、節と節の間は 24（スマホ表示）・32（PC 表示）。
- 画面の左右の余白: スマホ表示 16px、PC 表示 24px。
- 表示の切り替え: CDK の `Breakpoints.Handset` が「スマホ表示」、それ以外が「PC 表示」（02 の 1.）。
- 中身の最大の幅: SC-02・SC-03・SC-11・SC-12・SC-13・SC-14 は 1200px で中央に置く。フォームだけの画面（SC-04〜SC-10）は 400px の1段で中央に置く。
- SC-01 の PC 表示: 左のパネルの幅は **400px** で固定、残りを地図にする。パネルの中は上に条件、下に一覧（02）。
- 一覧のカードの並び（SC-11 の PC 表示）: 1列の幅が 280px 以上になる数の列。

#### 2.4.1 SC-02 の PC 表示の配置（人間の指摘への対応。コメント 6676583842 の 1.）

試作（SC-02 desktop）は右の列が「周辺の駅・コインロッカー」だけで、左の列より短く下が大きく空いていた。02 の SC-02 は PC の2段組みの中身を「左: 基本情報・予約・統計、右: 周辺、その下にレビュー」と決めている。この範囲で（項目を増やさず、02 が列を決めている項目は動かさず）、次のように置く。

| 場所 | 置くもの（上から） | 02 との対応 |
|---|---|---|
| 左の列（幅 2） | ① 基本情報（ポスター・公演名・状態・期間・会場・ジャンル・出演者・価格・公演時間・上演時間・観覧年齢） ② 予約 ③ 統計・評判（評価・受賞作・オープンラン・人気順位と集計期間） | 02「左: 基本情報・予約・統計」 |
| 右の列（幅 1） | ① **操作の枠**（`mat-card` outlined）: 「즐겨찾기」/「즐겨찾기 해제」（`mat-stroked-button`。管理者には出さない）、管理者だけ「관리 화면에서 수정」（`mat-stroked-button`）。ボタンは枠の幅いっぱい ② 周辺（駅 → コインロッカー） | 02 は「お気に入り」「管理者だけ」の項目を列に割り当てていない。それを右の列に置く。周辺は 02「右: 周辺」のとおり |
| 2段の下（全幅） | レビュー（自分のレビュー・「리뷰 쓰기」→ レビューの一覧・ページ送り） | 02「その下にレビュー」 |

- 右の列は **`position: sticky`** にする（上の位置はヘッダーの高さ + 24px）。左の列を読み進めても、操作と周辺が画面に見え続ける。右の列が画面の高さより高いときは、右の列の中だけをスクロールできるようにする（`max-height: calc(100vh - ヘッダーの高さ - 48px)`、`overflow-y: auto`）。これで右の列の下の空きは「スクロールのための余白」になる。
- 右の列の幅の比は左 2 : 右 1（変えない）。
- 操作の枠は、どのログインの状態でも少なくとも1つのボタンがある（未ログイン・一般ユーザー: 「즐겨찾기」、管理者: 「관리 화면에서 수정」）。
- スマホ表示は 02 の順（基本情報 → 統計 → 予約 → 周辺 → レビュー）。「즐겨찾기」（管理者は「관리 화면에서 수정」）は基本情報の公演名・状態の下に置く（試作 SC-02 mobile のとおり）。
- 予約・統計を右の列に移すと右の空きはさらに減るが、02 の「左: 基本情報・予約・統計」を変えることになるため、この文書では行わない（変える場合は architect が 02 を直す）。

### 2.5 影・境界線

- カードは**枠のカード**（`mat-card` の `appearance="outlined"`、境界線 `outline`）。影は使わない。
- 影（M3 の elevation）を使うのは、地図の上に重なる部品（地図の上のボタン、スマホ表示の下のシート、メニュー、ダイアログ）だけ。Angular Material の既定の影のまま。

### 2.6 押せる要素の大きさ

- 押せる要素は縦横 **44px 以上**。Angular Material の密度 0（ADR 0014 決定3）では、ボタン・アイコンボタン・チェックボックス・ラジオの押せる範囲は 48px、入力欄の高さは 56px で満たす。
- **`mat-button-toggle`**（言語の切り替え・SC-01 の場所の方法・SC-03 の期間）は密度 0 でも高さ 40px で、44px に足りない。テーマで公式のテーマの API `mat.button-toggle-overrides` の `height` を **48px** にする。公式のテーマの API なので ADR 0014 決定2 に反しない（1. の解釈。人間の決定、コメント 6676583842 の 4.）。実装で高さが 48px になることを確かめる。
- 自作の部分も 44px 以上にする: 文字のリンク（SC-04 の「회원가입」など。行の高さを 44px にする）、地図の会場の印（見た目 32px、押せる範囲 44px）、カード全体（押すと SC-02）、ページ送り。
- 押せる要素どうしの間は 8px 以上あける。

### 2.7 アイコン

- `mat-icon` で Material Symbols の **Outlined** を使う。フォントは npm のパッケージ `@material-symbols/font-400` の `outlined.css` だけを同梱し、Google Fonts からは読み込まない（ADR 0016 決定1・2）。アプリの起動のときに `MatIconRegistry.setDefaultFontSetClass('material-symbols-outlined', 'mat-ligature-font')` を呼び、`<mat-icon>名前</mat-icon>` で使う（ADR 0016 決定3）。
- 使うのは意味が決まったものだけ:

| 意味 | 名前 | 塗り（FILL 軸） |
|---|---|---|
| メニュー | `menu` | 0 |
| 評価の星（表示） | `star` | 1（塗る） |
| DL-01 の星（1〜5 の選択） | `star` | 選んだ星まで 1、それより後は 0 |
| 外部のリンク | `open_in_new` | 0 |
| 現在地 | `my_location` | 0 |
| 条件 | `tune` | 0 |
| 閉じる | `close` | 0 |
| 駅 | `train` | 0 |
| コインロッカー | `lock` | 0 |
| お気に入り（未登録） | `favorite` | 0（枠だけ） |
| お気に入り（登録済み） | `favorite` | 1（塗る） |
| ダーク表示に切り替える（いまライト表示） | `dark_mode` | 0 |
| ライト表示に切り替える（いまダーク表示） | `light_mode` | 0 |

- **塗りの切り替え（FILL 軸。ADR 0016 決定6）**: `favorite_border` は使わない（ADR 0016 の確認でパッケージの名前の一覧になかった）。塗りは自作のクラス（例: `.ct-icon-filled { font-variation-settings: 'FILL' 1; }`）を `<mat-icon>` の要素に付けて切り替える。Angular Material の部品のクラス（`.mat-icon` など）には CSS を書かない（1. の解釈）。
- アイコンの名前の確認: ADR 0016 の確認（2026-11-06、パッケージの `index.d.ts`）で `dark_mode`・`light_mode` があり、`favorite_border` がないことが分かっている。ほかの名前（`menu`・`star`・`open_in_new`・`my_location`・`tune`・`close`・`train`・`lock`・`favorite`）は、この文書の更新では一覧を開いて確かめられなかった。frontend-dev が依存関係を入れるときに `index.d.ts` で確かめ、ないものがあれば ui-designer に報告する。
- アイコンだけのボタンには `aria-label` を付ける。アイコンと文字を並べるボタンでは文字を必ず出す。
- 表示の大きさは `mat-icon` の既定（24px）。パッケージは optical size 48 に固定されている（ADR 0016 注意点）。実装で見た目が気になる場合は ui-designer に報告する。

## 3. Angular Material・CDK の部品との対応

02 の要素を、どの部品で作るかの対応。Material にない部品は CDK で自作する（ADR 0014 決定5）。

| 02 の要素 | 部品 | 見た目の決まり |
|---|---|---|
| CM-01 ヘッダー | `mat-toolbar`。スマホ表示のメニューは `mat-sidenav`（右から開く） | 背景 `surface`、下に境界線 `outline`。高さ PC 64px・スマホ 56px。選択中の画面の文字を `primary` |
| 表示言語の切り替え | `mat-button-toggle-group`（「한국어」「日本語」） | 言語名は各言語で書く（切り替えても変えない）。**選択中の側は背景 `primary-container`・文字 `primary`**（薄い藍色。`primary` の塗りは使わない。人間の決定、コメント 6676583842 の 3.）。高さ 48px（2.6） |
| ライト表示・ダーク表示の切り替え（F-36） | `mat-icon-button` | アイコンは押した後の表示（ライト表示のとき `dark_mode`、ダーク表示のとき `light_mode`）。`aria-label` も押した後の表示（韓国語「다크 표시로 전환」/「라이트 표시로 전환」、日本語「ダーク表示に切り替える」/「ライト表示に切り替える」。02 の 3.1）。色は `on-surface-variant` |
| CM-02 フッター | 自作（`<footer>`） | 背景 `surface-container-low`、文字 `body-small`・`on-surface-variant` |
| CM-03 通知 | `MatSnackBar` | 画面の下の中央。既定の見た目 |
| 主な操作（検索・ログイン・保存・登録） | `mat-flat-button`（塗り） | 1画面に1つを目安にする |
| 補助の操作（条件をクリア・キャンセル・お気に入り・レビューを書く） | `mat-stroked-button`（枠） | — |
| リンクの操作（会員登録・パスワードを忘れた方・ページ内の移動） | `mat-button`（文字）または `<a>` | 文字 `primary` |
| 削除・退会 | `mat-flat-button`（色 `error`、文字 `on-error`） | DL-02・DL-03 の確定のボタンだけ |
| 入力欄 | `mat-form-field`（`appearance="outline"`、`subscriptSizing="dynamic"`） | 誤りは欄の下に `mat-error`（02 の 3.3）。長い文言で切れないよう `dynamic` にする |
| 期間（開始日・終了日） | `mat-date-range-input` + `mat-date-range-picker` | 日付の形式は `YYYY-MM-DD` |
| 区・ジャンル（複数）、表示の状態 | `mat-select`（区・ジャンルは `multiple`） | — |
| 場所の方法（区・地図の範囲・会場） | `mat-button-toggle-group`（1つだけ選ぶ） | 選択中の色・高さは言語の切り替えと同じ。日本語で入らないときは折り返す（6.） |
| 公演の状態（公演中・公演予定）、「終わった公演も含める」 | `mat-checkbox` | — |
| 会場の候補 | `mat-autocomplete` | — |
| SC-03 の期間（日次・週次・月次） | `mat-button-toggle-group` | 選択中の色・高さは言語の切り替えと同じ |
| SC-03 のジャンル | PC: `mat-chip-listbox`（1つ選ぶ）、スマホ: `mat-select`（02） | — |
| タブ（SC-03、SC-01 の現在地の周辺） | `mat-tab-group` | — |
| 公演のカード（02 の 3.7） | `mat-card`（`outlined`）全体を `<a>` にする | 4.4 |
| 状態のラベル・受賞作バッジ・「原文」・「全国基準」 | 自作（押せないラベル。`<span>`） | 角丸 8px、`label-medium`。押せないので `mat-chip` は使わない |
| 評価（★平均（件数）） | `mat-icon`（`star`、FILL 1、色 `tertiary`）+ 文字 | 「★4.3（12）」。0件は「아직 리뷰가 없습니다」を `body-small` |
| 人気順位（SC-02）・集計期間（SC-02・SC-03） | 自作（文字） | 4.6 |
| ページ送り | `mat-paginator`（件数の選択は出さない。20件固定） | — |
| 管理の公演一覧（PC） | `mat-table` | 行全体を押せる。見出し行の背景 `surface-container` |
| スマホ表示の条件 | `MatDialog`（全画面） | — |
| スマホ表示の下のシート（SC-01） | **CDK で自作**（地図を見ながら操作するため、モーダルの `MatBottomSheet` は使わない） | 上の角 28px、上に「지도 / 목록」の切り替え |
| ダイアログ DL-01〜DL-04 | `MatDialog` | 角 28px。ボタンは右下に「キャンセル」（文字）→ 確定（塗り）の順 |
| DL-01 の星（1〜5） | 自作（`mat-icon-button` を5つ。ラジオのグループとして読み上げる） | 押せる範囲 48px。選んだ星まで FILL 1（2.7） |
| 読み込み中（ボタン） | `mat-progress-spinner`（20px）をボタンの中に | 押せなくする（02 の 3.6） |
| 読み込み中（一覧） | 自作のスケルトン（色 `placeholder`） | カードと同じ大きさの灰色の枠 |
| 横棒（SC-03 のジャンル別トレンド） | CSS で自作（02） | 棒の色 `primary`、下地 `surface-container`。割合の数字を必ず文字で出す |
| 地図 | `@angular/google-maps`（ADR 0006） | 4.5 |
| reCAPTCHA（SC-09） | Google の部品（ADR 0011） | 見た目は変えない |

- `mat-button-toggle` の選択中の色と高さは、テーマで1回だけ指定する: `mat.button-toggle-overrides` の `height: 48px`、`selected-state-background-color: var(--mat-sys-primary-container)`、`selected-state-text-color: var(--mat-sys-primary)`（トークンの名前は実装のときに公式の一覧で確かめる）。M3 の役割を参照するので、ダーク表示でも自動で 2.1.3 の色になる。

## 4. 共通部品・共通の表示の見た目

### 4.1 CM-01 ヘッダー

- PC 表示: 左にサービス名「CatTicket」、その右に「지도」「인기·트렌드」（ログインの状態に応じて「즐겨찾기」「관리」）、右端にログインの状態に応じたボタン（「로그인」「회원가입」/「계정（ニックネーム）」「로그아웃」）、言語の切り替え、ライト表示・ダーク表示の切り替え（この順）。
- スマホ表示: 左にサービス名、右に言語の切り替え → ライト表示・ダーク表示の切り替え → メニューのボタン。ほかの項目はメニューの中（02 の 3.1）。
- 管理者にも同じ位置に切り替えを出す（P-16）。

### 4.2 CM-02 フッター

- 出典を2行で表示（KOPIS、ソウル交通公社）。文言は韓国語の原文のまま（02 の 3.2）。
- SC-01 のスマホ表示では、地図の左下に「출처」の文字のリンク（押せる範囲 44px）を置き、押すと同じ内容をダイアログで出す。

### 4.3 状態の表示（02 の 3.6）

| 状態 | 見た目 |
|---|---|
| 読み込み中（一覧） | カードと同じ形の灰色の枠（`placeholder`）を3つ。動き（点滅）は付けない（動きを減らす設定の利用者に配慮し、作る手間も減らす） |
| 読み込み中（ボタン） | ボタンの文字の代わりにスピナー（20px）、ボタンは押せない |
| 読み込み中（地図） | 地図の領域を `placeholder` の色で塗る |
| 空 | 一覧の場所の中央に、02 の各画面の文言を `body-medium`・`on-surface-variant` で。リンク（SC-11 の SC-01 へのリンクなど、02 にあるものだけ）はその下に `mat-button` |
| エラー（通信・サーバー） | 一覧の場所の中央に「읽어 오지 못했습니다」（`body-medium`）と「다시 불러오기」（`mat-stroked-button`）（02 の 3.6） |
| 入力の誤り | 入力欄の下に `mat-error`（色 `error`） |
| 画面の上の案内（SC-04 の結果・ロック中など） | 入力欄の上に、背景 `surface-container`・文字 `on-surface` の枠（角 12px）。エラーの案内は左に 4px の `error` の線を付ける（色だけに頼らず、文言で伝える） |

ライト表示・ダーク表示のどちらでも同じトークンの名前を使う（2.1.3）。

### 4.4 公演のカード（02 の 3.7）

- スマホ表示・SC-01 の左パネル: 横長（左にポスター 72×96px、右に文字）。SC-11 の PC 表示: 同じ形を複数列に並べる。
- 文字の順: 公演名（`title-medium`。2行まで、超えたら「…」）→ 会場名（`body-medium`・`on-surface-variant`）→ 公演期間 → ジャンル・状態のラベル・受賞作バッジを1行に → 評価。
- 日本語表示の公演名・会場名は、翻訳（大きく）の下に原文（`body-small`・`on-surface-variant`）を1行で（02 の 3.4）。
- ポスターがないときは `placeholder` の枠。

### 4.5 地図（SC-01 だけ）

- 会場の印: 円（見た目 32px、押せる範囲 44px）、背景 `primary`、中に公演の数（`on-primary`、`label-medium`）。選択中の印は 40px にし、`surface` の色の枠（2px）を付ける。
- 現在地の印: `primary` の円に `surface` の色の枠。半径 500m の円: 線 `primary`、塗り `primary` の 12%。
- 駅の印: `on-surface` の円に `surface` の色の `train` アイコン。
- 地図の上のボタン（「근처 보기」「이 범위로 검색」）: `mat-flat-button` を地図の上に重ね、影を付ける。色はアプリの表示（ライト・ダーク）に従う。
- Google Maps の地図の見た目（スタイル）は変えない。暗い表示は 02 の 3.8 のとおり `colorScheme`（`LIGHT` / `DARK`）だけで行う。
- **印・円の色は地図の表示に合わせる**: 地図がダーク表示で作られたときは 2.1.3 のダークの値、ライトで作られたときは 2.1.1 のライトの値を使う。通常はアプリの表示と同じになる（02 の 3.8）。02 の 3.8 の「実装で確かめること」で、map ID のない地図が暗くならず**地図だけライト表示のまま**にしたときは、アプリがダーク表示でも印・円は**ライトの値**にする（明るい地図の上で明るい `primary` `#A5B4FC` の印は見分けにくいため）。地図の上のボタンはアプリの表示のまま。
- 地図の上限（02 の 6.1）・読み込み前の領域の色は、アプリの表示のトークン（`surface-container`・`placeholder`）。

### 4.6 人気順位・集計期間（SC-02・SC-03。P-12・P-17・P-18）

- **集計期間の形（P-18）**: ラベル「집계 기간」（日本語「集計期間」）の後に `YYYY-MM-DD`。日次は1日（例: `2026-11-04`）、週次・月次は初日〜末日（例: `2026-10-26~2026-11-01`）。両方の言語で同じ形。**日付と日付の区切りは半角の `~`（U+007E）で、前後に空白を入れない**（02 の P-18 の例に合わせる。集計期間・ジャンル別トレンドの集計期間・公演期間の表示で同じ。全角の `〜`・`～` は使わない）。`body-small`・`on-surface-variant`・`tabular-nums`。日付の途中で折り返さない（日付ごとに `white-space: nowrap`。折り返すなら区切りの記号の後）。
- **SC-02 の人気順位（P-02・P-12）**: 1つの順位を1行にする: 「주간·전체 3위」（`body-medium`・`tertiary`）の後に集計期間（上の形）。最大6行。チップ・枠にはしない（押せないため）。
- **SC-03 の集計期間**: 期間・ジャンルの選択の下、一覧の上に1行で。人気急上昇は「現在の期間の集計期間」と「比べる期間の集計期間」を2行で（02）。
- **人気急上昇で比べる期間の順位がない（P-17）**: 現在の期間の集計期間は通常と同じ位置に出し、一覧の場所に 02 の文言を 4.3 の「空」と同じ見た目（中央、`body-medium`・`on-surface-variant`）で出す。ボタンは置かない（02 にない）。

## 5. 試作のない画面・状態の見た目

試作を作ったのは 9. の7つだけ（Issue #15 コメント 6651238358）。ほかの画面・状態は、試作の画面と 2〜4. の決まりで作る。ダーク表示はすべての画面で試作を作らず、2.1.3 の値で作る。

| 画面・状態 | 代表にする試作 | 見た目の決まり |
|---|---|---|
| SC-05〜SC-10（フォームの画面） | SC-04（mobile） | 1段のフォーム（最大 400px）。上から: 画面のタイトル（`headline-small`）→ 案内の文（あれば）→ 入力欄 → 主なボタン（幅いっぱい、`mat-flat-button`）→ 文字のリンク。PC 表示でも同じ形で中央に置く |
| SC-06・SC-10（リンクを開いた後の結果） | SC-04 | 確認中はスピナー（40px）を中央に。結果は 4.3 の「画面の上の案内」の枠に文言、その下に主なボタン |
| SC-09 の reCAPTCHA | SC-04 | 入力欄と「재인증 메일 보내기」の間に置く |
| SC-11 お気に入り | SC-01（カード） | 4.4 のカードの右下に「해제」（`mat-button`）。PC は複数列（2.4） |
| SC-12 アカウント設定 | SC-04（フォーム） | 「닉네임」「비밀번호」「탈퇴」の3つの枠（`mat-card` outlined）を縦に。「탈퇴하기」は `mat-stroked-button`（色 `error`）、DL-03 の確定だけ塗りの `error` |
| SC-02 の PC 表示の右の列 | SC-02（desktop） | 2.4.1 |
| SC-03 の PC 表示 | SC-03（mobile） | 2.4（最大 1200px）・3.（ジャンルはチップ）・4.6 |
| SC-13 のスマホ表示 | SC-13（desktop） | 表の1行を1枚のカードにする（同じ項目。02） |
| SC-14 管理: 公演の修正 | SC-13（desktop） | 項目ごとに「項目名・自動収集の値・現在の値・修正欄・修正を取り消す」を PC は横1行、スマホは縦に（02）。修正した項目は現在の値の横に「수정됨」のラベル（`primary-container`）。「저장」は画面の下に固定しない（ページの最後に置く） |
| SC-99 エラー | — | 中央に文言（`headline-small`）と SC-01 へのリンク（`mat-flat-button`）。イラスト・画像は置かない |
| DL-01〜DL-04 | — | 3. の「ダイアログ」の行 |
| 読み込み中・空・エラー（全画面） | — | 4.3 |
| 地図を表示できない（02 の 6.1） | SC-01 | 地図の領域を `surface-container` で塗り、中央に 02 の文言。スマホ表示では下のシートを開いた状態 |
| メールの上限（02 の 6.2） | SC-04 | 4.3 の「画面の上の案内」（エラーの線付き） |
| 翻訳がない（02 の 3.4、P-06） | — | 原文の右に「원문 / 原文」のラベル（`surface-container` の背景、`label-medium`） |
| 「全国基準」「ソウル基準」（SC-03） | SC-03 | タブの見出しのすぐ下に `primary-container` のラベル。説明の文は `body-small` |
| SC-03 の人気急上昇・ジャンル別トレンド | SC-03（人気順位のタブ） | 同じ期間・ジャンルの選び方。「NEW」は `tertiary-container` の背景・`tertiary` の文字、「▲3」は `success` の文字。上がった公演が0件のとき（02、01 の F-33 ⑪）の「-」は `on-surface-variant` の文字・背景なし（「NEW」「▲3」と同じ場所・同じ文字の大きさ。色と背景の有無で区別する）。`aria-label` は 02 のとおり「순위 상승 없음」（日本語「順位の上昇なし」） |
| SC-03 の人気急上昇で比べる期間の順位がない（P-17） | SC-03 | 4.6 |
| ダーク表示（全画面・F-36） | — | 2.1.3〜2.1.5。地図の上の部分は 4.5 |

## 6. 日本語表示の注意（試作は韓国語で作った）

日本語は同じ意味でも韓国語より文字が多くなることがある。実装では日本語でも次を確かめる。

| 場所 | 韓国語 → 日本語の例 | 決まり |
|---|---|---|
| ヘッダー（PC） | 인기·트렌드 → 人気・トレンド、즐겨찾기 → お気に入り、로그아웃 → ログアウト | 項目を折り返さない（`white-space: nowrap`）。PC 表示のいちばん狭い幅（960px）で日本語・管理者・一般ユーザーの3つを確かめる（言語の切り替え・ライト／ダークの切り替えを含めて） |
| ヘッダー（スマホ） | 言語名は切り替えても変わらない | サービス名・言語の切り替え（48px の高さ）・ライト／ダークの切り替え・メニューが幅 360px で1行に入ることを確かめる。入らないときはサービス名を `title-medium` にする |
| SC-01 の場所の方法 | 구 / 지도 범위 / 공연장 → 区 / 地図の範囲 / 会場 | 400px のパネルに入らないときは2行に折り返す |
| ボタン | 조건 지우기 → 条件をクリア、재인증 메일 보내기 → 再認証のメールを送る、관리 화면에서 수정 → 管理画面で修正 | ボタンの幅を固定しない。スマホ表示のフォームのボタン・SC-02 PC の操作の枠のボタンは幅いっぱい |
| SC-04 のロック中の案内 | 解除の方法の2つのリンクが長い | リンクは1行に1つずつ縦に並べる |
| カード | 公演名の翻訳 + 原文の2段（02 の 3.4） | 公演名は2行まで、原文は1行まで（超えたら「…」）。カードの高さを固定しない |
| 表（SC-13） | 관리자 수정 → 管理者の修正 | 見出しは折り返してよい。公演 ID・期間の列は折り返さない |
| 入力の誤り・案内の文 | 日本語のほうが長い | `subscriptSizing="dynamic"` で複数行を許す |
| SC-03 の人気急上昇の P-17 の文言 | 「比べる期間（…）の順位データがないため、人気急上昇を表示できません」 | 複数行を許す。集計期間の日付の途中では折り返さない（4.6） |
| 日付・集計期間 | — | 両方の言語で `YYYY-MM-DD`（02 の文言の例、P-18） |

## 7. アクセシビリティ

ADR 0014 決定6 のとおり、要件としての達成基準（WCAG の等級）は置かない。次は**この文書の見た目の決まり**として守り、試作でも守る。

- 文字と背景のコントラストは 4.5:1 以上。ライト表示（2.1.2）とダーク表示（2.1.4）の両方。
- 押せる要素は 44px 以上（2.6）。
- 色だけで意味を伝えない（状態・エラー・順位の変化は文字でも示す）。
- フォーカスが見えにくいときは `mat.strong-focus-indicators()` を使う（ADR 0014 決定6）。ダーク表示でも見えることを確かめる。
- 画像（ポスター）には公演名を代替テキストにする。
- アイコンだけのボタン（メニュー・ライト／ダークの切り替えなど）には `aria-label`（2.7、3.）。

## 8. 未決事項

未決事項はない。U-1〜U-3 は解決した。

| No. | 内容 | 結果 | 決定の出典 |
|---|---|---|---|
| U-1 | Material Symbols のアイコンのフォントの配り方 | **解決済み**: npm のパッケージ `@material-symbols/font-400` の Outlined を同梱し、Google Fonts から読み込まない。2.7 に反映 | 人間の決定: Issue #15 コメント 6676583842 の 5.、ADR 0016 の採用: コメント 6676943465 |
| U-2 | 色・レイアウトの採否 | **解決済み**: 試作7つを採用、色（primary `#4338CA` ほか）・SC-01 の配置を採用。言語の切り替えの色（3.）、SC-02 PC の右の列（2.4.1）、ダーク表示（2.1.3〜2.1.5）を反映 | 人間の決定: Issue #15 コメント 6676583842 の 1.〜3.・6. |
| U-3 | `mat.button-toggle-overrides` で高さを 48px にすることが ADR 0014 決定2 の範囲か | **解決済み**: 公式のテーマの API は「部品の CSS を直接上書きする」に当たらない。禁止は部品のクラスに直接 CSS を書くこと。48px のとおり（1.・2.6） | 人間の決定: Issue #15 コメント 6676583842 の 4. |

## 9. 画面 ID と試作・節の対応表

02 のすべての画面 ID。試作は [mockups/](mockups/README.md)（ファイルの一覧と Stitch の画面 ID は README）。試作の見た目がこの文書と違うところ（太さ・角丸・押せる大きさなど）は README の「07 との差」に書いた。実装はこの文書に従う（人間が承認。コメント 6676583842）。ダーク表示は全画面で 2.1.3〜2.1.5。

| 画面 ID | 画面名 | 試作（スマホ表示） | 試作（PC 表示） | 試作がないときの対応 |
|---|---|---|---|---|
| SC-01 | 地図・検索 | [SC-01-mobile.png](mockups/SC-01/SC-01-mobile.png) | [SC-01-desktop.png](mockups/SC-01/SC-01-desktop.png) | 状態（読み込み中・空・エラー・位置情報・地図の上限）は 4.3・5.、地図の上の部分は 4.5 |
| SC-02 | 公演詳細 | [SC-02-mobile.png](mockups/SC-02/SC-02-mobile.png) | [SC-02-desktop.png](mockups/SC-02/SC-02-desktop.png) | PC の右の列は 2.4.1、人気順位は 4.6、DL-01・DL-02 は 3.、状態は 4.3 |
| SC-03 | 人気・トレンド | [SC-03-mobile.png](mockups/SC-03/SC-03-mobile.png) | — | PC 表示は 2.4・3.（ジャンルはチップ）。集計期間・P-17 は 4.6。ほかのタブ・状態は 5.・4.3 |
| SC-04 | ログイン | [SC-04-mobile.png](mockups/SC-04/SC-04-mobile.png) | — | PC 表示は 2.4（同じ形で中央）。結果の表示は 4.3 |
| SC-05 | 会員登録 | — | — | 5.（SC-04 を代表） |
| SC-06 | メール認証の完了 | — | — | 5. |
| SC-07 | パスワード再設定の申込 | — | — | 5.（SC-04 を代表） |
| SC-08 | 新しいパスワードの設定 | — | — | 5.（SC-04 を代表） |
| SC-09 | ロック解除の申込 | — | — | 5.（SC-04 を代表） |
| SC-10 | ロック解除の完了 | — | — | 5. |
| SC-11 | お気に入り | — | — | 5.・4.4 |
| SC-12 | アカウント設定 | — | — | 5.（SC-04 を代表）、DL-03 は 3. |
| SC-13 | 管理: 公演一覧 | — | [SC-13-desktop.png](mockups/SC-13/SC-13-desktop.png) | スマホ表示は 5. |
| SC-14 | 管理: 公演の修正 | — | — | 5.、DL-02・DL-04 は 3. |
| SC-99 | エラー | — | — | 5. |
| CM-01〜CM-03 | 共通部品 | 各試作に含む | 各試作に含む | 4.1〜4.2、3.（ライト／ダークの切り替えは 3.・4.1） |

---

## 한국어

# 07. UI 디자인 가이드

- 상태: 승인(2026-11-06, Issue #15 코멘트 6678921752)
- 담당: ui-designer
- 최종 갱신: 2026-11-06
- 전제: [02. 화면 설계서](02-screens.md)(P-01~P-18은 결정됨. Issue #15 코멘트 6651238358·6676943465), [01. 요건정의서](01-requirements.md)(N-01, N-02, F-36), ADR 0014(Angular Material + CDK), ADR 0015(Google Stitch), ADR 0016(Material Symbols를 npm으로 포함), ADR 0006(지도)
- 사람의 결정 반영: Issue #15 코멘트 6676583842(시안·07 확인), 6676943465(ADR 0016·P-16~P-18), 6678516145(관문 3 reviewer 지적 6678542365 대응. 4.6의 기간 구분 기호)
- 시안: [mockups/](mockups/README.md)

## 1. 위치

- 이 문서는 **겉모습**(색·글자·모양·여백·부품 선택)을 정한다. 화면의 항목·조작·상태·전이는 02가 기준이며, 이 문서와 시안은 02를 따른다(ADR 0015 결정 3).
- **토큰의 기준은 이 문서**. Stitch 디자인 시스템은 색·모서리 둥글기만 저장하므로(stitch-workflow 스킬), 시안의 겉모습과 다르면 이 문서를 따른다. 시안과 07의 차이는 "구현은 07을 따른다"로 사람이 승인했다(코멘트 6676583842의 1.).
- 구현은 Angular Material(Material 3)과 CDK로 한다(ADR 0014). 토큰은 `mat.theme` Sass mixin으로 한 곳에 모으고, 부품의 CSS를 직접 덮어쓰지 않는다(ADR 0014 결정 2). 시안의 HTML(Tailwind)은 구현에 쓰지 않는다.
- **ADR 0014 결정 2의 해석(사람의 결정. 코멘트 6676583842의 4.)**: `mat.button-toggle-overrides`·`mat.theme-overrides` 등 Angular Material이 제공하는 **공식 테마 API**를 쓰는 것은 "부품의 CSS를 직접 덮어쓰기"에 해당하지 않는다. 금지하는 것은 부품의 클래스(`.mat-mdc-*`·`.mdc-*`·`.mat-icon` 등)에 CSS를 직접 쓰는 것. 직접 만든 클래스·직접 만든 부품의 CSS는 써도 된다.
- 시안은 겉모습 참고이며, Stitch 생성 특성상 세부(배치·여백·샘플 값)가 02·이 문서와 어긋날 수 있다. 어긋나면 02와 이 문서가 기준.
- 다크 표시(F-36)의 시안은 만들지 않는다(사람의 결정. 코멘트 6676583842의 6.). 색은 2.1.3~2.1.5에서 정하고 구현에서 확인한다.

## 2. 디자인 토큰

### 2.1 색

색 값은 Material 3 테마의 **바탕이 되는 색**(seed)과, 직접 만드는 부분(지도 표시·가로 막대·스켈레톤 등)에 쓰는 값. Angular Material 팔레트는 구현 때 공식 `ng generate @angular/material:theme-color`로 아래 primary·tertiary·error·neutral에서 만든다. 만든 뒤 2.1.2·2.1.4의 대비를 만족하는지 구현에서 확인한다.

#### 2.1.1 색 목록(라이트 표시)

| 토큰 | HEX | Material 3 역할 | 용도 |
|---|---|---|---|
| `primary` | `#4338CA` | primary | 주요 버튼(검색·로그인·저장), 링크, 선택된 탭·칩, 지도의 공연장 표시, "공연 예정" 라벨의 글자, 선택된 전환(`mat-button-toggle`)의 글자 |
| `on-primary` | `#FFFFFF` | on-primary | primary 위의 글자·아이콘 |
| `primary-container` | `#EEF2FF` | primary-container | "공연 예정" 라벨의 배경, 선택된 행·항목의 배경, 선택된 전환(`mat-button-toggle`)의 배경 |
| `tertiary` | `#B45309` | tertiary | 평가의 별, 수상작 배지의 글자·테두리, 인기 순위 표시 |
| `tertiary-container` | `#FFFBEB` | tertiary-container | 수상작 배지의 배경, "NEW"의 배경 |
| `success` | `#166534` | (직접 만듦. M3에 역할이 없음) | "공연 중" 라벨의 글자, "▲3"의 글자 |
| `success-container` | `#F0FDF4` | (직접 만듦) | "공연 중" 라벨의 배경 |
| `error` | `#B91C1C` | error | 입력 오류, 오류 문구, 삭제 버튼 |
| `on-error` | `#FFFFFF` | on-error | 삭제 버튼(채움)의 글자 |
| `background` / `surface` | `#FFFFFF` | surface | 화면 배경, 카드 |
| `surface-container-low` | `#F9FAFB` | surface-container-low | SC-01의 왼쪽 패널·아래 시트, 푸터의 배경 |
| `surface-container` | `#F3F4F6` | surface-container | "종료" 라벨의 배경, 표의 머리 행 |
| `placeholder` | `#E5E7EB` | (직접 만듦) | 이미지(포스터)가 없을 때의 틀, 로딩 중 스켈레톤, 지도를 불러오기 전의 틀 |
| `on-surface` | `#1F2937` | on-surface | 본문·제목의 글자 |
| `on-surface-variant` | `#4B5563` | on-surface-variant | 보조 글자(공연장명·기간·건수·원문 병기·출처·집계 기간), "종료" 라벨의 글자 |
| `outline` | `#D1D5DB` | outline-variant | 카드·입력란·표의 경계선(글자에는 쓰지 않음) |

- 색만으로 의미를 전하지 않는다. 공연 상태는 반드시 글자("공연 중" "공연 예정" "종료")로 보여 주고 색은 보조로 한다.
- 다크 표시(F-36)의 색은 2.1.3. 모든 화면에서 라이트 표시·다크 표시 모두 같은 토큰 이름을 쓴다(값만 바뀐다).

#### 2.1.2 대비(라이트 표시. 글자와 배경)

WCAG 2.x 계산식으로 구한 값(소수 둘째 자리 버림). 모두 4.5:1 이상.

| 글자 | 배경 | 비 |
|---|---|---|
| `on-surface` `#1F2937` | `#FFFFFF` | 14.6:1 |
| `on-surface-variant` `#4B5563` | `#FFFFFF` | 7.5:1 |
| `on-surface-variant` `#4B5563` | `surface-container` `#F3F4F6` | 6.8:1 |
| `on-surface-variant` `#4B5563` | `placeholder` `#E5E7EB` | 6.1:1 |
| `on-primary` `#FFFFFF` | `primary` `#4338CA` | 7.9:1 |
| `primary` `#4338CA` | `primary-container` `#EEF2FF` | 7.0:1 |
| `primary` `#4338CA`(링크) | `#FFFFFF` | 7.9:1 |
| `tertiary` `#B45309` | `#FFFFFF` | 5.0:1 |
| `tertiary` `#B45309` | `tertiary-container` `#FFFBEB` | 4.8:1 |
| `success` `#166534` | `success-container` `#F0FDF4` | 6.8:1 |
| `success` `#166534`("▲3") | `#FFFFFF` | 7.1:1 |
| `error` `#B91C1C` | `#FFFFFF` | 6.4:1 |
| `on-error` `#FFFFFF` | `error` `#B91C1C` | 6.4:1 |

#### 2.1.3 색 목록(다크 표시. F-36·P-16)

**대상은 모든 화면**: SC-01~SC-14·SC-99(관리자 화면 SC-13·SC-14 포함. P-16), 다이얼로그 DL-01~DL-04, 공통 부품 CM-01~CM-03, 지도 위의 직접 만든 부분(4.5). 지도(Google Maps) 자체의 어두운 표시는 02의 3.8을 따른다(`colorScheme`).

토큰 이름·용도는 2.1.1과 같다. 값만 다크 표시 값으로 한다.

| 토큰 | 라이트 | 다크 | 다크에서의 주의 |
|---|---|---|---|
| `primary` | `#4338CA` | `#A5B4FC` | 다크에서는 밝은 색이 된다. 채움 버튼의 글자는 `on-primary` |
| `on-primary` | `#FFFFFF` | `#1E1B4B` | 채움 버튼·공연장 표시의 숫자 |
| `primary-container` | `#EEF2FF` | `#312E81` | 선택된 전환·"공연 예정"·"수정됨" 라벨의 배경 |
| `tertiary` | `#B45309` | `#FCD34D` | 평가의 별·수상작 배지 |
| `tertiary-container` | `#FFFBEB` | `#451A03` | 수상작 배지·"NEW"의 배경 |
| `success` | `#166534` | `#86EFAC` | "공연 중"·"▲3"의 글자 |
| `success-container` | `#F0FDF4` | `#052E16` | "공연 중" 라벨의 배경 |
| `error` | `#B91C1C` | `#FCA5A5` | 입력 오류·삭제 버튼 |
| `on-error` | `#FFFFFF` | `#450A0A` | 삭제 버튼(채움)의 글자 |
| `background` / `surface` | `#FFFFFF` | `#111827` | 화면 배경·카드 |
| `surface-container-low` | `#F9FAFB` | `#1F2937` | SC-01의 왼쪽 패널·아래 시트, 푸터 |
| `surface-container` | `#F3F4F6` | `#374151` | "종료" 라벨, 표의 머리 행, 화면 위 안내의 틀 |
| `placeholder` | `#E5E7EB` | `#4B5563` | 포스터가 없을 때·스켈레톤·지도를 불러오기 전 |
| `on-surface` | `#1F2937` | `#F3F4F6` | 본문·제목 |
| `on-surface-variant` | `#4B5563` | `#D1D5DB` | 보조 글자 |
| `outline` | `#D1D5DB` | `#4B5563` | 경계선(글자에는 쓰지 않음) |

- 포스터 이미지는 밝기를 바꾸지 않는다(사진에 손대지 않는다).
- 그림자(2.5)는 다크에서 잘 안 보이지만 Angular Material 기본 그대로(경계선 `outline`으로 구분이 된다).
- 다이얼로그·메뉴·알림(`MatSnackBar`)의 배경은 Angular Material 테마가 만드는 M3 역할(`surface-container-high` 등)의 색을 그대로 쓴다.
- reCAPTCHA(SC-09)는 Google 부품이며 겉모습을 바꾸지 않는다(다크 표시에서도 라이트 그대로).

#### 2.1.4 대비(다크 표시. 글자와 배경)

2.1.2와 같은 계산식(소수 둘째 자리 버림). 모두 4.5:1 이상.

| 글자 | 배경 | 비 |
|---|---|---|
| `on-surface` `#F3F4F6` | `surface` `#111827` | 16.1:1 |
| `on-surface` `#F3F4F6` | `surface-container-low` `#1F2937` | 13.3:1 |
| `on-surface` `#F3F4F6` | `surface-container` `#374151` | 9.3:1 |
| `on-surface-variant` `#D1D5DB` | `surface` `#111827` | 12.0:1 |
| `on-surface-variant` `#D1D5DB` | `surface-container-low` `#1F2937` | 9.9:1 |
| `on-surface-variant` `#D1D5DB` | `surface-container` `#374151` | 6.9:1 |
| `on-surface-variant` `#D1D5DB` | `placeholder` `#4B5563` | 5.1:1 |
| `on-primary` `#1E1B4B` | `primary` `#A5B4FC` | 8.0:1 |
| `primary` `#A5B4FC`(링크·선택된 탭) | `surface` `#111827` | 8.8:1 |
| `primary` `#A5B4FC` | `surface-container-low` `#1F2937` | 7.3:1 |
| `primary` `#A5B4FC` | `surface-container` `#374151` | 5.1:1 |
| `primary` `#A5B4FC`(선택된 전환) | `primary-container` `#312E81` | 5.7:1 |
| `tertiary` `#FCD34D` | `surface` `#111827` | 12.3:1 |
| `tertiary` `#FCD34D` | `surface-container-low` `#1F2937` | 10.1:1 |
| `tertiary` `#FCD34D` | `tertiary-container` `#451A03` | 10.3:1 |
| `success` `#86EFAC` | `success-container` `#052E16` | 10.6:1 |
| `success` `#86EFAC`("▲3") | `surface` `#111827` | 12.6:1 |
| `error` `#FCA5A5` | `surface` `#111827` | 9.3:1 |
| `error` `#FCA5A5` | `surface-container-low` `#1F2937` | 7.7:1 |
| `on-error` `#450A0A` | `error` `#FCA5A5` | 8.5:1 |

지도 위의 직접 만든 부분(4.5):

| 부분 | 전경 | 배경 | 비 |
|---|---|---|---|
| 공연장 표시의 숫자 | `on-primary` `#1E1B4B` | `primary` `#A5B4FC` | 8.0:1 |
| 역 표시의 아이콘 | `surface` `#111827` | `on-surface` `#F3F4F6` | 16.1:1 |

- 표시·원과 지도 배경의 구분은 지도 색이 Google 표시로 정해지므로 여기서는 수치로 하지 않는다. 구현에서 어두운 지도·밝은 지도 모두에서 표시·원을 구분할 수 있는지 확인한다.

#### 2.1.5 라이트 표시·다크 표시의 구현(Angular Material 공식 테마)

ADR 0014 결정 2(`mat.theme`으로 한 곳에 모음)의 범위에서 만든다. 새 라이브러리는 쓰지 않는다.

1. `mat.theme`의 `color`에 2.1.1의 색으로 만든 팔레트와 **`theme-type: color-scheme`**을 지정한다. 그러면 Angular Material은 M3 역할의 색(`--mat-sys-*`)을 CSS `light-dark()`로 내보내고, 라이트·다크 두 값을 테마 하나로 가진다(공식 테마 가이드의 방법. 이름은 구현 때 공식 가이드로 확인한다).
2. 표시 전환은 `<html>`의 CSS **`color-scheme`을 `light` 또는 `dark`**로 하는 것으로 한다. 어느 쪽으로 할지는 02의 3.8 판정(`localStorage` 값, 없으면 `prefers-color-scheme`)으로 정한다. 판정과 설정 방법(첫 그리기 전에 함)은 06·구현. 페이지는 다시 불러오지 않는다(02의 3.8).
3. 화면 스타일에서는 `@media (prefers-color-scheme: dark)`를 쓰지 않는다(브라우저에 기억한 표시를 우선할 수 없으므로. F-36 ③).
4. M3 역할의 색은 생성한 팔레트의 다크 값을 그대로 쓴다. 2.1.4의 비를 만족하지 않는 조합이 있으면 공식 `mat.theme-overrides`로 2.1.3의 값으로 한다(1.의 공식 테마 API).
5. 직접 만든 부분의 색(`success`·`success-container`·`placeholder`, 지도 위의 표시·원)은 직접 만든 CSS 변수에 `light-dark(라이트 값, 다크 값)`으로 정의한다(예: `--ct-success: light-dark(#166534, #86EFAC)`). M3 역할의 색을 쓰는 부분은 `--mat-sys-*`를 참조한다.
6. `color-scheme`을 바꾸면 브라우저 기본 부분(스크롤바·`<input>` 날짜 등)도 그 표시가 된다.

### 2.2 글자

#### 2.2.1 글꼴

**기기에 들어 있는 글꼴을 쓰고, 웹 폰트는 불러오지 않는다.** 웹 폰트를 외부에서 불러오면 새 외부 서비스가 되어 비용·이용 조건 확인과 ADR이 필요하기 때문(ADR 0014 주의점). 한국어·일본어는 다른 순서로 지정한다(같은 한자라도 글자 모양이 다르기 때문). 아이콘 글꼴은 예외로, npm 패키지를 앱에 포함한다(ADR 0016. 2.7).

| 표시 언어 | `font-family` |
|---|---|
| 한국어(`html[lang="ko"]`) | `"Apple SD Gothic Neo", "Malgun Gothic", "Noto Sans KR", "Noto Sans CJK KR", system-ui, sans-serif` |
| 일본어(`html[lang="ja"]`) | `"Hiragino Sans", "Hiragino Kaku Gothic ProN", "Yu Gothic UI", "Meiryo", "Noto Sans JP", "Noto Sans CJK JP", system-ui, sans-serif` |

- 구현: `mat.theme`의 `typography`에 한국어 순서를 지정하고, `html:lang(ja)` 선택자 안에서 `typography`만 지정한 `mat.theme`를 한 번 더 포함한다(부품의 CSS는 덮어쓰지 않음). 표시 언어를 바꿀 때 `<html lang>`을 바꾼다(바꾸면 페이지를 다시 불러온다. 02의 3.1).
- Android의 기본 글꼴(Noto Sans CJK)은 `sans-serif`로 선택된다.
- **시안의 글꼴**: Stitch는 기기 글꼴을 지정할 수 없으므로 시안은 **Noto Sans KR**(Android 기본과 같은 계열)로 만든다. 시안의 글꼴 확인은 "HTML의 제목·본문 `font-family`가 Noto Sans(KR)인 것"으로 한다.

#### 2.2.2 글자 크기(Material 3 타입 스케일. 기본값 그대로 쓴다)

| 타입(M3) | 크기 / 줄 높이 | 굵기 | 쓰는 곳 |
|---|---|---|---|
| `headline-small` | 24px / 32px | 400 | 화면 제목(SC-02의 공연명, 각 화면의 제목) |
| `title-large` | 22px / 28px | 400 | 헤더의 서비스명, 다이얼로그 제목 |
| `title-medium` | 16px / 24px | 500 | 절 제목("주변 역" "리뷰" 등), 카드의 공연명 |
| `body-large` | 16px / 24px | 400 | 입력란의 글자, SC-02의 본문 |
| `body-medium` | 14px / 20px | 400 | 카드의 공연장명·기간, 표의 셀, 리뷰 본문 |
| `body-small` | 12px / 16px | 400 | 출처, 원문 병기, 집계 기간, 작성일 |
| `label-large` | 14px / 20px | 500 | 버튼, 탭 |
| `label-medium` | 12px / 16px | 500 | 상태 라벨·배지·칩 |

- 본문의 최소는 12px. 12px는 보조(출처·원문·날짜·집계 기간)에만 쓴다.
- 한국어·일본어는 영문보다 글자면이 커서 굵기는 400 / 500 두 가지만 쓴다(700은 쓰지 않음).
- 숫자(가격·거리·건수·날짜)는 `font-variant-numeric: tabular-nums`로 자릿수를 맞춘다(표·목록·집계 기간).

### 2.3 모양(모서리 둥글기)

Material 3 기본 모양(`--mat-sys-corner-*`)을 그대로 쓴다.

| 부품 | 둥글기 |
|---|---|
| 버튼(채움·테두리·텍스트) | 전체를 둥글게(full) |
| 입력란(outline) | 4px(extra-small) |
| 라벨·배지·칩 | 8px(small) |
| 카드, SC-01 왼쪽 패널 안의 틀, 지도 위 버튼 | 12px(medium) |
| 아래에서 올라오는 시트(위쪽 두 모서리) | 28px(extra-large) |
| 다이얼로그 | 28px(extra-large) |
| 지도의 공연장 표시 | 원 |
| 포스터의 틀 | 8px(small) |

Stitch 디자인 시스템에는 `ROUND_TWELVE`(카드의 둥글기)를 지정한다.

### 2.4 여백·배치

- **여백은 4px의 배수**: 4 / 8 / 12 / 16 / 24 / 32 / 48px. 부품 안은 8·12·16, 절과 절 사이는 24(스마트폰 표시)·32(PC 표시).
- 화면 좌우 여백: 스마트폰 표시 16px, PC 표시 24px.
- 표시 전환: CDK `Breakpoints.Handset`이 "스마트폰 표시", 그 밖이 "PC 표시"(02의 1.).
- 내용의 최대 폭: SC-02·SC-03·SC-11·SC-12·SC-13·SC-14는 1200px로 가운데에 둔다. 폼만 있는 화면(SC-04~SC-10)은 400px 1단으로 가운데에 둔다.
- SC-01의 PC 표시: 왼쪽 패널 폭은 **400px** 고정, 나머지를 지도로 한다. 패널 안은 위에 조건, 아래에 목록(02).
- 목록 카드의 배치(SC-11의 PC 표시): 한 열 폭이 280px 이상이 되는 수의 열.

#### 2.4.1 SC-02의 PC 표시 배치(사람의 지적에 대한 대응. 코멘트 6676583842의 1.)

시안(SC-02 desktop)은 오른쪽 열이 "주변 역·물품보관함"뿐이라 왼쪽 열보다 짧고 아래가 크게 비어 있었다. 02의 SC-02는 PC 2단의 내용을 "왼쪽: 기본 정보·예매·통계, 오른쪽: 주변, 그 아래에 리뷰"로 정했다. 이 범위에서(항목을 늘리지 않고, 02가 열을 정한 항목은 옮기지 않고) 다음과 같이 둔다.

| 위치 | 두는 것(위에서부터) | 02와의 대응 |
|---|---|---|
| 왼쪽 열(폭 2) | ① 기본 정보(포스터·공연명·상태·기간·공연장·장르·출연자·가격·공연 시간·상연 시간·관람 연령) ② 예매 ③ 통계·평판(평가·수상작·오픈런·인기 순위와 집계 기간) | 02 "왼쪽: 기본 정보·예매·통계" |
| 오른쪽 열(폭 1) | ① **조작 틀**(`mat-card` outlined): "즐겨찾기"/"즐겨찾기 해제"(`mat-stroked-button`. 관리자에게는 보이지 않음), 관리자만 "관리 화면에서 수정"(`mat-stroked-button`). 버튼은 틀 폭 가득 ② 주변(역 → 물품보관함) | 02는 "즐겨찾기" "관리자만" 항목을 열에 배정하지 않았다. 그것을 오른쪽 열에 둔다. 주변은 02 "오른쪽: 주변" 그대로 |
| 2단 아래(전체 폭) | 리뷰(내 리뷰·"리뷰 쓰기" → 리뷰 목록·페이지 넘김) | 02 "그 아래에 리뷰" |

- 오른쪽 열은 **`position: sticky`**로 한다(위 위치는 헤더 높이 + 24px). 왼쪽 열을 읽어 내려가도 조작과 주변이 화면에 계속 보인다. 오른쪽 열이 화면 높이보다 높을 때는 오른쪽 열 안만 스크롤할 수 있게 한다(`max-height: calc(100vh - 헤더 높이 - 48px)`, `overflow-y: auto`). 이로써 오른쪽 열 아래의 빈 곳은 "스크롤을 위한 여백"이 된다.
- 열 폭의 비는 왼쪽 2 : 오른쪽 1(바꾸지 않음).
- 조작 틀은 어떤 로그인 상태에서도 버튼이 적어도 하나 있다(비로그인·일반 사용자: "즐겨찾기", 관리자: "관리 화면에서 수정").
- 스마트폰 표시는 02의 순서(기본 정보 → 통계 → 예매 → 주변 → 리뷰). "즐겨찾기"(관리자는 "관리 화면에서 수정")는 기본 정보의 공연명·상태 아래에 둔다(시안 SC-02 mobile대로).
- 예매·통계를 오른쪽 열로 옮기면 오른쪽 빈 곳이 더 줄지만, 02의 "왼쪽: 기본 정보·예매·통계"를 바꾸게 되므로 이 문서에서는 하지 않는다(바꾼다면 architect가 02를 고친다).

### 2.5 그림자·경계선

- 카드는 **테두리 카드**(`mat-card`의 `appearance="outlined"`, 경계선 `outline`). 그림자는 쓰지 않는다.
- 그림자(M3 elevation)를 쓰는 것은 지도 위에 겹치는 부품(지도 위 버튼, 스마트폰 표시의 아래 시트, 메뉴, 다이얼로그)뿐. Angular Material 기본 그림자 그대로.

### 2.6 누를 수 있는 요소의 크기

- 누를 수 있는 요소는 가로세로 **44px 이상**. Angular Material 밀도 0(ADR 0014 결정 3)에서는 버튼·아이콘 버튼·체크박스·라디오의 누르는 범위가 48px, 입력란 높이가 56px라서 만족한다.
- **`mat-button-toggle`**(언어 전환·SC-01의 장소 방법·SC-03의 기간)은 밀도 0에서도 높이 40px라 44px에 못 미친다. 테마에서 공식 테마 API `mat.button-toggle-overrides`의 `height`를 **48px**로 한다. 공식 테마 API이므로 ADR 0014 결정 2에 어긋나지 않는다(1.의 해석. 사람의 결정, 코멘트 6676583842의 4.). 구현에서 높이가 48px가 되는지 확인한다.
- 직접 만드는 부분도 44px 이상으로 한다: 글자 링크(SC-04의 "회원가입" 등. 줄 높이를 44px로), 지도의 공연장 표시(보이는 크기 32px, 누르는 범위 44px), 카드 전체(누르면 SC-02), 페이지 넘김.
- 누를 수 있는 요소 사이는 8px 이상 띄운다.

### 2.7 아이콘

- `mat-icon`으로 Material Symbols **Outlined**를 쓴다. 글꼴은 npm 패키지 `@material-symbols/font-400`의 `outlined.css`만 포함하고 Google Fonts에서 불러오지 않는다(ADR 0016 결정 1·2). 앱 시작 때 `MatIconRegistry.setDefaultFontSetClass('material-symbols-outlined', 'mat-ligature-font')`를 호출하고 `<mat-icon>이름</mat-icon>`으로 쓴다(ADR 0016 결정 3).
- 쓰는 것은 의미가 정해진 것뿐:

| 의미 | 이름 | 채움(FILL 축) |
|---|---|---|
| 메뉴 | `menu` | 0 |
| 평가의 별(표시) | `star` | 1(채움) |
| DL-01의 별(1~5 선택) | `star` | 고른 별까지 1, 그 뒤는 0 |
| 외부 링크 | `open_in_new` | 0 |
| 현재 위치 | `my_location` | 0 |
| 조건 | `tune` | 0 |
| 닫기 | `close` | 0 |
| 역 | `train` | 0 |
| 물품보관함 | `lock` | 0 |
| 즐겨찾기(미등록) | `favorite` | 0(테두리만) |
| 즐겨찾기(등록됨) | `favorite` | 1(채움) |
| 다크 표시로 전환(지금 라이트 표시) | `dark_mode` | 0 |
| 라이트 표시로 전환(지금 다크 표시) | `light_mode` | 0 |

- **채움 전환(FILL 축. ADR 0016 결정 6)**: `favorite_border`는 쓰지 않는다(ADR 0016 확인에서 패키지 이름 목록에 없었다). 채움은 직접 만든 클래스(예: `.ct-icon-filled { font-variation-settings: 'FILL' 1; }`)를 `<mat-icon>` 요소에 붙여 전환한다. Angular Material 부품의 클래스(`.mat-icon` 등)에는 CSS를 쓰지 않는다(1.의 해석).
- 아이콘 이름 확인: ADR 0016의 확인(2026-11-06, 패키지 `index.d.ts`)에서 `dark_mode`·`light_mode`는 있고 `favorite_border`는 없다는 것을 알았다. 다른 이름(`menu`·`star`·`open_in_new`·`my_location`·`tune`·`close`·`train`·`lock`·`favorite`)은 이번 문서 갱신에서 목록을 열어 확인하지 못했다. frontend-dev가 의존성을 넣을 때 `index.d.ts`로 확인하고, 없는 것이 있으면 ui-designer에게 보고한다.
- 아이콘만 있는 버튼에는 `aria-label`을 붙인다. 아이콘과 글자를 함께 쓰는 버튼에서는 글자를 반드시 보여 준다.
- 표시 크기는 `mat-icon` 기본(24px). 패키지는 optical size 48로 고정되어 있다(ADR 0016 주의점). 구현에서 겉모습이 신경 쓰이면 ui-designer에게 보고한다.

## 3. Angular Material·CDK 부품과의 대응

02의 요소를 어떤 부품으로 만들지의 대응. Material에 없는 부품은 CDK로 직접 만든다(ADR 0014 결정 5).

| 02의 요소 | 부품 | 겉모습 규칙 |
|---|---|---|
| CM-01 헤더 | `mat-toolbar`. 스마트폰 표시의 메뉴는 `mat-sidenav`(오른쪽에서 열림) | 배경 `surface`, 아래에 경계선 `outline`. 높이 PC 64px·스마트폰 56px. 선택된 화면의 글자를 `primary` |
| 표시 언어 전환 | `mat-button-toggle-group`("한국어" "日本語") | 언어 이름은 각 언어로 쓴다(전환해도 바꾸지 않음). **선택된 쪽은 배경 `primary-container`·글자 `primary`**(옅은 남색. `primary` 채움은 쓰지 않는다. 사람의 결정, 코멘트 6676583842의 3.). 높이 48px(2.6) |
| 라이트 표시·다크 표시 전환(F-36) | `mat-icon-button` | 아이콘은 누른 뒤의 표시(라이트 표시일 때 `dark_mode`, 다크 표시일 때 `light_mode`). `aria-label`도 누른 뒤의 표시(한국어 "다크 표시로 전환"/"라이트 표시로 전환", 일본어 "ダーク表示に切り替える"/"ライト表示に切り替える". 02의 3.1). 색은 `on-surface-variant` |
| CM-02 푸터 | 직접 만듦(`<footer>`) | 배경 `surface-container-low`, 글자 `body-small`·`on-surface-variant` |
| CM-03 알림 | `MatSnackBar` | 화면 아래 가운데. 기본 겉모습 |
| 주요 조작(검색·로그인·저장·등록) | `mat-flat-button`(채움) | 한 화면에 하나를 기준으로 한다 |
| 보조 조작(조건 지우기·취소·즐겨찾기·리뷰 쓰기) | `mat-stroked-button`(테두리) | — |
| 링크 조작(회원가입·비밀번호 찾기·페이지 안 이동) | `mat-button`(텍스트) 또는 `<a>` | 글자 `primary` |
| 삭제·탈퇴 | `mat-flat-button`(색 `error`, 글자 `on-error`) | DL-02·DL-03의 확정 버튼만 |
| 입력란 | `mat-form-field`(`appearance="outline"`, `subscriptSizing="dynamic"`) | 오류는 입력란 아래에 `mat-error`(02의 3.3). 긴 문구가 잘리지 않도록 `dynamic`으로 한다 |
| 기간(시작일·종료일) | `mat-date-range-input` + `mat-date-range-picker` | 날짜 형식은 `YYYY-MM-DD` |
| 구·장르(여러 개), 표시 상태 | `mat-select`(구·장르는 `multiple`) | — |
| 장소 방법(구·지도 범위·공연장) | `mat-button-toggle-group`(하나만 선택) | 선택된 색·높이는 언어 전환과 같다. 일본어에서 들어가지 않으면 줄바꿈(6.) |
| 공연 상태(공연 중·공연 예정), "종료된 공연도 포함" | `mat-checkbox` | — |
| 공연장 후보 | `mat-autocomplete` | — |
| SC-03의 기간(일간·주간·월간) | `mat-button-toggle-group` | 선택된 색·높이는 언어 전환과 같다 |
| SC-03의 장르 | PC: `mat-chip-listbox`(하나 선택), 스마트폰: `mat-select`(02) | — |
| 탭(SC-03, SC-01의 현재 위치 주변) | `mat-tab-group` | — |
| 공연 카드(02의 3.7) | `mat-card`(`outlined`) 전체를 `<a>`로 | 4.4 |
| 상태 라벨·수상작 배지·"원문"·"전국 기준" | 직접 만듦(누를 수 없는 라벨. `<span>`) | 모서리 8px, `label-medium`. 누를 수 없으므로 `mat-chip`은 쓰지 않는다 |
| 평가(★평균(건수)) | `mat-icon`(`star`, FILL 1, 색 `tertiary`) + 글자 | "★4.3(12)". 0건은 "아직 리뷰가 없습니다"를 `body-small`로 |
| 인기 순위(SC-02)·집계 기간(SC-02·SC-03) | 직접 만듦(글자) | 4.6 |
| 페이지 넘김 | `mat-paginator`(건수 선택은 보이지 않음. 20건 고정) | — |
| 관리 공연 목록(PC) | `mat-table` | 행 전체를 누를 수 있다. 머리 행 배경 `surface-container` |
| 스마트폰 표시의 조건 | `MatDialog`(전체 화면) | — |
| 스마트폰 표시의 아래 시트(SC-01) | **CDK로 직접 만듦**(지도를 보면서 조작하므로 모달인 `MatBottomSheet`는 쓰지 않는다) | 위 모서리 28px, 위에 "지도 / 목록" 전환 |
| 다이얼로그 DL-01~DL-04 | `MatDialog` | 모서리 28px. 버튼은 오른쪽 아래에 "취소"(텍스트) → 확정(채움) 순서 |
| DL-01의 별(1~5) | 직접 만듦(`mat-icon-button` 5개. 라디오 그룹으로 읽어 준다) | 누르는 범위 48px. 고른 별까지 FILL 1(2.7) |
| 로딩 중(버튼) | `mat-progress-spinner`(20px)를 버튼 안에 | 누를 수 없게 한다(02의 3.6) |
| 로딩 중(목록) | 직접 만든 스켈레톤(색 `placeholder`) | 카드와 같은 크기의 회색 틀 |
| 가로 막대(SC-03의 장르별 트렌드) | CSS로 직접 만듦(02) | 막대 색 `primary`, 바탕 `surface-container`. 비율 숫자를 반드시 글자로 보여 준다 |
| 지도 | `@angular/google-maps`(ADR 0006) | 4.5 |
| reCAPTCHA(SC-09) | Google 부품(ADR 0011) | 겉모습은 바꾸지 않는다 |

- `mat-button-toggle`의 선택된 색과 높이는 테마에서 한 번만 지정한다: `mat.button-toggle-overrides`의 `height: 48px`, `selected-state-background-color: var(--mat-sys-primary-container)`, `selected-state-text-color: var(--mat-sys-primary)`(토큰 이름은 구현 때 공식 목록으로 확인한다). M3 역할을 참조하므로 다크 표시에서도 자동으로 2.1.3의 색이 된다.

## 4. 공통 부품·공통 표시의 겉모습

### 4.1 CM-01 헤더

- PC 표시: 왼쪽에 서비스명 "CatTicket", 그 오른쪽에 "지도" "인기·트렌드"(로그인 상태에 따라 "즐겨찾기" "관리"), 오른쪽 끝에 로그인 상태에 따른 버튼("로그인" "회원가입" / "계정(닉네임)" "로그아웃"), 언어 전환, 라이트 표시·다크 표시 전환(이 순서).
- 스마트폰 표시: 왼쪽에 서비스명, 오른쪽에 언어 전환 → 라이트 표시·다크 표시 전환 → 메뉴 버튼. 나머지 항목은 메뉴 안(02의 3.1).
- 관리자에게도 같은 위치에 전환을 보여 준다(P-16).

### 4.2 CM-02 푸터

- 출처를 2줄로 표시(KOPIS, 서울교통공사). 문구는 한국어 원문 그대로(02의 3.2).
- SC-01의 스마트폰 표시에서는 지도 왼쪽 아래에 "출처" 글자 링크(누르는 범위 44px)를 두고, 누르면 같은 내용을 다이얼로그로 보여 준다.

### 4.3 상태 표시(02의 3.6)

| 상태 | 겉모습 |
|---|---|
| 로딩 중(목록) | 카드와 같은 모양의 회색 틀(`placeholder`) 3개. 움직임(깜박임)은 넣지 않는다(움직임을 줄이는 설정을 쓰는 이용자를 배려하고, 만드는 수고도 줄인다) |
| 로딩 중(버튼) | 버튼 글자 대신 스피너(20px), 버튼은 누를 수 없음 |
| 로딩 중(지도) | 지도 영역을 `placeholder` 색으로 칠한다 |
| 빈 결과 | 목록 자리 가운데에 02의 각 화면 문구를 `body-medium`·`on-surface-variant`로. 링크(SC-11의 SC-01 링크 등 02에 있는 것만)는 그 아래에 `mat-button` |
| 오류(통신·서버) | 목록 자리 가운데에 "읽어 오지 못했습니다"(`body-medium`)와 "다시 불러오기"(`mat-stroked-button`)(02의 3.6) |
| 입력 오류 | 입력란 아래에 `mat-error`(색 `error`) |
| 화면 위 안내(SC-04의 결과·잠금 등) | 입력란 위에 배경 `surface-container`·글자 `on-surface`의 틀(모서리 12px). 오류 안내는 왼쪽에 4px `error` 선을 붙인다(색에만 기대지 않고 문구로 전한다) |

라이트 표시·다크 표시 어느 쪽이든 같은 토큰 이름을 쓴다(2.1.3).

### 4.4 공연 카드(02의 3.7)

- 스마트폰 표시·SC-01의 왼쪽 패널: 가로형(왼쪽에 포스터 72×96px, 오른쪽에 글자). SC-11의 PC 표시: 같은 모양을 여러 열로 배치.
- 글자 순서: 공연명(`title-medium`. 2줄까지, 넘으면 "…") → 공연장명(`body-medium`·`on-surface-variant`) → 공연 기간 → 장르·상태 라벨·수상작 배지를 한 줄에 → 평가.
- 일본어 표시의 공연명·공연장명은 번역(크게) 아래에 원문(`body-small`·`on-surface-variant`)을 한 줄로(02의 3.4).
- 포스터가 없으면 `placeholder` 틀.

### 4.5 지도(SC-01만)

- 공연장 표시: 원(보이는 크기 32px, 누르는 범위 44px), 배경 `primary`, 안에 공연 수(`on-primary`, `label-medium`). 선택된 표시는 40px로 하고 `surface` 색의 테두리(2px)를 붙인다.
- 현재 위치 표시: `primary` 원에 `surface` 색의 테두리. 반경 500m 원: 선 `primary`, 채움 `primary`의 12%.
- 역 표시: `on-surface` 원에 `surface` 색의 `train` 아이콘.
- 지도 위 버튼("근처 보기" "이 범위로 검색"): `mat-flat-button`을 지도 위에 겹치고 그림자를 붙인다. 색은 앱의 표시(라이트·다크)를 따른다.
- Google Maps 지도의 겉모습(스타일)은 바꾸지 않는다. 어두운 표시는 02의 3.8대로 `colorScheme`(`LIGHT` / `DARK`)만으로 한다.
- **표시·원의 색은 지도의 표시에 맞춘다**: 지도가 다크 표시로 만들어졌을 때는 2.1.3의 다크 값, 라이트로 만들어졌을 때는 2.1.1의 라이트 값을 쓴다. 보통은 앱의 표시와 같다(02의 3.8). 02 3.8의 "구현에서 확인할 것"에서 map ID가 없는 지도가 어두워지지 않아 **지도만 라이트 표시 그대로**로 했을 때는, 앱이 다크 표시여도 표시·원은 **라이트 값**으로 한다(밝은 지도 위에서 밝은 `primary` `#A5B4FC` 표시는 구분하기 어렵기 때문). 지도 위 버튼은 앱의 표시 그대로.
- 지도 상한(02의 6.1)·불러오기 전 영역의 색은 앱 표시의 토큰(`surface-container`·`placeholder`).

### 4.6 인기 순위·집계 기간(SC-02·SC-03. P-12·P-17·P-18)

- **집계 기간의 형식(P-18)**: 라벨 "집계 기간"(일본어 "集計期間") 뒤에 `YYYY-MM-DD`. 일간은 하루(예: `2026-11-04`), 주간·월간은 첫날~끝날(예: `2026-10-26~2026-11-01`). 두 언어 모두 같은 형식. **날짜와 날짜의 구분은 반각 `~`(U+007E)이고, 앞뒤에 공백을 넣지 않는다**(02 P-18의 예에 맞춘다. 집계 기간·장르별 트렌드의 집계 기간·공연 기간 표시에서 같다. 전각 `〜`·`～`는 쓰지 않는다). `body-small`·`on-surface-variant`·`tabular-nums`. 날짜 중간에서 줄바꿈하지 않는다(날짜마다 `white-space: nowrap`. 줄바꿈한다면 구분 기호 뒤).
- **SC-02의 인기 순위(P-02·P-12)**: 순위 하나를 한 줄로: "주간·전체 3위"(`body-medium`·`tertiary`) 뒤에 집계 기간(위 형식). 최대 6줄. 칩·틀로 만들지 않는다(누를 수 없으므로).
- **SC-03의 집계 기간**: 기간·장르 선택 아래, 목록 위에 한 줄로. 인기 급상승은 "현재 기간의 집계 기간"과 "비교할 기간의 집계 기간"을 두 줄로(02).
- **인기 급상승에서 비교할 기간의 순위가 없음(P-17)**: 현재 기간의 집계 기간은 평소와 같은 위치에 보여 주고, 목록 자리에 02의 문구를 4.3의 "빈 결과"와 같은 겉모습(가운데, `body-medium`·`on-surface-variant`)으로 보여 준다. 버튼은 두지 않는다(02에 없음).

## 5. 시안이 없는 화면·상태의 겉모습

시안을 만든 것은 9.의 7개뿐(Issue #15 코멘트 6651238358). 다른 화면·상태는 시안의 화면과 2~4.의 규칙으로 만든다. 다크 표시는 모든 화면에서 시안을 만들지 않고 2.1.3의 값으로 만든다.

| 화면·상태 | 대표로 삼는 시안 | 겉모습 규칙 |
|---|---|---|
| SC-05~SC-10(폼 화면) | SC-04(mobile) | 1단 폼(최대 400px). 위에서부터: 화면 제목(`headline-small`) → 안내 문장(있으면) → 입력란 → 주요 버튼(폭 가득, `mat-flat-button`) → 글자 링크. PC 표시에서도 같은 모양으로 가운데에 둔다 |
| SC-06·SC-10(링크를 연 뒤의 결과) | SC-04 | 확인 중에는 스피너(40px)를 가운데에. 결과는 4.3의 "화면 위 안내" 틀에 문구, 그 아래에 주요 버튼 |
| SC-09의 reCAPTCHA | SC-04 | 입력란과 "재인증 메일 보내기" 사이에 둔다 |
| SC-11 즐겨찾기 | SC-01(카드) | 4.4 카드의 오른쪽 아래에 "해제"(`mat-button`). PC는 여러 열(2.4) |
| SC-12 계정 설정 | SC-04(폼) | "닉네임" "비밀번호" "탈퇴" 세 틀(`mat-card` outlined)을 세로로. "탈퇴하기"는 `mat-stroked-button`(색 `error`), DL-03의 확정만 채움 `error` |
| SC-02의 PC 표시 오른쪽 열 | SC-02(desktop) | 2.4.1 |
| SC-03의 PC 표시 | SC-03(mobile) | 2.4(최대 1200px)·3.(장르는 칩)·4.6 |
| SC-13의 스마트폰 표시 | SC-13(desktop) | 표의 한 행을 카드 한 장으로(같은 항목. 02) |
| SC-14 관리: 공연 수정 | SC-13(desktop) | 항목마다 "항목명·자동 수집 값·현재 값·수정란·수정 취소"를 PC는 가로 한 줄, 스마트폰은 세로로(02). 수정한 항목은 현재 값 옆에 "수정됨" 라벨(`primary-container`). "저장"은 화면 아래에 고정하지 않는다(페이지 끝에 둔다) |
| SC-99 오류 | — | 가운데에 문구(`headline-small`)와 SC-01 링크(`mat-flat-button`). 일러스트·이미지는 두지 않는다 |
| DL-01~DL-04 | — | 3.의 "다이얼로그" 행 |
| 로딩 중·빈 결과·오류(모든 화면) | — | 4.3 |
| 지도를 표시할 수 없음(02의 6.1) | SC-01 | 지도 영역을 `surface-container`로 칠하고 가운데에 02의 문구. 스마트폰 표시에서는 아래 시트를 연 상태 |
| 메일 상한(02의 6.2) | SC-04 | 4.3의 "화면 위 안내"(오류 선 포함) |
| 번역이 없음(02의 3.4, P-06) | — | 원문 오른쪽에 "원문 / 原文" 라벨(`surface-container` 배경, `label-medium`) |
| "전국 기준" "서울 기준"(SC-03) | SC-03 | 탭 제목 바로 아래에 `primary-container` 라벨. 설명 문장은 `body-small` |
| SC-03의 인기 급상승·장르별 트렌드 | SC-03(인기 순위 탭) | 같은 기간·장르 선택 방식. "NEW"는 `tertiary-container` 배경·`tertiary` 글자, "▲3"은 `success` 글자. 오른 공연이 0건일 때(02, 01의 F-33 ⑪)의 "-"는 `on-surface-variant` 글자·배경 없음("NEW"·"▲3"과 같은 자리·같은 글자 크기. 색과 배경 유무로 구별한다). `aria-label`은 02대로 "순위 상승 없음"(일본어 "順位の上昇なし") |
| SC-03 인기 급상승에서 비교할 기간의 순위가 없음(P-17) | SC-03 | 4.6 |
| 다크 표시(모든 화면·F-36) | — | 2.1.3~2.1.5. 지도 위 부분은 4.5 |

## 6. 일본어 표시의 주의(시안은 한국어로 만들었다)

일본어는 같은 뜻이라도 한국어보다 글자가 많아질 때가 있다. 구현에서는 일본어로도 다음을 확인한다.

| 위치 | 한국어 → 일본어 예 | 규칙 |
|---|---|---|
| 헤더(PC) | 인기·트렌드 → 人気・トレンド, 즐겨찾기 → お気に入り, 로그아웃 → ログアウト | 항목을 줄바꿈하지 않는다(`white-space: nowrap`). PC 표시의 가장 좁은 폭(960px)에서 일본어·관리자·일반 사용자 세 가지를 확인한다(언어 전환·라이트/다크 전환 포함) |
| 헤더(스마트폰) | 언어 이름은 전환해도 바뀌지 않는다 | 서비스명·언어 전환(높이 48px)·라이트/다크 전환·메뉴가 폭 360px에서 한 줄에 들어가는지 확인한다. 들어가지 않으면 서비스명을 `title-medium`으로 한다 |
| SC-01의 장소 방법 | 구 / 지도 범위 / 공연장 → 区 / 地図の範囲 / 会場 | 400px 패널에 들어가지 않으면 2줄로 줄바꿈 |
| 버튼 | 조건 지우기 → 条件をクリア, 재인증 메일 보내기 → 再認証のメールを送る, 관리 화면에서 수정 → 管理画面で修正 | 버튼 폭을 고정하지 않는다. 스마트폰 표시 폼의 버튼·SC-02 PC 조작 틀의 버튼은 폭 가득 |
| SC-04의 잠금 안내 | 해제 방법 두 링크가 길다 | 링크는 한 줄에 하나씩 세로로 놓는다 |
| 카드 | 공연명 번역 + 원문 2단(02의 3.4) | 공연명은 2줄까지, 원문은 1줄까지(넘으면 "…"). 카드 높이를 고정하지 않는다 |
| 표(SC-13) | 관리자 수정 → 管理者の修正 | 머리글은 줄바꿈해도 된다. 공연 ID·기간 열은 줄바꿈하지 않는다 |
| 입력 오류·안내 문장 | 일본어가 더 길다 | `subscriptSizing="dynamic"`으로 여러 줄을 허용한다 |
| SC-03 인기 급상승의 P-17 문구 | "比べる期間（…）の順位データがないため、人気急上昇を表示できません" | 여러 줄을 허용한다. 집계 기간의 날짜 중간에서는 줄바꿈하지 않는다(4.6) |
| 날짜·집계 기간 | — | 두 언어 모두 `YYYY-MM-DD`(02 문구의 예, P-18) |

## 7. 접근성

ADR 0014 결정 6대로 요건으로서의 달성 기준(WCAG 등급)은 두지 않는다. 다음은 **이 문서의 겉모습 규칙**으로 지키고, 시안에서도 지킨다.

- 글자와 배경의 대비는 4.5:1 이상. 라이트 표시(2.1.2)와 다크 표시(2.1.4) 모두.
- 누를 수 있는 요소는 44px 이상(2.6).
- 색만으로 의미를 전하지 않는다(상태·오류·순위 변화는 글자로도 보여 준다).
- 포커스가 잘 안 보이면 `mat.strong-focus-indicators()`를 쓴다(ADR 0014 결정 6). 다크 표시에서도 보이는지 확인한다.
- 이미지(포스터)에는 공연명을 대체 텍스트로 한다.
- 아이콘만 있는 버튼(메뉴·라이트/다크 전환 등)에는 `aria-label`(2.7, 3.).

## 8. 미결 사항

미결 사항은 없다. U-1~U-3은 해결했다.

| No. | 내용 | 결과 | 결정의 출처 |
|---|---|---|---|
| U-1 | Material Symbols 아이콘 글꼴의 배포 방법 | **해결됨**: npm 패키지 `@material-symbols/font-400`의 Outlined를 포함하고 Google Fonts에서 불러오지 않는다. 2.7에 반영 | 사람의 결정: Issue #15 코멘트 6676583842의 5., ADR 0016 채택: 코멘트 6676943465 |
| U-2 | 색·레이아웃의 채택 여부 | **해결됨**: 시안 7개 채택, 색(primary `#4338CA` 등)·SC-01 배치 채택. 언어 전환 색(3.), SC-02 PC 오른쪽 열(2.4.1), 다크 표시(2.1.3~2.1.5)를 반영 | 사람의 결정: Issue #15 코멘트 6676583842의 1.~3.·6. |
| U-3 | `mat.button-toggle-overrides`로 높이를 48px로 하는 것이 ADR 0014 결정 2의 범위인가 | **해결됨**: 공식 테마 API는 "부품의 CSS를 직접 덮어쓰기"에 해당하지 않는다. 금지는 부품의 클래스에 CSS를 직접 쓰는 것. 48px대로(1.·2.6) | 사람의 결정: Issue #15 코멘트 6676583842의 4. |

## 9. 화면 ID와 시안·절의 대응표

02의 모든 화면 ID. 시안은 [mockups/](mockups/README.md)(파일 목록과 Stitch 화면 ID는 README). 시안의 겉모습이 이 문서와 다른 곳(굵기·모서리 둥글기·누르는 크기 등)은 README의 "07과의 차이"에 썼다. 구현은 이 문서를 따른다(사람이 승인. 코멘트 6676583842). 다크 표시는 모든 화면에서 2.1.3~2.1.5.

| 화면 ID | 화면명 | 시안(스마트폰 표시) | 시안(PC 표시) | 시안이 없을 때의 대응 |
|---|---|---|---|---|
| SC-01 | 지도·검색 | [SC-01-mobile.png](mockups/SC-01/SC-01-mobile.png) | [SC-01-desktop.png](mockups/SC-01/SC-01-desktop.png) | 상태(로딩 중·빈 결과·오류·위치 정보·지도 상한)는 4.3·5., 지도 위 부분은 4.5 |
| SC-02 | 공연 상세 | [SC-02-mobile.png](mockups/SC-02/SC-02-mobile.png) | [SC-02-desktop.png](mockups/SC-02/SC-02-desktop.png) | PC 오른쪽 열은 2.4.1, 인기 순위는 4.6, DL-01·DL-02는 3., 상태는 4.3 |
| SC-03 | 인기·트렌드 | [SC-03-mobile.png](mockups/SC-03/SC-03-mobile.png) | — | PC 표시는 2.4·3.(장르는 칩). 집계 기간·P-17은 4.6. 다른 탭·상태는 5.·4.3 |
| SC-04 | 로그인 | [SC-04-mobile.png](mockups/SC-04/SC-04-mobile.png) | — | PC 표시는 2.4(같은 모양으로 가운데). 결과 표시는 4.3 |
| SC-05 | 회원가입 | — | — | 5.(SC-04가 대표) |
| SC-06 | 메일 인증 완료 | — | — | 5. |
| SC-07 | 비밀번호 재설정 신청 | — | — | 5.(SC-04가 대표) |
| SC-08 | 새 비밀번호 설정 | — | — | 5.(SC-04가 대표) |
| SC-09 | 잠금 해제 신청 | — | — | 5.(SC-04가 대표) |
| SC-10 | 잠금 해제 완료 | — | — | 5. |
| SC-11 | 즐겨찾기 | — | — | 5.·4.4 |
| SC-12 | 계정 설정 | — | — | 5.(SC-04가 대표), DL-03은 3. |
| SC-13 | 관리: 공연 목록 | — | [SC-13-desktop.png](mockups/SC-13/SC-13-desktop.png) | 스마트폰 표시는 5. |
| SC-14 | 관리: 공연 수정 | — | — | 5., DL-02·DL-04는 3. |
| SC-99 | 오류 | — | — | 5. |
| CM-01~CM-03 | 공통 부품 | 각 시안에 포함 | 각 시안에 포함 | 4.1~4.2, 3.(라이트/다크 전환은 3.·4.1) |
