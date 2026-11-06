# 07. UI デザインガイド

- 状態: 基本設計（レビュー待ち。試作は人間の確認待ち）
- 担当: ui-designer
- 最終更新: 2026-11-06
- 前提: [02. 画面設計書](02-screens.md)（P-01〜P-15 は採用済み。Issue #15 コメント 6651238358）、[01. 要件定義書](01-requirements.md)（N-01, N-02）、ADR 0014（Angular Material + CDK）、ADR 0015（Google Stitch）
- 試作: [mockups/](mockups/README.md)

## 1. 位置づけ

- この文書は**見た目**（色・文字・形・余白・部品の選び方）を決める。画面の項目・操作・状態・遷移は 02 が正で、この文書と試作は 02 に従う（ADR 0015 決定3）。
- **トークンの正はこの文書**。Stitch のデザインシステムは色・角丸しか保存しない（stitch-workflow スキル）ため、試作の見た目と食い違うときはこの文書に従う。
- 実装は Angular Material（Material 3）と CDK で行う（ADR 0014）。トークンは `mat.theme` の Sass mixin で1か所にまとめ、部品の CSS を直接上書きしない（ADR 0014 決定2）。試作の HTML（Tailwind）は実装に使わない。
- 試作は見た目の参考で、Stitch の生成のため細部（並び・余白・サンプルの値）が 02・この文書とずれることがある。ずれたときは 02 とこの文書が正。

## 2. デザイントークン

### 2.1 色

色の値は Material 3 のテーマの**元になる色**（seed）と、自作の部分（地図の印・横棒・スケルトンなど）で使う値。Angular Material のパレットは、実装のときに公式の `ng generate @angular/material:theme-color` で下の primary・tertiary・error・neutral から作る。作った後、2.1.2 のコントラストを満たすことを実装で確かめる。

#### 2.1.1 色の一覧

| トークン | HEX | Material 3 の役割 | 用途 |
|---|---|---|---|
| `primary` | `#4338CA` | primary | 主なボタン（検索・ログイン・保存）、リンク、選択中のタブ・チップ、地図の会場の印、「公演予定」のラベルの文字 |
| `on-primary` | `#FFFFFF` | on-primary | primary の上の文字・アイコン |
| `primary-container` | `#EEF2FF` | primary-container | 「公演予定」のラベルの背景、選択中の行・項目の背景 |
| `tertiary` | `#B45309` | tertiary | 評価の星、受賞作バッジの文字・枠、人気順位の表示 |
| `tertiary-container` | `#FFFBEB` | tertiary-container | 受賞作バッジの背景 |
| `success` | `#166534` | （自作。M3 に役割がない） | 「公演中」のラベルの文字 |
| `success-container` | `#F0FDF4` | （自作） | 「公演中」のラベルの背景 |
| `error` | `#B91C1C` | error | 入力の誤り、エラーの文言、削除のボタン |
| `background` / `surface` | `#FFFFFF` | surface | 画面の背景、カード |
| `surface-container-low` | `#F9FAFB` | surface-container-low | SC-01 の左パネル・下のシート、フッターの背景 |
| `surface-container` | `#F3F4F6` | surface-container | 「終了」のラベルの背景、表の見出し行 |
| `placeholder` | `#E5E7EB` | （自作） | 画像（ポスター）がないときの枠、読み込み中のスケルトン、地図の読み込み前の枠 |
| `on-surface` | `#1F2937` | on-surface | 本文・見出しの文字 |
| `on-surface-variant` | `#4B5563` | on-surface-variant | 補足の文字（会場名・期間・件数・原文の併記・出典）、「終了」のラベルの文字 |
| `outline` | `#D1D5DB` | outline-variant | カード・入力欄・表の境界線（文字には使わない） |

- 色だけで意味を伝えない。公演の状態は必ず文字（「公演中」「公演予定」「終了」）で示し、色は補助にする。
- ダークモードは作らない（02・01 に要件がない）。

#### 2.1.2 コントラスト（文字と背景）

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
| `error` `#B91C1C` | `#FFFFFF` | 6.4:1 |

### 2.2 文字

#### 2.2.1 フォント

**端末に入っているフォントを使い、Web フォントは読み込まない。** Web フォントを外部から読み込むと新しい外部サービスになり、費用・利用条件の確認と ADR が要る（ADR 0014 注意点）ため。韓国語・日本語で別の並びにする（同じ漢字でも字形が違うため）。

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

- 本文の最小は 12px。12px は補足（出典・原文・日付）だけに使う。
- 韓国語・日本語は英字より字面が大きいので、太さは 400 / 500 の2つだけ使う（700 は使わない）。
- 数字（価格・距離・件数）は `font-variant-numeric: tabular-nums` で桁をそろえる（表・一覧）。

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
- SC-02 の PC 表示: 2段組みの幅は左 2 : 右 1。
- 一覧のカードの並び（SC-11 の PC 表示）: 1列の幅が 280px 以上になる数の列。

### 2.5 影・境界線

- カードは**枠のカード**（`mat-card` の `appearance="outlined"`、境界線 `outline`）。影は使わない。
- 影（M3 の elevation）を使うのは、地図の上に重なる部品（地図の上のボタン、スマホ表示の下のシート、メニュー、ダイアログ）だけ。Angular Material の既定の影のまま。

### 2.6 押せる要素の大きさ

