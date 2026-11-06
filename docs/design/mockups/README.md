# 画面の試作（Google Stitch）

- 担当: ui-designer
- 作成日: 2026-11-06（Issue #15）
- 位置づけ: **見た目の参考**。画面の項目・操作・状態は [02](../02-screens.md)、トークンは [07](../07-ui-guide.md) が正（ADR 0015 決定3）。HTML は Tailwind の CDN と Google Fonts を読み込む Stitch の出力で、**実装（`frontend/`）には使わない**（Tailwind は ADR にない）。見た目の正は PNG と 07。

## Stitch のプロジェクト

| 項目 | 値 |
|---|---|
| プロジェクト | `projects/17389669384892123455`（タイトル `catticket-ui`、PRIVATE） |
| デザインシステム | `assets/10494663557269344773`（`CatTicket (07-ui-guide)`。primary `#4338CA`、tertiary `#B45309`、neutral `#1F2937`、`ROUND_TWELVE`、LIGHT、フォント `NOTO_SANS`） |

同じ作業の続きでは新しいプロジェクトを作らず、上の ID を使う。

## 試作の一覧

試作の対象は Issue #15 コメント 6651238358 の決定の7つだけ。状態はすべて「通常（データあり）」（SC-04 は「通常」）。

| 画面 ID | 端末 | ログインの状態 | ファイル | Stitch の画面 ID（最後の版） | 生成 / 編集 | 生成日 | 人間の確認 |
|---|---|---|---|---|---|---|---|
| SC-01 | mobile | 未ログイン | [SC-01-mobile.png](SC-01/SC-01-mobile.png) / [.html](SC-01/SC-01-mobile.html) | `d57f0846cac34568adca9b5bd1a5eefe` | 1 / 0 | 2026-11-06 | 承認（2026-11-06、[Issue #15 コメント 6676583842](https://github.com/YoonByungjin/catticket/issues/15#issuecomment-6676583842)） |
| SC-01 | desktop | 未ログイン | [SC-01-desktop.png](SC-01/SC-01-desktop.png) / [.html](SC-01/SC-01-desktop.html) | `62fc8968042c4191a00610e6124dc540` | 1 / 2 | 2026-11-06 | 承認（2026-11-06、[Issue #15 コメント 6676583842](https://github.com/YoonByungjin/catticket/issues/15#issuecomment-6676583842)） |
| SC-02 | mobile | 一般ユーザー | [SC-02-mobile.png](SC-02/SC-02-mobile.png) / [.html](SC-02/SC-02-mobile.html) | `4ef838ab13174e1797cdbbed5b9d6590` | 1 / 1 | 2026-11-06 | 承認（2026-11-06、[Issue #15 コメント 6676583842](https://github.com/YoonByungjin/catticket/issues/15#issuecomment-6676583842)） |
| SC-02 | desktop | 一般ユーザー | [SC-02-desktop.png](SC-02/SC-02-desktop.png) / [.html](SC-02/SC-02-desktop.html) | `51c79232379c40cbbf3353b2404ef555` | 1 / 0 | 2026-11-06 | 承認（2026-11-06、[Issue #15 コメント 6676583842](https://github.com/YoonByungjin/catticket/issues/15#issuecomment-6676583842)） |
| SC-03 | mobile | 未ログイン | [SC-03-mobile.png](SC-03/SC-03-mobile.png) / [.html](SC-03/SC-03-mobile.html) | `2ec6737638dc457fb3ebef9f1602903a` | 1 / 1 | 2026-11-06 | 承認（2026-11-06、[Issue #15 コメント 6676583842](https://github.com/YoonByungjin/catticket/issues/15#issuecomment-6676583842)） |
| SC-04 | mobile | 未ログイン | [SC-04-mobile.png](SC-04/SC-04-mobile.png) / [.html](SC-04/SC-04-mobile.html) | `83aca223df534fc3818d604fd9c6e38e` | 1 / 0 | 2026-11-06 | 承認（2026-11-06、[Issue #15 コメント 6676583842](https://github.com/YoonByungjin/catticket/issues/15#issuecomment-6676583842)） |
| SC-13 | desktop | 管理者 | [SC-13-desktop.png](SC-13/SC-13-desktop.png) / [.html](SC-13/SC-13-desktop.html) | `4a355528d409420e8590edd2af9bf368` | 1 / 0 | 2026-11-06 | 承認（2026-11-06、[Issue #15 コメント 6676583842](https://github.com/YoonByungjin/catticket/issues/15#issuecomment-6676583842)） |

- 生成・編集の合計: `create_project` 1、`create_design_system` 1、`generate_screen_from_text` 7、`edit_screens` 4。`generate_variants` は使っていない。
- 途中の版（Stitch には残っている）: SC-01 desktop `2479b3a67de846788a771d216feab6cc`（生成）→ `ab7cf3505200450697c2bd18a151521b`（編集1）。SC-02 mobile `68ed8fd15ed24d7ab1cf0bbfebd55ff2`（生成）。SC-03 mobile `8aaa211f07c74c548c1eeecd913d819e`（生成）。リポジトリには最後の版だけを置いた。
- 編集の理由: SC-01 desktop はアイコンのフォントが読み込まれず、アイコンの名前の文字（`calendar_today` など）が出ていた（編集1）。編集1の後、区・ジャンルの選択欄のアイコンが誤ってカレンダーになった（編集2）。SC-02 mobile・SC-03 mobile はスマホの指定で生成したのに PC の幅の画面になった（編集1で 390px の幅に直した）。
- 試作の文言・数値（公演名・会場名・件数など）は架空のサンプル。

## 確認したこと

- **フォント**: 最初の試作（SC-04 mobile）の HTML で、見出し・本文の `font-family` が `Noto Sans KR` であることを確かめた（07 の 2.2.1 の確認の基準）。ほかの6つも `Noto Sans KR`。
- **02 にない要素**: 7つすべてで、02 の項目と共通部品（CM-01・CM-02）以外の要素がないことを画像で確かめた。プロンプトで検索欄・戻るボタン・下のタブバー・写真・購入ボタンなどを禁止し、生成結果にも出なかった。Stitch の返した `suggestion`（ダークモード、予約ボタンを画面の下に固定する、など）は採用していない。
- 地図・ポスターは灰色の枠（写真なし）。PNG はすべて 1MB 未満。

## 人間の確認

- **承認（2026-11-06、[Issue #15 コメント 6676583842](https://github.com/YoonByungjin/catticket/issues/15#issuecomment-6676583842)）**。7つすべて採用。編集・作り直し・別案（variants）はなし。
- 下の「07 との差」は、**実装で 07 に従う**ことで承認された。試作は直さない。
- SC-02 desktop の右の列（「周辺の駅・コインロッカー」だけで下が大きく空いている）は、試作を編集せず、07 の 2.4.1 に PC の配置を決めた。
- ダーク表示（F-36）の試作は作らない（同コメントの 6.）。色は 07 の 2.1.3〜2.1.5。

## 07 との差（実装は 07 に従う）

| 試作 | 差 |
|---|---|
| すべての試作 | ヘッダーにライト表示・ダーク表示の切り替え（F-36）がない（試作の後に 02 の CM-01 に追加された）。07 の 3.・4.1 に従う |
| すべての mobile | 言語の切り替えのボタンの高さが約 28px（07 の 2.6 では 48px にする）。ヘッダーのサービス名の色が `primary`（07 は指定なし。許容） |
| SC-01 mobile | キーワードの入力欄に虫眼鏡のアイコンがある（飾り。02 の要素は増えていない）。地図の中央の仮の文字に印が重なっている（仮の表示のため問題なし） |
| SC-01 desktop | 場所の方法の文字が 12px（07 は `label-large` 14px）。チェックボックスの見た目 20px（Material では押せる範囲 48px） |
| SC-02 mobile | 見出しが太さ 700（07 は 400 / 500 だけ）。カードの角丸 16px（07 は 12px）。「리뷰 쓰기」の高さ 36px・ページ送りのボタン 32px（07 は 44px 以上） |
| SC-02 desktop・SC-13 | 選択中の言語のボタンが `primary` の塗り。人間の決定（コメント 6676583842 の 3.）で、選択中は背景 `primary-container`・文字 `primary`（薄い藍色）にする。07 の 3. に従う |
| SC-02 desktop | 右の列が「周辺」だけで、お気に入りのボタンが公演名の下にある。07 の 2.4.1（右の列の上に操作の枠、右の列は `sticky`）に従う |
| SC-02 mobile・desktop | 人気順位が「주간·전체 3위」のラベルだけで、集計期間がない（02 の P-12 は集計期間と一緒に表示）。07 の 4.6 に従う |
| SC-13 | 「숨김」でない行を「—」で表示（02 は「非表示かどうか」。表示の形は 07 で決めていない。実装で決める） |

## 使ったプロンプト

Stitch に送ったプロンプトは、stitch-workflow スキルの 2.3 の形（英語、文言は韓国語、要素を 02 のとおりに並べ、ほかの要素を禁止）。Stitch が書き換えた後の `prompt` は次のとおり（最後の版を作った操作のもの）。

<details>
<summary>SC-04 mobile（生成）</summary>

```
Material 3 (Angular Material) mobile login screen for "CatTicket" (performance-map web service in Seoul).
Strict constraints:
- Font: 'Noto Sans KR' for all text, weights 400 and 500 only.
- Colors: primary #4338CA, on-primary #FFFFFF, background #FFFFFF, text #1F2937, secondary text #4B5563, outline #D1D5DB, footer background #F9FAFB.
- Text fields: outlined with 4px corners (rounded-[4px]), 56px height, floating/outlined labels.
- Buttons: fully rounded (rounded-full), filled primary #4338CA, text #FFFFFF.
- Spacing: 4px unit (16px page side padding), touch targets at least 44px tall.
- Content column: max width 400px, centered horizontally.
- Negative constraints: No photos, no illustrations, no logo images, no social login buttons, no 'remember me' checkbox, no back button, no bottom navigation/tab bar, no divider lines with 'or', no ads.

Top to bottom elements strictly as listed:
1. Header bar: 56px height, white background, bottom border 1px solid #D1D5DB, flex items-center justify-between px-4:
   - Left: service name text "CatTicket" (font-medium text-lg text-primary #4338CA or #1F2937).
   - Right: segmented language toggle with two options: "한국어" (selected, active styling) and "日本語", followed by a hamburger menu icon button (44px touch target).
2. Page title: "로그인" (font-medium, 24px, text #1F2937, pt-6 pb-6).
3. Outlined text field: label "이메일 주소" (empty state, height 56px, rounded-[4px], border #D1D5DB).
4. Outlined text field: label "비밀번호" (password field with masked dots placeholder or empty, no visibility toggle icon, height 56px, rounded-[4px], border #D1D5DB).
5. Full-width filled primary button: "로그인" (height 48px or 52px, fully rounded rounded-full, bg-[#4338CA] text-white font-medium).
6. Three text links stacked vertically, each on its own 44px-tall row, color #4338CA, text-sm font-medium, flex items-center justify-center:
   - "회원가입"
   - "비밀번호를 잊으셨나요?"
   - "잠금 해제하기"
7. Footer: background #F9FAFB, 12px text #4B5563, px-4 py-6 border-t border-[#D1D5DB], containing exactly two lines:
   - "출처: (재)예술경영지원센터 공연예술통합전산망(www.kopis.or.kr)"
   - "서울교통공사 공공데이터(역 좌표·주소, 물품보관함 위치, 역명 다국어 표기 – 공공누리 제1유형)"

State: normal (empty form, nothing entered, no error message). Clean Material 3 mobile presentation.
```
</details>

<details>
<summary>SC-01 mobile（生成）</summary>

```
MOBILE screen for Seoul performance-map web service 'CatTicket' ([SC-01 Map & Search (top page)]).
Built ONLY from Material 3 (Angular Material) components, strictly obeying all specified design tokens and exact Korean UI copy.
(以下、トークン（Noto Sans KR 400/500、primary #4338CA ほか）、ヘッダー、灰色の地図の枠と印 5 つ（3, 1, 2, 5, 1）、
キーワードの入力欄と「조건」、「근처 보기」、「출처」、下のシート（「지도 / 목록」、「검색 결과 128건」、カード 2 枚）を
クラス名付きで列挙し、最後に "Strictly do NOT add any unrequested elements (no bottom nav/tab bar, no back button,
no filter chips row, no category icons, no extra FAB, no ads, no avatar)." で終わる。Stitch の返した全文は約 4,000 字のため要約)
```
</details>

<details>
<summary>SC-01 desktop（編集2）</summary>

```
Fix only the trailing icons in the two select fields in the left panel:
1. In the select/input field showing "종로구, 마포구" (under 장소 / '구' tab), replace the trailing calendar SVG icon with a small downward chevron inline SVG: 20px by 20px, stroke/fill color #4B5563.
2. In the "장르" field showing "뮤지컬, 연극", replace the trailing calendar SVG icon with the exact same small downward chevron inline SVG (20px by 20px, color #4B5563).
3. Keep the calendar icon ONLY in the "기간" field ("2026-11-06 ~ 2026-11-30").
4. Keep everything else exactly identical: layout, text, colors, checkbox states, pins on the map, list items, paginator, header, and footer. Do not add or remove any other element.
```
</details>

<details>
<summary>SC-02 mobile（編集1）</summary>

```
Re-render this screen as a MOBILE screen (device_type: mobile, 390px wide frame).
1. Mobile layout: Content fills the full width of the mobile viewport with 16px side padding.
2. Content fidelity: Keep EVERY element, exact text, card order, and color exactly as in the source screen. Do NOT add or remove any elements.
   (ヘッダー、ポスター、公演名と「공연 중」、「즐겨찾기」、基本情報 8 行、평가·통계、예매、주변 역・물품보관함、리뷰 2 件とページ送り、フッターを列挙)
3. Icons: Render ALL icons strictly as crisp inline SVG elements (do NOT use Material Symbols or icon font ligatures).
```
</details>

<details>
<summary>SC-02 desktop（生成）・SC-13 desktop（生成）</summary>

Stitch の返した `prompt` は画面名だけだった: `CatTicket 공연 상세 (SC-02) Desktop`、`CatTicket 공연 목록 (관리) (SC-13) Desktop`。
</details>

<details>
<summary>SC-03 mobile（編集1）</summary>

```
Re-render this screen as a true MOBILE screen (device_type: mobile, 390px wide frame). The content must fill the full width of the mobile viewport with 16px horizontal padding (px-4). Remove any desktop wrapper/gray background outside the mobile frame. Ensure all elements, texts, tabs ('인기 순위', '인기 급상승', '장르별 트렌드'), period pills ('일간', '주간', '월간'), genre dropdown ('전체' with dropdown chevron SVG), date range note ('집계 기간: 2026-10-26 ~ 2026-11-01'), ranked items (1~4 cards with rank number, poster placeholder, title, venue, date, tags), footer, and top navigation bar are kept exactly the same with exact text, order, and colors. All icons must be rendered as clean inline SVG.
```
</details>

---

## 한국어

# 화면 시안(Google Stitch)

- 담당: ui-designer
- 작성일: 2026-11-06(Issue #15)
- 위치: **겉모습 참고**. 화면의 항목·조작·상태는 [02](../02-screens.md), 토큰은 [07](../07-ui-guide.md)이 기준(ADR 0015 결정 3). HTML은 Tailwind CDN과 Google Fonts를 불러오는 Stitch 출력이며 **구현(`frontend/`)에는 쓰지 않는다**(Tailwind는 ADR에 없음). 겉모습의 기준은 PNG와 07.

## Stitch 프로젝트

| 항목 | 값 |
|---|---|
| 프로젝트 | `projects/17389669384892123455`(제목 `catticket-ui`, PRIVATE) |
| 디자인 시스템 | `assets/10494663557269344773`(`CatTicket (07-ui-guide)`. primary `#4338CA`, tertiary `#B45309`, neutral `#1F2937`, `ROUND_TWELVE`, LIGHT, 글꼴 `NOTO_SANS`) |

같은 작업을 이어서 할 때는 새 프로젝트를 만들지 않고 위 ID를 쓴다.

## 시안 목록

시안 대상은 Issue #15 코멘트 6651238358의 결정에 있는 7개뿐. 상태는 모두 "보통(데이터 있음)"(SC-04는 "보통").

| 화면 ID | 단말 | 로그인 상태 | 파일 | Stitch 화면 ID(마지막 판) | 생성 / 편집 | 생성일 | 사람의 확인 |
|---|---|---|---|---|---|---|---|
| SC-01 | mobile | 비로그인 | [SC-01-mobile.png](SC-01/SC-01-mobile.png) / [.html](SC-01/SC-01-mobile.html) | `d57f0846cac34568adca9b5bd1a5eefe` | 1 / 0 | 2026-11-06 | 승인(2026-11-06, [Issue #15 코멘트 6676583842](https://github.com/YoonByungjin/catticket/issues/15#issuecomment-6676583842)) |
| SC-01 | desktop | 비로그인 | [SC-01-desktop.png](SC-01/SC-01-desktop.png) / [.html](SC-01/SC-01-desktop.html) | `62fc8968042c4191a00610e6124dc540` | 1 / 2 | 2026-11-06 | 승인(2026-11-06, [Issue #15 코멘트 6676583842](https://github.com/YoonByungjin/catticket/issues/15#issuecomment-6676583842)) |
| SC-02 | mobile | 일반 사용자 | [SC-02-mobile.png](SC-02/SC-02-mobile.png) / [.html](SC-02/SC-02-mobile.html) | `4ef838ab13174e1797cdbbed5b9d6590` | 1 / 1 | 2026-11-06 | 승인(2026-11-06, [Issue #15 코멘트 6676583842](https://github.com/YoonByungjin/catticket/issues/15#issuecomment-6676583842)) |
| SC-02 | desktop | 일반 사용자 | [SC-02-desktop.png](SC-02/SC-02-desktop.png) / [.html](SC-02/SC-02-desktop.html) | `51c79232379c40cbbf3353b2404ef555` | 1 / 0 | 2026-11-06 | 승인(2026-11-06, [Issue #15 코멘트 6676583842](https://github.com/YoonByungjin/catticket/issues/15#issuecomment-6676583842)) |
| SC-03 | mobile | 비로그인 | [SC-03-mobile.png](SC-03/SC-03-mobile.png) / [.html](SC-03/SC-03-mobile.html) | `2ec6737638dc457fb3ebef9f1602903a` | 1 / 1 | 2026-11-06 | 승인(2026-11-06, [Issue #15 코멘트 6676583842](https://github.com/YoonByungjin/catticket/issues/15#issuecomment-6676583842)) |
| SC-04 | mobile | 비로그인 | [SC-04-mobile.png](SC-04/SC-04-mobile.png) / [.html](SC-04/SC-04-mobile.html) | `83aca223df534fc3818d604fd9c6e38e` | 1 / 0 | 2026-11-06 | 승인(2026-11-06, [Issue #15 코멘트 6676583842](https://github.com/YoonByungjin/catticket/issues/15#issuecomment-6676583842)) |
| SC-13 | desktop | 관리자 | [SC-13-desktop.png](SC-13/SC-13-desktop.png) / [.html](SC-13/SC-13-desktop.html) | `4a355528d409420e8590edd2af9bf368` | 1 / 0 | 2026-11-06 | 승인(2026-11-06, [Issue #15 코멘트 6676583842](https://github.com/YoonByungjin/catticket/issues/15#issuecomment-6676583842)) |

- 생성·편집 합계: `create_project` 1, `create_design_system` 1, `generate_screen_from_text` 7, `edit_screens` 4. `generate_variants`는 쓰지 않았다.
- 중간 판(Stitch에는 남아 있음): SC-01 desktop `2479b3a67de846788a771d216feab6cc`(생성) → `ab7cf3505200450697c2bd18a151521b`(편집 1). SC-02 mobile `68ed8fd15ed24d7ab1cf0bbfebd55ff2`(생성). SC-03 mobile `8aaa211f07c74c548c1eeecd913d819e`(생성). 저장소에는 마지막 판만 두었다.
- 편집 이유: SC-01 desktop은 아이콘 글꼴이 로드되지 않아 아이콘 이름 글자(`calendar_today` 등)가 보였다(편집 1). 편집 1 뒤 구·장르 선택란의 아이콘이 잘못 달력이 되었다(편집 2). SC-02 mobile·SC-03 mobile은 스마트폰으로 지정해 생성했는데 PC 폭 화면이 되었다(편집 1에서 390px 폭으로 고쳤다).
- 시안의 문구·숫자(공연명·공연장명·건수 등)는 가상의 샘플.

## 확인한 것

- **글꼴**: 첫 시안(SC-04 mobile)의 HTML에서 제목·본문 `font-family`가 `Noto Sans KR`인 것을 확인했다(07의 2.2.1 확인 기준). 나머지 6개도 `Noto Sans KR`.
- **02에 없는 요소**: 7개 모두 02의 항목과 공통 부품(CM-01·CM-02) 이외의 요소가 없음을 이미지로 확인했다. 프롬프트에서 검색창·뒤로 가기 버튼·아래 탭 바·사진·구매 버튼 등을 금지했고 생성 결과에도 나오지 않았다. Stitch가 돌려준 `suggestion`(다크 모드, 예매 버튼을 화면 아래에 고정 등)은 채택하지 않았다.
- 지도·포스터는 회색 틀(사진 없음). PNG는 모두 1MB 미만.

## 사람의 확인

- **승인(2026-11-06, [Issue #15 코멘트 6676583842](https://github.com/YoonByungjin/catticket/issues/15#issuecomment-6676583842))**. 7개 모두 채택. 편집·다시 만들기·다른 안(variants)은 없음.
- 아래 "07과의 차이"는 **구현에서 07을 따르는** 것으로 승인되었다. 시안은 고치지 않는다.
- SC-02 desktop의 오른쪽 열("주변 역·물품보관함"뿐이라 아래가 크게 비어 있음)은 시안을 편집하지 않고, 07의 2.4.1에 PC 배치를 정했다.
- 다크 표시(F-36)의 시안은 만들지 않는다(같은 코멘트의 6.). 색은 07의 2.1.3~2.1.5.

## 07과의 차이(구현은 07을 따른다)

| 시안 | 차이 |
|---|---|
| 모든 시안 | 헤더에 라이트 표시·다크 표시 전환(F-36)이 없다(시안 뒤에 02의 CM-01에 추가됨). 07의 3.·4.1을 따른다 |
| 모든 mobile | 언어 전환 버튼의 높이가 약 28px(07의 2.6에서는 48px로 한다). 헤더의 서비스명 색이 `primary`(07에 지정 없음. 허용) |
| SC-01 mobile | 키워드 입력란에 돋보기 아이콘이 있다(장식. 02의 요소는 늘지 않음). 지도 가운데 임시 글자에 표시가 겹쳐 있다(임시 표시라 문제없음) |
| SC-01 desktop | 장소 방법 글자가 12px(07은 `label-large` 14px). 체크박스 겉모습 20px(Material에서는 누르는 범위 48px) |
| SC-02 mobile | 제목이 굵기 700(07은 400 / 500만). 카드 모서리 16px(07은 12px). "리뷰 쓰기" 높이 36px·페이지 넘김 버튼 32px(07은 44px 이상) |
| SC-02 desktop·SC-13 | 선택된 언어 버튼이 `primary` 채움. 사람의 결정(코멘트 6676583842의 3.)으로 선택된 쪽은 배경 `primary-container`·글자 `primary`(옅은 남색)로 한다. 07의 3.을 따른다 |
| SC-02 desktop | 오른쪽 열이 "주변"뿐이고, 즐겨찾기 버튼이 공연명 아래에 있다. 07의 2.4.1(오른쪽 열 위에 조작 틀, 오른쪽 열은 `sticky`)을 따른다 |
| SC-02 mobile·desktop | 인기 순위가 "주간·전체 3위" 라벨뿐이고 집계 기간이 없다(02의 P-12는 집계 기간과 함께 표시). 07의 4.6을 따른다 |
| SC-13 | "숨김"이 아닌 행을 "—"로 표시(02는 "숨김 여부". 표시 형태는 07에서 정하지 않았다. 구현에서 정한다) |

## 사용한 프롬프트

Stitch에 보낸 프롬프트는 stitch-workflow 스킬 2.3의 형식(영어, 문구는 한국어, 요소를 02대로 나열, 다른 요소 금지). Stitch가 고쳐 쓴 뒤의 `prompt`는 위 일본어 절의 "使ったプロンプト"에 있다(프롬프트 원문은 영어라서 한 번만 싣는다).
