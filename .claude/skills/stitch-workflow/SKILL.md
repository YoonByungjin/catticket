---
name: stitch-workflow
description: Google Stitch の MCP ツールで、UI デザインガイド（docs/design/07-ui-guide.md）と画面の試作（docs/design/mockups/）を作る手順。デザインシステムの作り方、画面 ID ごとのプロンプト、生成回数のルール、結果をリポジトリに保存する方法。ui-designer が試作を作るとき、または Stitch を使うときに読む。
---

# Stitch の使い方（CatTicket）

ui-designer が試作を作るときの手順。ツールの動きは 2026-11-05 の接続試験（Issue #16 の PR）で確認したもの。

## 0. 前提

- Stitch の MCP サーバー（`https://stitch.googleapis.com/mcp`）は**ユーザー設定（`-s user`）**に登録してある。API キーはそこにだけある。リポジトリ・`.mcp.json`・試作のファイルにキーを書かない。
- ツールが見つからない・認証エラーになるときは、作業を止めて報告する（人間が `claude mcp list` で確認する）。
- `delete_project` は使わない。不要なプロジェクトは人間が消す。

## 1. 使うツール

| ツール | 何をするか | 確認した動き |
|---|---|---|
| `create_project` | プロジェクトを作る | `projects/{ID}` が返る。非公開（PRIVATE） |
| `get_project` / `list_projects` | プロジェクトの情報・一覧 | `list_projects` は作った直後でも空 `{}` を返したことがある。**ID は自分で控える** |
| `create_design_system` | デザインシステム（トークン）を作る | `assets/{ID}` が返る。**保存されたのは色・カラーモード・角丸だけ**で、指定したフォント・余白は返らず、生成された HTML にも反映されなかった |
| `update_design_system` | デザインシステムを更新する | 試験では `invalid argument` で失敗した。**頼らない** |
| `list_design_systems` | デザインシステムの一覧 | `projectId` を指定すれば動く（指定しないと `invalid argument`） |
| `apply_design_system` | 既存の画面にデザインシステムを当てる | 未確認（`get_project` が画面インスタンス ID を返さなかった） |
| `generate_screen_from_text` | プロンプトから画面を1つ作る | 1〜2分。画面 ID・HTML と画像のファイル名が返る。**プロンプトは Stitch が書き換える**（返り値の `prompt`） |
| `edit_screens` | 画面をプロンプトで直す | **元の画面は残り、新しい画面 ID ができる**。指示どおりに要素を消す・色を変えることができた |
| `generate_variants` | 画面の別案を作る | 未確認。案の数は 1〜5 |
| `list_screens` / `get_screen` | 画面の一覧・情報 | `list_screens` は空を返したことがある。`get_screen` は画面 ID で確実に取れる |
| `download_file` | 画面の HTML・画像を取り出す | HTML は文字列で返る。画像（PNG）は base64。大きいと結果がファイルに保存され、そのパスが返る |

返り値の `suggestion`（「検索欄を足しますか」など）は**提案であり、採用しない**。02 にない要素を足すことになるため。

## 2. 手順

### 2.1 07 にトークンを書く（先に）

`docs/design/07-ui-guide.md` に次を決めて書く。**トークンの正は 07**（Stitch はフォント・余白を保存しないため）。

- 色: primary・secondary・背景・文字・エラーなど（HEX）。文字と背景のコントラスト 4.5:1 以上
- 文字: フォント（韓国語・日本語が表示できるもの）、見出し・本文・ラベルの大きさ・太さ
- 角丸・余白（4px か 8px の倍数）・影
- 押せる要素の最小サイズ（44px）
- 採用された UI ライブラリ（ADR）のコンポーネントとの対応

### 2.2 Stitch のプロジェクトとデザインシステムを作る（1回だけ）

1. `create_project`（タイトル: `catticket-ui`）。返った ID を `docs/design/mockups/README.md` に書く
2. `create_design_system`（`projectId` を指定）。07 の primary を `customColor`、角丸を `roundness`、`colorMode: LIGHT`。フォントは選択肢から 07 に近いもの
3. 返った `assets/{ID}` を `docs/design/mockups/README.md` に書く。以後のすべての生成で `designSystem` にこの ID を渡す