- 押せる要素は縦横 **44px 以上**。Angular Material の密度 0（ADR 0014 決定3）では、ボタン・アイコンボタン・チェックボックス・ラジオの押せる範囲は 48px、入力欄の高さは 56px で満たす。
- **例外の対応**: `mat-button-toggle`（言語の切り替え・SC-01 の場所の方法・SC-03 の期間）は密度 0 でも高さ 40px で、44px に足りない。テーマで公式のトークンの API `mat.button-toggle-overrides((height: 48px))` を使って 48px にする（部品の CSS を直接書き換えるのではなく、Angular Material が用意したテーマの API。ADR 0014 決定2 の範囲）。実装で高さが 48px になることを確かめる。
- 自作の部分も 44px 以上にする: 文字のリンク（SC-04 の「회원가입」など。行の高さを 44px にする）、地図の会場の印（見た目 32px、押せる範囲 44px）、カード全体（押すと SC-02）、ページ送り。
- 押せる要素どうしの間は 8px 以上あける。

### 2.7 アイコン

- `mat-icon` で Material Symbols（Outlined）を使う。使うのは意味が決まったものだけ: メニュー（`menu`）、評価の星（`star`）、外部のリンク（`open_in_new`）、現在地（`my_location`）、条件（`tune`）、閉じる（`close`）、駅（`train`）、コインロッカー（`lock`）、お気に入り（`favorite` / `favorite_border`）。
- アイコンだけのボタンには `aria-label` を付ける。アイコンと文字を並べるボタンでは文字を必ず出す。
- アイコンのフォントの配り方（Google Fonts から読み込むか、ファイルを同梱するか）は未決（8. 未決事項）。

## 3. Angular Material・CDK の部品との対応

02 の要素を、どの部品で作るかの対応。Material にない部品は CDK で自作する（ADR 0014 決定5）。

| 02 の要素 | 部品 | 見た目の決まり |
|---|---|---|
| CM-01 ヘッダー | `mat-toolbar`。スマホ表示のメニューは `mat-sidenav`（右から開く） | 背景 `surface`、下に境界線 `outline`。高さ PC 64px・スマホ 56px。選択中の画面の文字を `primary` |
| 表示言語の切り替え | `mat-button-toggle-group`（「한국어」「日本語」） | 言語名は各言語で書く（切り替えても変えない） |
| CM-02 フッター | 自作（`<footer>`） | 背景 `surface-container-low`、文字 `body-small`・`on-surface-variant` |
| CM-03 通知 | `MatSnackBar` | 画面の下の中央。既定の見た目 |
| 主な操作（検索・ログイン・保存・登録） | `mat-flat-button`（塗り） | 1画面に1つを目安にする |
| 補助の操作（条件をクリア・キャンセル・お気に入り・レビューを書く） | `mat-stroked-button`（枠） | — |
| リンクの操作（会員登録・パスワードを忘れた方・ページ内の移動） | `mat-button`（文字）または `<a>` | 文字 `primary` |
| 削除・退会 | `mat-flat-button`（色 `error`） | DL-02・DL-03 の確定のボタンだけ |
| 入力欄 | `mat-form-field`（`appearance="outline"`、`subscriptSizing="dynamic"`） | 誤りは欄の下に `mat-error`（02 の 3.3）。長い文言で切れないよう `dynamic` にする |
| 期間（開始日・終了日） | `mat-date-range-input` + `mat-date-range-picker` | 日付の形式は `YYYY-MM-DD` |
| 区・ジャンル（複数）、表示の状態 | `mat-select`（区・ジャンルは `multiple`） | — |
| 場所の方法（区・地図の範囲・会場） | `mat-button-toggle-group`（1つだけ選ぶ） | 日本語で入らないときは折り返す（6.） |
| 公演の状態（公演中・公演予定）、「終わった公演も含める」 | `mat-checkbox` | — |
| 会場の候補 | `mat-autocomplete` | — |
| SC-03 の期間（日次・週次・月次） | `mat-button-toggle-group` | — |
| SC-03 のジャンル | PC: `mat-chip-listbox`（1つ選ぶ）、スマホ: `mat-select`（02） | — |
| タブ（SC-03、SC-01 の現在地の周辺） | `mat-tab-group` | — |
| 公演のカード（02 の 3.7） | `mat-card`（`outlined`）全体を `<a>` にする | 4.4 |
| 状態のラベル・受賞作バッジ・「原文」・「全国基準」 | 自作（押せないラベル。`<span>`） | 角丸 8px、`label-medium`。押せないので `mat-chip` は使わない |
| 評価（★平均（件数）） | `mat-icon`（`star`、色 `tertiary`）+ 文字 | 「★4.3（12）」。0件は「아직 리뷰가 없습니다」を `body-small` |
| ページ送り | `mat-paginator`（件数の選択は出さない。20件固定） | — |
| 管理の公演一覧（PC） | `mat-table` | 行全体を押せる。見出し行の背景 `surface-container` |
| スマホ表示の条件 | `MatDialog`（全画面） | — |
| スマホ表示の下のシート（SC-01） | **CDK で自作**（地図を見ながら操作するため、モーダルの `MatBottomSheet` は使わない） | 上の角 28px、上に「지도 / 목록」の切り替え |
| ダイアログ DL-01〜DL-04 | `MatDialog` | 角 28px。ボタンは右下に「キャンセル」（文字）→ 確定（塗り）の順 |
| DL-01 の星（1〜5） | 自作（`mat-icon-button` を5つ。ラジオのグループとして読み上げる） | 押せる範囲 48px |
| 読み込み中（ボタン） | `mat-progress-spinner`（20px）をボタンの中に | 押せなくする（02 の 3.6） |
| 読み込み中（一覧） | 自作のスケルトン（色 `placeholder`） | カードと同じ大きさの灰色の枠 |
| 横棒（SC-03 のジャンル別トレンド） | CSS で自作（02） | 棒の色 `primary`、下地 `surface-container`。割合の数字を必ず文字で出す |
| 地図 | `@angular/google-maps`（ADR 0006） | 4.5 |
| reCAPTCHA（SC-09） | Google の部品（ADR 0011） | 見た目は変えない |

## 4. 共通部品・共通の表示の見た目

### 4.1 CM-01 ヘッダー

- PC 表示: 左にサービス名「CatTicket」、その右に「지도」「인기·트렌드」（ログインの状態に応じて「즐겨찾기」「관리」）、右端にログインの状態に応じたボタン（「로그인」「회원가입」/「계정（ニックネーム）」「로그아웃」）と言語の切り替え。
- スマホ表示: 左にサービス名、右に言語の切り替えとメニューのボタン。ほかの項目はメニューの中（02 の 3.1）。

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

### 4.4 公演のカード（02 の 3.7）

- スマホ表示・SC-01 の左パネル: 横長（左にポスター 72×96px、右に文字）。SC-11 の PC 表示: 同じ形を複数列に並べる。
- 文字の順: 公演名（`title-medium`。2行まで、超えたら「…」）→ 会場名（`body-medium`・`on-surface-variant`）→ 公演期間 → ジャンル・状態のラベル・受賞作バッジを1行に → 評価。
- 日本語表示の公演名・会場名は、翻訳（大きく）の下に原文（`body-small`・`on-surface-variant`）を1行で（02 の 3.4）。
- ポスターがないときは `placeholder` の枠。

### 4.5 地図（SC-01 だけ）

- 会場の印: 円（見た目 32px、押せる範囲 44px）、背景 `primary`、中に公演の数（白、`label-medium`）。選択中の印は 40px にし、白い枠を付ける。
- 現在地の印: `primary` の円に白い枠。半径 500m の円: 線 `primary`、塗り `primary` の 12%。
- 駅の印: `on-surface` の円に白の `train` アイコン。
- 地図の上のボタン（「근처 보기」「이 범위로 검색」）: `mat-flat-button` を地図の上に重ね、影を付ける。
- Google Maps の地図の見た目（スタイル）は変えない。

## 5. 試作のない画面・状態の見た目

試作を作ったのは 9. の7つだけ（Issue #15 コメント 6651238358）。ほかの画面・状態は、試作の画面と 2〜4. の決まりで作る。

| 画面・状態 | 代表にする試作 | 見た目の決まり |
|---|---|---|
| SC-05〜SC-10（フォームの画面） | SC-04（mobile） | 1段のフォーム（最大 400px）。上から: 画面のタイトル（`headline-small`）→ 案内の文（あれば）→ 入力欄 → 主なボタン（幅いっぱい、`mat-flat-button`）→ 文字のリンク。PC 表示でも同じ形で中央に置く |
| SC-06・SC-10（リンクを開いた後の結果） | SC-04 | 確認中はスピナー（40px）を中央に。結果は 4.3 の「画面の上の案内」の枠に文言、その下に主なボタン |
| SC-09 の reCAPTCHA | SC-04 | 入力欄と「재인증 메일 보내기」の間に置く |
| SC-11 お気に入り | SC-01（カード） | 4.4 のカードの右下に「해제」（`mat-button`）。PC は複数列（2.4） |
| SC-12 アカウント設定 | SC-04（フォーム） | 「닉네임」「비밀번호」「탈퇴」の3つの枠（`mat-card` outlined）を縦に。「탈퇴하기」は `mat-stroked-button`（色 `error`）、DL-03 の確定だけ塗りの `error` |
| SC-13 のスマホ表示 | SC-13（desktop） | 表の1行を1枚のカードにする（同じ項目。02） |
| SC-14 管理: 公演の修正 | SC-13（desktop） | 項目ごとに「項目名・自動収集の値・現在の値・修正欄・修正を取り消す」を PC は横1行、スマホは縦に（02）。修正した項目は現在の値の横に「수정됨」のラベル（`primary-container`）。「저장」は画面の下に固定しない（ページの最後に置く） |
| SC-99 エラー | — | 中央に文言（`headline-small`）と SC-01 へのリンク（`mat-flat-button`）。イラスト・画像は置かない |
| DL-01〜DL-04 | — | 3. の「ダイアログ」の行 |
| 読み込み中・空・エラー（全画面） | — | 4.3 |
| 地図を表示できない（02 の 6.1） | SC-01 | 地図の領域を `surface-container` で塗り、中央に 02 の文言。スマホ表示では下のシートを開いた状態 |
| メールの上限（02 の 6.2） | SC-04 | 4.3 の「画面の上の案内」（エラーの線付き） |
| 翻訳がない（02 の 3.4、P-06） | — | 原文の右に「원문 / 原文」のラベル（`surface-container` の背景、`label-medium`） |
| 「全国基準」「ソウル基準」（SC-03） | SC-03 | タブの見出しのすぐ下に `primary-container` のラベル。説明の文は `body-small` |
| SC-03 の人気急上昇・ジャンル別トレンド | SC-03（人気順位のタブ） | 同じ期間・ジャンルの選び方。「NEW」は `tertiary-container`、「▲3」は `success` の文字 |

## 6. 日本語表示の注意（試作は韓国語で作った）

日本語は同じ意味でも韓国語より文字が多くなることがある。実装では日本語でも次を確かめる。