同じ作業の続きでは、新しいプロジェクトを作らず、README に書いた ID を使う。

### 2.3 画面 ID ごとにプロンプトを書く

02 の画面 ID 1つにつき1つのプロンプト。英語で書き（生成が安定する）、画面の文言は韓国語で指定する。形:

```
[S-01 {画面名}] {MOBILE か DESKTOP} screen for a performance-map web service in Seoul.
Design tokens: primary {HEX}, background {HEX}, text {HEX}, font {07 のフォント}, corner radius {px}, spacing unit {px}.
Elements (top to bottom), exactly these and nothing else:
1. {02 の項目1}: {種類（ボタン・入力欄など）}, label "{韓国語の文言}"
2. ...
State: {通常 / 読み込み中 / 空 / エラー のどれか}.
Use plain gray placeholder boxes with a centered label for maps and images (no photos, no illustrations).
Do not add any element not listed above (no search bar, back button, tab bar, ads, or sample photos unless listed).
```

- 要素は 02 の項目をそのまま並べる。02 にない要素を足さない。足りないと思ったら描かずに報告する。
- **地図・画像は灰色の箱にする**。写真を頼むと、地図の代わりに架空のアプリ画面の写真が入り（試験で確認）、PNG も 1MB を超える。
- デザインシステムがフォント・余白を保存しないため、**トークンをプロンプトにも書く**。
- 状態（読み込み中・空・エラー）は、02 に書かれていて見た目が大きく変わるものだけ別の画面にする。

### 2.4 生成して確認する

1. `generate_screen_from_text`（`projectId`、`designSystem`、`deviceType`、`prompt`）。**タイムアウトしても再実行しない**。`get_screen` を 30 秒おきに最大 10 回確認する
2. `download_file` で画像を取り出し、保存（3章）してから `Read` で画像を見る
3. 確認: 02 にない要素がないか / 02 の要素が欠けていないか / 07 の色が使われているか / 文言が韓国語か
4. 直すところがあれば `edit_screens` で、**直すことを全部1回のプロンプトにまとめて**頼む（例: 「戻る矢印を消す。ボタンの色を #3B5BDB にする。ほかの要素を足さない」）

### 2.5 PM・人間の確認

試作は PM が人間に見せて確認する。決まった内容（採用する案・直すところ）は Issue のコメントに残り、それを 07 と試作に反映する。ui-designer が自分で「確定」にしない。

## 3. 結果をリポジトリに保存する

置き場所: `docs/design/mockups/`

```
docs/design/mockups/
  README.md                  プロジェクト ID・デザインシステム ID、画面 ID ごとの一覧
  S-01/
    S-01-mobile.png          画像（確認用。PR で見られる）
    S-01-mobile.html         Stitch の HTML（参考）
    S-01-desktop.png
    S-01-desktop.html
```

- ファイル名: `{画面ID}-{mobile|desktop}[-{状態}].{png|html}`（状態は `loading` / `empty` / `error`）
- **画像**: `download_file` の結果がファイルに保存されたときは、そのパスを使って次で PNG にする（保存先は `docs/design/mockups/` の下だけ。1MB を超えると止まる）:
  ```
  python -I scripts/stitch-save.py {結果の JSON ファイルのパス} docs/design/mockups/S-01/S-01-mobile.png
  ```
- **HTML**: 結果に `html` が文字列で返ったときは、その内容を `Write` で `.html` に保存する。ファイルに保存されたときは上と同じスクリプトで `.html` を指定する。
- HTML は Tailwind の CDN と Google Fonts を読み込み、画像は `lh3.googleusercontent.com` の URL を参照する（期限が切れる可能性がある）。**HTML は参考であり、frontend-dev は実装にそのまま使わない**（Tailwind は ADR にない）。見た目の正は PNG と 07。
- `mockups/README.md` の一覧には、画面 ID・端末・状態・ファイル・Stitch の画面 ID・生成日・使ったプロンプト（Stitch が書き換えた後の `prompt`）・人間の確認（未 / 済・Issue コメントへのリンク）を書く。
- 採用しなかった案・途中の画面は保存しない（最後の版だけ）。
- 保存後、`git diff --stat` でファイルの大きさを確認する。