| 場所 | 韓国語 → 日本語の例 | 決まり |
|---|---|---|
| ヘッダー（PC） | 인기·트렌드 → 人気・トレンド、즐겨찾기 → お気に入り、로그아웃 → ログアウト | 項目を折り返さない（`white-space: nowrap`）。PC 表示のいちばん狭い幅（960px）で日本語・管理者・一般ユーザーの3つを確かめる |
| SC-01 の場所の方法 | 구 / 지도 범위 / 공연장 → 区 / 地図の範囲 / 会場 | 400px のパネルに入らないときは2行に折り返す |
| ボタン | 조건 지우기 → 条件をクリア、재인증 메일 보내기 → 再認証のメールを送る | ボタンの幅を固定しない。スマホ表示のフォームのボタンは幅いっぱい |
| SC-04 のロック中の案内 | 解除の方法の2つのリンクが長い | リンクは1行に1つずつ縦に並べる |
| カード | 公演名の翻訳 + 原文の2段（02 の 3.4） | 公演名は2行まで、原文は1行まで（超えたら「…」）。カードの高さを固定しない |
| 表（SC-13） | 관리자 수정 → 管理者の修正 | 見出しは折り返してよい。公演 ID・期間の列は折り返さない |
| 入力の誤り・案内の文 | 日本語のほうが長い | `subscriptSizing="dynamic"` で複数行を許す |
| 日付 | — | 両方の言語で `YYYY-MM-DD`（02 の文言の例に合わせる） |

## 7. アクセシビリティ

ADR 0014 決定6 のとおり、要件としての達成基準（WCAG の等級）は置かない。次は**この文書の見た目の決まり**として守り、試作でも守る。

- 文字と背景のコントラストは 4.5:1 以上（2.1.2）。
- 押せる要素は 44px 以上（2.6）。
- 色だけで意味を伝えない（状態・エラー・順位の変化は文字でも示す）。
- フォーカスが見えにくいときは `mat.strong-focus-indicators()` を使う（ADR 0014 決定6）。
- 画像（ポスター）には公演名を代替テキストにする。

## 8. 未決事項

| No. | 内容 | 決める人・場所 |
|---|---|---|
| U-1 | Material Symbols のアイコンのフォントの配り方（Google Fonts から読み込むか、npm のパッケージで同梱するか）。ADR 0014 はアイコンの配り方を書いていない。外部の読み込み・新しいパッケージになるなら ADR が要る | architect（ADR）・人間 |
| U-2 | 色・レイアウトの採否（試作を見て人間が決める）。決定は Issue #15 のコメントに記録し、この文書と試作に反映する | 人間（PM 経由） |
| U-3 | `mat.button-toggle-overrides` で高さを 48px にすること（2.6）が ADR 0014 決定2（部品の CSS を直接上書きしない）の範囲と言えるか。範囲外とするなら、言語の切り替えなどを別の部品（例: `mat-menu`）にする | 人間・architect |

## 9. 画面 ID と試作・節の対応表

02 のすべての画面 ID。試作は [mockups/](mockups/README.md)（ファイルの一覧と Stitch の画面 ID は README）。試作の見た目がこの文書と違うところ（太さ・角丸・押せる大きさなど）は README の「07 との差」に書いた。実装はこの文書に従う。

| 画面 ID | 画面名 | 試作（スマホ表示） | 試作（PC 表示） | 試作がないときの対応 |
|---|---|---|---|---|
| SC-01 | 地図・検索 | [SC-01-mobile.png](mockups/SC-01/SC-01-mobile.png) | [SC-01-desktop.png](mockups/SC-01/SC-01-desktop.png) | 状態（読み込み中・空・エラー・位置情報・地図の上限）は 4.3・5. |
| SC-02 | 公演詳細 | [SC-02-mobile.png](mockups/SC-02/SC-02-mobile.png) | [SC-02-desktop.png](mockups/SC-02/SC-02-desktop.png) | DL-01・DL-02 は 3.、状態は 4.3 |
| SC-03 | 人気・トレンド | [SC-03-mobile.png](mockups/SC-03/SC-03-mobile.png) | — | PC 表示は 2.4・3.（ジャンルはチップ）。ほかのタブ・状態は 5.・4.3 |
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
| CM-01〜CM-03 | 共通部品 | 各試作に含む | 各試作に含む | 4.1〜4.2、3. |

---

## 한국어

# 07. UI 디자인 가이드

- 상태: 기본설계(리뷰 대기. 시안은 사람의 확인 대기)
- 담당: ui-designer
- 최종 갱신: 2026-11-06
- 전제: [02. 화면 설계서](02-screens.md)(P-01~P-15는 채택됨. Issue #15 코멘트 6651238358), [01. 요건정의서](01-requirements.md)(N-01, N-02), ADR 0014(Angular Material + CDK), ADR 0015(Google Stitch)
- 시안: [mockups/](mockups/README.md)

## 1. 위치

- 이 문서는 **겉모습**(색·글자·모양·여백·부품 선택)을 정한다. 화면의 항목·조작·상태·전이는 02가 기준이며, 이 문서와 시안은 02를 따른다(ADR 0015 결정 3).
- **토큰의 기준은 이 문서**. Stitch 디자인 시스템은 색·모서리 둥글기만 저장하므로(stitch-workflow 스킬), 시안의 겉모습과 다르면 이 문서를 따른다.
- 구현은 Angular Material(Material 3)과 CDK로 한다(ADR 0014). 토큰은 `mat.theme` Sass mixin으로 한 곳에 모으고, 부품의 CSS를 직접 덮어쓰지 않는다(ADR 0014 결정 2). 시안의 HTML(Tailwind)은 구현에 쓰지 않는다.
- 시안은 겉모습 참고이며, Stitch 생성 특성상 세부(배치·여백·샘플 값)가 02·이 문서와 어긋날 수 있다. 어긋나면 02와 이 문서가 기준.

## 2. 디자인 토큰

### 2.1 색

색 값은 Material 3 테마의 **바탕이 되는 색**(seed)과, 직접 만드는 부분(지도 표시·가로 막대·스켈레톤 등)에 쓰는 값. Angular Material 팔레트는 구현 때 공식 `ng generate @angular/material:theme-color`로 아래 primary·tertiary·error·neutral에서 만든다. 만든 뒤 2.1.2의 대비를 만족하는지 구현에서 확인한다.

#### 2.1.1 색 목록

| 토큰 | HEX | Material 3 역할 | 용도 |
|---|---|---|---|
| `primary` | `#4338CA` | primary | 주요 버튼(검색·로그인·저장), 링크, 선택된 탭·칩, 지도의 공연장 표시, "공연 예정" 라벨의 글자 |
| `on-primary` | `#FFFFFF` | on-primary | primary 위의 글자·아이콘 |
| `primary-container` | `#EEF2FF` | primary-container | "공연 예정" 라벨의 배경, 선택된 행·항목의 배경 |
| `tertiary` | `#B45309` | tertiary | 평가의 별, 수상작 배지의 글자·테두리, 인기 순위 표시 |
| `tertiary-container` | `#FFFBEB` | tertiary-container | 수상작 배지의 배경 |
| `success` | `#166534` | (직접 만듦. M3에 역할이 없음) | "공연 중" 라벨의 글자 |
| `success-container` | `#F0FDF4` | (직접 만듦) | "공연 중" 라벨의 배경 |
| `error` | `#B91C1C` | error | 입력 오류, 오류 문구, 삭제 버튼 |
| `background` / `surface` | `#FFFFFF` | surface | 화면 배경, 카드 |
| `surface-container-low` | `#F9FAFB` | surface-container-low | SC-01의 왼쪽 패널·아래 시트, 푸터의 배경 |
| `surface-container` | `#F3F4F6` | surface-container | "종료" 라벨의 배경, 표의 머리 행 |
| `placeholder` | `#E5E7EB` | (직접 만듦) | 이미지(포스터)가 없을 때의 틀, 로딩 중 스켈레톤, 지도를 불러오기 전의 틀 |
| `on-surface` | `#1F2937` | on-surface | 본문·제목의 글자 |
| `on-surface-variant` | `#4B5563` | on-surface-variant | 보조 글자(공연장명·기간·건수·원문 병기·출처), "종료" 라벨의 글자 |
| `outline` | `#D1D5DB` | outline-variant | 카드·입력란·표의 경계선(글자에는 쓰지 않음) |

- 색만으로 의미를 전하지 않는다. 공연 상태는 반드시 글자("공연 중" "공연 예정" "종료")로 보여 주고 색은 보조로 한다.
- 다크 모드는 만들지 않는다(02·01에 요건이 없음).

#### 2.1.2 대비(글자와 배경)

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
| `error` `#B91C1C` | `#FFFFFF` | 6.4:1 |

### 2.2 글자

#### 2.2.1 글꼴

**기기에 들어 있는 글꼴을 쓰고, 웹 폰트는 불러오지 않는다.** 웹 폰트를 외부에서 불러오면 새 외부 서비스가 되어 비용·이용 조건 확인과 ADR이 필요하기 때문(ADR 0014 주의점). 한국어·일본어는 다른 순서로 지정한다(같은 한자라도 글자 모양이 다르기 때문).

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

- 본문의 최소는 12px. 12px는 보조(출처·원문·날짜)에만 쓴다.
- 한국어·일본어는 영문보다 글자면이 커서 굵기는 400 / 500 두 가지만 쓴다(700은 쓰지 않음).
- 숫자(가격·거리·건수)는 `font-variant-numeric: tabular-nums`로 자릿수를 맞춘다(표·목록).

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
- SC-02의 PC 표시: 2단의 폭은 왼쪽 2 : 오른쪽 1.
- 목록 카드의 배치(SC-11의 PC 표시): 한 열 폭이 280px 이상이 되는 수의 열.

### 2.5 그림자·경계선

- 카드는 **테두리 카드**(`mat-card`의 `appearance="outlined"`, 경계선 `outline`). 그림자는 쓰지 않는다.
- 그림자(M3 elevation)를 쓰는 것은 지도 위에 겹치는 부품(지도 위 버튼, 스마트폰 표시의 아래 시트, 메뉴, 다이얼로그)뿐. Angular Material 기본 그림자 그대로.

### 2.6 누를 수 있는 요소의 크기

- 누를 수 있는 요소는 가로세로 **44px 이상**. Angular Material 밀도 0(ADR 0014 결정 3)에서는 버튼·아이콘 버튼·체크박스·라디오의 누르는 범위가 48px, 입력란 높이가 56px라서 만족한다.
- **예외 대응**: `mat-button-toggle`(언어 전환·SC-01의 장소 방법·SC-03의 기간)은 밀도 0에서도 높이 40px라 44px에 못 미친다. 테마에서 공식 토큰 API `mat.button-toggle-overrides((height: 48px))`를 써서 48px로 한다(부품의 CSS를 직접 고쳐 쓰는 것이 아니라 Angular Material이 제공하는 테마 API. ADR 0014 결정 2의 범위). 구현에서 높이가 48px가 되는지 확인한다.
- 직접 만드는 부분도 44px 이상으로 한다: 글자 링크(SC-04의 "회원가입" 등. 줄 높이를 44px로), 지도의 공연장 표시(보이는 크기 32px, 누르는 범위 44px), 카드 전체(누르면 SC-02), 페이지 넘김.
- 누를 수 있는 요소 사이는 8px 이상 띄운다.

### 2.7 아이콘

- `mat-icon`으로 Material Symbols(Outlined)를 쓴다. 쓰는 것은 의미가 정해진 것뿐: 메뉴(`menu`), 평가의 별(`star`), 외부 링크(`open_in_new`), 현재 위치(`my_location`), 조건(`tune`), 닫기(`close`), 역(`train`), 물품보관함(`lock`), 즐겨찾기(`favorite` / `favorite_border`).
- 아이콘만 있는 버튼에는 `aria-label`을 붙인다. 아이콘과 글자를 함께 쓰는 버튼에서는 글자를 반드시 보여 준다.
- 아이콘 글꼴을 배포하는 방법(Google Fonts에서 불러올지, 파일을 함께 넣을지)은 미결(8. 미결 사항).

## 3. Angular Material·CDK 부품과의 대응

02의 요소를 어떤 부품으로 만들지의 대응. Material에 없는 부품은 CDK로 직접 만든다(ADR 0014 결정 5).

| 02의 요소 | 부품 | 겉모습 규칙 |
|---|---|---|
| CM-01 헤더 | `mat-toolbar`. 스마트폰 표시의 메뉴는 `mat-sidenav`(오른쪽에서 열림) | 배경 `surface`, 아래에 경계선 `outline`. 높이 PC 64px·스마트폰 56px. 선택된 화면의 글자를 `primary` |
| 표시 언어 전환 | `mat-button-toggle-group`("한국어" "日本語") | 언어 이름은 각 언어로 쓴다(전환해도 바꾸지 않음) |
| CM-02 푸터 | 직접 만듦(`<footer>`) | 배경 `surface-container-low`, 글자 `body-small`·`on-surface-variant` |
| CM-03 알림 | `MatSnackBar` | 화면 아래 가운데. 기본 겉모습 |
| 주요 조작(검색·로그인·저장·등록) | `mat-flat-button`(채움) | 한 화면에 하나를 기준으로 한다 |
| 보조 조작(조건 지우기·취소·즐겨찾기·리뷰 쓰기) | `mat-stroked-button`(테두리) | — |
| 링크 조작(회원가입·비밀번호 찾기·페이지 안 이동) | `mat-button`(텍스트) 또는 `<a>` | 글자 `primary` |
| 삭제·탈퇴 | `mat-flat-button`(색 `error`) | DL-02·DL-03의 확정 버튼만 |
| 입력란 | `mat-form-field`(`appearance="outline"`, `subscriptSizing="dynamic"`) | 오류는 입력란 아래에 `mat-error`(02의 3.3). 긴 문구가 잘리지 않도록 `dynamic`으로 한다 |
| 기간(시작일·종료일) | `mat-date-range-input` + `mat-date-range-picker` | 날짜 형식은 `YYYY-MM-DD` |
| 구·장르(여러 개), 표시 상태 | `mat-select`(구·장르는 `multiple`) | — |
| 장소 방법(구·지도 범위·공연장) | `mat-button-toggle-group`(하나만 선택) | 일본어에서 들어가지 않으면 줄바꿈(6.) |
| 공연 상태(공연 중·공연 예정), "종료된 공연도 포함" | `mat-checkbox` | — |
| 공연장 후보 | `mat-autocomplete` | — |
| SC-03의 기간(일간·주간·월간) | `mat-button-toggle-group` | — |
| SC-03의 장르 | PC: `mat-chip-listbox`(하나 선택), 스마트폰: `mat-select`(02) | — |
| 탭(SC-03, SC-01의 현재 위치 주변) | `mat-tab-group` | — |
| 공연 카드(02의 3.7) | `mat-card`(`outlined`) 전체를 `<a>`로 | 4.4 |
| 상태 라벨·수상작 배지·"원문"·"전국 기준" | 직접 만듦(누를 수 없는 라벨. `<span>`) | 모서리 8px, `label-medium`. 누를 수 없으므로 `mat-chip`은 쓰지 않는다 |
| 평가(★평균(건수)) | `mat-icon`(`star`, 색 `tertiary`) + 글자 | "★4.3(12)". 0건은 "아직 리뷰가 없습니다"를 `body-small`로 |
| 페이지 넘김 | `mat-paginator`(건수 선택은 보이지 않음. 20건 고정) | — |
| 관리 공연 목록(PC) | `mat-table` | 행 전체를 누를 수 있다. 머리 행 배경 `surface-container` |
| 스마트폰 표시의 조건 | `MatDialog`(전체 화면) | — |
| 스마트폰 표시의 아래 시트(SC-01) | **CDK로 직접 만듦**(지도를 보면서 조작하므로 모달인 `MatBottomSheet`는 쓰지 않는다) | 위 모서리 28px, 위에 "지도 / 목록" 전환 |
| 다이얼로그 DL-01~DL-04 | `MatDialog` | 모서리 28px. 버튼은 오른쪽 아래에 "취소"(텍스트) → 확정(채움) 순서 |
| DL-01의 별(1~5) | 직접 만듦(`mat-icon-button` 5개. 라디오 그룹으로 읽어 준다) | 누르는 범위 48px |
| 로딩 중(버튼) | `mat-progress-spinner`(20px)를 버튼 안에 | 누를 수 없게 한다(02의 3.6) |
| 로딩 중(목록) | 직접 만든 스켈레톤(색 `placeholder`) | 카드와 같은 크기의 회색 틀 |
| 가로 막대(SC-03의 장르별 트렌드) | CSS로 직접 만듦(02) | 막대 색 `primary`, 바탕 `surface-container`. 비율 숫자를 반드시 글자로 보여 준다 |
| 지도 | `@angular/google-maps`(ADR 0006) | 4.5 |
| reCAPTCHA(SC-09) | Google 부품(ADR 0011) | 겉모습은 바꾸지 않는다 |

## 4. 공통 부품·공통 표시의 겉모습

### 4.1 CM-01 헤더

- PC 표시: 왼쪽에 서비스명 "CatTicket", 그 오른쪽에 "지도" "인기·트렌드"(로그인 상태에 따라 "즐겨찾기" "관리"), 오른쪽 끝에 로그인 상태에 따른 버튼("로그인" "회원가입" / "계정(닉네임)" "로그아웃")과 언어 전환.
- 스마트폰 표시: 왼쪽에 서비스명, 오른쪽에 언어 전환과 메뉴 버튼. 나머지 항목은 메뉴 안(02의 3.1).

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

### 4.4 공연 카드(02의 3.7)

- 스마트폰 표시·SC-01의 왼쪽 패널: 가로형(왼쪽에 포스터 72×96px, 오른쪽에 글자). SC-11의 PC 표시: 같은 모양을 여러 열로 배치.
- 글자 순서: 공연명(`title-medium`. 2줄까지, 넘으면 "…") → 공연장명(`body-medium`·`on-surface-variant`) → 공연 기간 → 장르·상태 라벨·수상작 배지를 한 줄에 → 평가.
- 일본어 표시의 공연명·공연장명은 번역(크게) 아래에 원문(`body-small`·`on-surface-variant`)을 한 줄로(02의 3.4).
- 포스터가 없으면 `placeholder` 틀.

### 4.5 지도(SC-01만)

- 공연장 표시: 원(보이는 크기 32px, 누르는 범위 44px), 배경 `primary`, 안에 공연 수(흰색, `label-medium`). 선택된 표시는 40px로 하고 흰 테두리를 붙인다.
- 현재 위치 표시: `primary` 원에 흰 테두리. 반경 500m 원: 선 `primary`, 채움 `primary`의 12%.
- 역 표시: `on-surface` 원에 흰 `train` 아이콘.
- 지도 위 버튼("근처 보기" "이 범위로 검색"): `mat-flat-button`을 지도 위에 겹치고 그림자를 붙인다.
- Google Maps 지도의 겉모습(스타일)은 바꾸지 않는다.

## 5. 시안이 없는 화면·상태의 겉모습

시안을 만든 것은 9.의 7개뿐(Issue #15 코멘트 6651238358). 다른 화면·상태는 시안의 화면과 2~4.의 규칙으로 만든다.

| 화면·상태 | 대표로 삼는 시안 | 겉모습 규칙 |
|---|---|---|
| SC-05~SC-10(폼 화면) | SC-04(mobile) | 1단 폼(최대 400px). 위에서부터: 화면 제목(`headline-small`) → 안내 문장(있으면) → 입력란 → 주요 버튼(폭 가득, `mat-flat-button`) → 글자 링크. PC 표시에서도 같은 모양으로 가운데에 둔다 |
| SC-06·SC-10(링크를 연 뒤의 결과) | SC-04 | 확인 중에는 스피너(40px)를 가운데에. 결과는 4.3의 "화면 위 안내" 틀에 문구, 그 아래에 주요 버튼 |
| SC-09의 reCAPTCHA | SC-04 | 입력란과 "재인증 메일 보내기" 사이에 둔다 |
| SC-11 즐겨찾기 | SC-01(카드) | 4.4 카드의 오른쪽 아래에 "해제"(`mat-button`). PC는 여러 열(2.4) |
| SC-12 계정 설정 | SC-04(폼) | "닉네임" "비밀번호" "탈퇴" 세 틀(`mat-card` outlined)을 세로로. "탈퇴하기"는 `mat-stroked-button`(색 `error`), DL-03의 확정만 채움 `error` |
| SC-13의 스마트폰 표시 | SC-13(desktop) | 표의 한 행을 카드 한 장으로(같은 항목. 02) |
| SC-14 관리: 공연 수정 | SC-13(desktop) | 항목마다 "항목명·자동 수집 값·현재 값·수정란·수정 취소"를 PC는 가로 한 줄, 스마트폰은 세로로(02). 수정한 항목은 현재 값 옆에 "수정됨" 라벨(`primary-container`). "저장"은 화면 아래에 고정하지 않는다(페이지 끝에 둔다) |
| SC-99 오류 | — | 가운데에 문구(`headline-small`)와 SC-01 링크(`mat-flat-button`). 일러스트·이미지는 두지 않는다 |
| DL-01~DL-04 | — | 3.의 "다이얼로그" 행 |
| 로딩 중·빈 결과·오류(모든 화면) | — | 4.3 |
| 지도를 표시할 수 없음(02의 6.1) | SC-01 | 지도 영역을 `surface-container`로 칠하고 가운데에 02의 문구. 스마트폰 표시에서는 아래 시트를 연 상태 |
| 메일 상한(02의 6.2) | SC-04 | 4.3의 "화면 위 안내"(오류 선 포함) |
| 번역이 없음(02의 3.4, P-06) | — | 원문 오른쪽에 "원문 / 原文" 라벨(`surface-container` 배경, `label-medium`) |
| "전국 기준" "서울 기준"(SC-03) | SC-03 | 탭 제목 바로 아래에 `primary-container` 라벨. 설명 문장은 `body-small` |
| SC-03의 인기 급상승·장르별 트렌드 | SC-03(인기 순위 탭) | 같은 기간·장르 선택 방식. "NEW"는 `tertiary-container`, "▲3"은 `success` 글자 |

## 6. 일본어 표시의 주의(시안은 한국어로 만들었다)

일본어는 같은 뜻이라도 한국어보다 글자가 많아질 때가 있다. 구현에서는 일본어로도 다음을 확인한다.

| 위치 | 한국어 → 일본어 예 | 규칙 |
|---|---|---|
| 헤더(PC) | 인기·트렌드 → 人気・トレンド, 즐겨찾기 → お気に入り, 로그아웃 → ログアウト | 항목을 줄바꿈하지 않는다(`white-space: nowrap`). PC 표시의 가장 좁은 폭(960px)에서 일본어·관리자·일반 사용자 세 가지를 확인한다 |
| SC-01의 장소 방법 | 구 / 지도 범위 / 공연장 → 区 / 地図の範囲 / 会場 | 400px 패널에 들어가지 않으면 2줄로 줄바꿈 |
| 버튼 | 조건 지우기 → 条件をクリア, 재인증 메일 보내기 → 再認証のメールを送る | 버튼 폭을 고정하지 않는다. 스마트폰 표시 폼의 버튼은 폭 가득 |
| SC-04의 잠금 안내 | 해제 방법 두 링크가 길다 | 링크는 한 줄에 하나씩 세로로 놓는다 |
| 카드 | 공연명 번역 + 원문 2단(02의 3.4) | 공연명은 2줄까지, 원문은 1줄까지(넘으면 "…"). 카드 높이를 고정하지 않는다 |
| 표(SC-13) | 관리자 수정 → 管理者の修正 | 머리글은 줄바꿈해도 된다. 공연 ID·기간 열은 줄바꿈하지 않는다 |
| 입력 오류·안내 문장 | 일본어가 더 길다 | `subscriptSizing="dynamic"`으로 여러 줄을 허용한다 |
| 날짜 | — | 두 언어 모두 `YYYY-MM-DD`(02 문구의 예에 맞춘다) |

## 7. 접근성

ADR 0014 결정 6대로 요건으로서의 달성 기준(WCAG 등급)은 두지 않는다. 다음은 **이 문서의 겉모습 규칙**으로 지키고, 시안에서도 지킨다.

- 글자와 배경의 대비는 4.5:1 이상(2.1.2).
- 누를 수 있는 요소는 44px 이상(2.6).
- 색만으로 의미를 전하지 않는다(상태·오류·순위 변화는 글자로도 보여 준다).
- 포커스가 잘 안 보이면 `mat.strong-focus-indicators()`를 쓴다(ADR 0014 결정 6).
- 이미지(포스터)에는 공연명을 대체 텍스트로 한다.

## 8. 미결 사항

| No. | 내용 | 정하는 사람·곳 |
|---|---|---|
| U-1 | Material Symbols 아이콘 글꼴의 배포 방법(Google Fonts에서 불러올지, npm 패키지로 함께 넣을지). ADR 0014는 아이콘 배포 방법을 쓰지 않았다. 외부 로드·새 패키지가 된다면 ADR이 필요 | architect(ADR)·사람 |
| U-2 | 색·레이아웃의 채택 여부(시안을 보고 사람이 정한다). 결정은 Issue #15 코멘트에 기록하고 이 문서와 시안에 반영한다 | 사람(PM 경유) |
| U-3 | `mat.button-toggle-overrides`로 높이를 48px로 하는 것(2.6)이 ADR 0014 결정 2(부품의 CSS를 직접 덮어쓰지 않음)의 범위라고 할 수 있는지. 범위 밖이라면 언어 전환 등을 다른 부품(예: `mat-menu`)으로 한다 | 사람·architect |

## 9. 화면 ID와 시안·절의 대응표

02의 모든 화면 ID. 시안은 [mockups/](mockups/README.md)(파일 목록과 Stitch 화면 ID는 README). 시안의 겉모습이 이 문서와 다른 곳(굵기·모서리 둥글기·누르는 크기 등)은 README의 "07과의 차이"에 썼다. 구현은 이 문서를 따른다.

| 화면 ID | 화면명 | 시안(스마트폰 표시) | 시안(PC 표시) | 시안이 없을 때의 대응 |
|---|---|---|---|---|
| SC-01 | 지도·검색 | [SC-01-mobile.png](mockups/SC-01/SC-01-mobile.png) | [SC-01-desktop.png](mockups/SC-01/SC-01-desktop.png) | 상태(로딩 중·빈 결과·오류·위치 정보·지도 상한)는 4.3·5. |
| SC-02 | 공연 상세 | [SC-02-mobile.png](mockups/SC-02/SC-02-mobile.png) | [SC-02-desktop.png](mockups/SC-02/SC-02-desktop.png) | DL-01·DL-02는 3., 상태는 4.3 |
| SC-03 | 인기·트렌드 | [SC-03-mobile.png](mockups/SC-03/SC-03-mobile.png) | — | PC 표시는 2.4·3.(장르는 칩). 다른 탭·상태는 5.·4.3 |
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
| CM-01~CM-03 | 공통 부품 | 각 시안에 포함 | 각 시안에 포함 | 4.1~4.2, 3. |