## 4. 生成回数のルール

Stitch には利用回数の上限がある。無駄に使わない。

| 操作 | 上限 |
|---|---|
| `create_project` / `create_design_system` | 作業全体で 1 回ずつ（失敗したときを除く） |
| `generate_screen_from_text` | 画面 ID × 端末 × 状態 ごとに 1 回 |
| `edit_screens` | 同じ画面に 2 回まで（直すことを1回にまとめる） |
| `generate_variants` | 人間（PM 経由）が別案を求めたときだけ。`variantCount` は 2 以下 |
| 失敗・タイムアウト | 再生成しない。`get_screen` で確認する |

- 上限に達しても直らないときは、作業を止めて「何を頼んだか・何が直らなかったか」を報告する。
- 作業全体の生成・編集の回数を数え、報告に書く。

## 5. やってはいけないこと

- API キーを表示する・ファイルに書く・`.mcp.json` を作る。
- 02 にない要素・画面を描く。返り値の `suggestion` を採用する。
- 写真・イラストを入れる（灰色の箱にする）。
- `delete_project` を使う。
- 試作の HTML を `frontend/` にコピーする。

---

## 한국어

# Stitch 사용법 (CatTicket)

ui-designer가 시안을 만들 때의 절차. 도구의 동작은 2026-11-05 연결 시험(Issue #16의 PR)에서 확인한 것.

## 0. 전제

- Stitch MCP 서버(`https://stitch.googleapis.com/mcp`)는 **사용자 설정(`-s user`)**에 등록되어 있다. API 키는 거기에만 있다. 저장소·`.mcp.json`·시안 파일에 키를 쓰지 않는다.
- 도구가 보이지 않거나 인증 오류가 나면 작업을 멈추고 보고한다(사람이 `claude mcp list`로 확인한다).
- `delete_project`는 쓰지 않는다. 필요 없는 프로젝트는 사람이 지운다.

## 1. 쓰는 도구

| 도구 | 하는 일 | 확인한 동작 |
|---|---|---|
| `create_project` | 프로젝트를 만든다 | `projects/{ID}`가 돌아온다. 비공개(PRIVATE) |
| `get_project` / `list_projects` | 프로젝트 정보·목록 | `list_projects`는 만든 직후에도 빈 `{}`를 돌려준 적이 있다. **ID는 직접 적어 둔다** |
| `create_design_system` | 디자인 시스템(토큰)을 만든다 | `assets/{ID}`가 돌아온다. **저장된 것은 색·컬러 모드·모서리 둥글기뿐**이고, 지정한 글꼴·여백은 돌아오지 않았으며 생성된 HTML에도 반영되지 않았다 |
| `update_design_system` | 디자인 시스템을 갱신한다 | 시험에서 `invalid argument`로 실패했다. **의존하지 않는다** |
| `list_design_systems` | 디자인 시스템 목록 | `projectId`를 지정하면 동작한다(지정하지 않으면 `invalid argument`) |
| `apply_design_system` | 기존 화면에 디자인 시스템을 적용한다 | 미확인(`get_project`가 화면 인스턴스 ID를 돌려주지 않았다) |
| `generate_screen_from_text` | 프롬프트로 화면 하나를 만든다 | 1~2분. 화면 ID·HTML과 이미지 파일 이름이 돌아온다. **프롬프트는 Stitch가 고쳐 쓴다**(반환값의 `prompt`) |
| `edit_screens` | 화면을 프롬프트로 고친다 | **원래 화면은 남고 새 화면 ID가 생긴다**. 지시대로 요소를 지우고 색을 바꿀 수 있었다 |
| `generate_variants` | 화면의 다른 안을 만든다 | 미확인. 안의 수는 1~5 |
| `list_screens` / `get_screen` | 화면 목록·정보 | `list_screens`는 빈 값을 돌려준 적이 있다. `get_screen`은 화면 ID로 확실히 가져온다 |
| `download_file` | 화면의 HTML·이미지를 꺼낸다 | HTML은 문자열로 돌아온다. 이미지(PNG)는 base64. 크면 결과가 파일로 저장되고 그 경로가 돌아온다 |

반환값의 `suggestion`("검색창을 더할까요" 등)은 **제안이며 채택하지 않는다**. 02에 없는 요소를 더하게 되기 때문.

## 2. 절차

### 2.1 07에 토큰을 쓴다 (먼저)

`docs/design/07-ui-guide.md`에 다음을 정해서 쓴다. **토큰의 기준은 07**(Stitch는 글꼴·여백을 저장하지 않으므로).

- 색: primary·secondary·배경·글자·오류 등(HEX). 글자와 배경의 대비 4.5:1 이상
- 글자: 글꼴(한국어·일본어를 표시할 수 있는 것), 제목·본문·라벨의 크기·굵기
- 모서리 둥글기·여백(4px 또는 8px의 배수)·그림자
- 누를 수 있는 요소의 최소 크기(44px)
- 채택된 UI 라이브러리(ADR)의 컴포넌트와의 대응

### 2.2 Stitch 프로젝트와 디자인 시스템을 만든다 (한 번만)

1. `create_project`(제목: `catticket-ui`). 돌아온 ID를 `docs/design/mockups/README.md`에 쓴다
2. `create_design_system`(`projectId` 지정). 07의 primary를 `customColor`, 모서리 둥글기를 `roundness`, `colorMode: LIGHT`. 글꼴은 선택지 중 07에 가까운 것
3. 돌아온 `assets/{ID}`를 `docs/design/mockups/README.md`에 쓴다. 이후 모든 생성에서 `designSystem`에 이 ID를 넘긴다

같은 작업을 이어서 할 때는 새 프로젝트를 만들지 않고 README에 쓴 ID를 쓴다.

### 2.3 화면 ID별로 프롬프트를 쓴다

02의 화면 ID 하나에 프롬프트 하나. 영어로 쓰고(생성이 안정적), 화면 문구는 한국어로 지정한다. 형식:

```
[S-01 {화면 이름}] {MOBILE 또는 DESKTOP} screen for a performance-map web service in Seoul.
Design tokens: primary {HEX}, background {HEX}, text {HEX}, font {07의 글꼴}, corner radius {px}, spacing unit {px}.
Elements (top to bottom), exactly these and nothing else:
1. {02의 항목1}: {종류(버튼·입력란 등)}, label "{한국어 문구}"
2. ...
State: {보통 / 로딩 중 / 빈 결과 / 오류 중 하나}.
Use plain gray placeholder boxes with a centered label for maps and images (no photos, no illustrations).
Do not add any element not listed above (no search bar, back button, tab bar, ads, or sample photos unless listed).
```

- 요소는 02의 항목을 그대로 나열한다. 02에 없는 요소를 더하지 않는다. 부족하다고 생각되면 그리지 말고 보고한다.
- **지도·이미지는 회색 상자로 한다**. 사진을 요청하면 지도 대신 가상의 앱 화면 사진이 들어가고(시험에서 확인) PNG도 1MB를 넘는다.
- 디자인 시스템이 글꼴·여백을 저장하지 않으므로 **토큰을 프롬프트에도 쓴다**.
- 상태(로딩 중·빈 결과·오류)는 02에 쓰여 있고 겉모습이 크게 바뀌는 것만 별도 화면으로 만든다.

### 2.4 생성하고 확인한다

1. `generate_screen_from_text`(`projectId`, `designSystem`, `deviceType`, `prompt`). **타임아웃이 나도 다시 실행하지 않는다**. `get_screen`을 30초 간격으로 최대 10번 확인한다
2. `download_file`로 이미지를 꺼내 저장(3장)한 뒤 `Read`로 이미지를 본다
3. 확인: 02에 없는 요소가 없는지 / 02의 요소가 빠지지 않았는지 / 07의 색이 쓰였는지 / 문구가 한국어인지
4. 고칠 곳이 있으면 `edit_screens`로, **고칠 것을 모두 한 번의 프롬프트에 모아서** 요청한다(예: "뒤로 가기 화살표를 지운다. 버튼 색을 #3B5BDB로 한다. 다른 요소를 더하지 않는다")

### 2.5 PM·사람의 확인

시안은 PM이 사람에게 보여주고 확인한다. 정해진 내용(채택할 안·고칠 곳)은 Issue 코멘트에 남고, 그것을 07과 시안에 반영한다. ui-designer가 스스로 "확정"하지 않는다.

## 3. 결과를 저장소에 저장한다

위치: `docs/design/mockups/`

```
docs/design/mockups/
  README.md                  프로젝트 ID·디자인 시스템 ID, 화면 ID별 목록
  S-01/
    S-01-mobile.png          이미지(확인용. PR에서 볼 수 있다)
    S-01-mobile.html         Stitch의 HTML(참고)
    S-01-desktop.png
    S-01-desktop.html
```

- 파일 이름: `{화면ID}-{mobile|desktop}[-{상태}].{png|html}` (상태는 `loading` / `empty` / `error`)
- **이미지**: `download_file` 결과가 파일로 저장되었을 때는 그 경로로 다음을 실행해 PNG로 만든다(저장 위치는 `docs/design/mockups/` 아래만. 1MB를 넘으면 멈춘다):
  ```
  python -I scripts/stitch-save.py {결과 JSON 파일 경로} docs/design/mockups/S-01/S-01-mobile.png
  ```
- **HTML**: 결과에 `html`이 문자열로 돌아왔을 때는 그 내용을 `Write`로 `.html`에 저장한다. 파일로 저장되었을 때는 위와 같은 스크립트에서 `.html`을 지정한다.
- HTML은 Tailwind CDN과 Google Fonts를 불러오고, 이미지는 `lh3.googleusercontent.com` URL을 참조한다(기한이 끝날 수 있다). **HTML은 참고용이며 frontend-dev는 구현에 그대로 쓰지 않는다**(Tailwind는 ADR에 없음). 겉모습의 기준은 PNG와 07.
- `mockups/README.md` 목록에는 화면 ID·단말·상태·파일·Stitch 화면 ID·생성일·사용한 프롬프트(Stitch가 고쳐 쓴 뒤의 `prompt`)·사람의 확인(미 / 완료·Issue 코멘트 링크)을 쓴다.
- 채택하지 않은 안·중간 화면은 저장하지 않는다(마지막 판만).
- 저장 후 `git diff --stat`으로 파일 크기를 확인한다.

## 4. 생성 횟수 규칙

Stitch에는 사용 횟수 상한이 있다. 낭비하지 않는다.

| 조작 | 상한 |
|---|---|
| `create_project` / `create_design_system` | 작업 전체에서 각 1번(실패했을 때 제외) |
| `generate_screen_from_text` | 화면 ID × 단말 × 상태마다 1번 |
| `edit_screens` | 같은 화면에 2번까지(고칠 것을 한 번에 모은다) |
| `generate_variants` | 사람(PM 경유)이 다른 안을 요청했을 때만. `variantCount`는 2 이하 |
| 실패·타임아웃 | 다시 생성하지 않는다. `get_screen`으로 확인한다 |

- 상한에 이르러도 고쳐지지 않으면 작업을 멈추고 "무엇을 요청했고 무엇이 고쳐지지 않았는지"를 보고한다.
- 작업 전체의 생성·편집 횟수를 세어 보고에 쓴다.

## 5. 하면 안 되는 것

- API 키를 보여주기·파일에 쓰기·`.mcp.json` 만들기.
- 02에 없는 요소·화면을 그리기. 반환값의 `suggestion`을 채택하기.
- 사진·일러스트를 넣기(회색 상자로 한다).
- `delete_project`를 쓰기.
- 시안 HTML을 `frontend/`에 복사하기.
