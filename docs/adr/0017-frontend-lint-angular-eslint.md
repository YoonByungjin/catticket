# 0017. フロントエンドの静的解析（lint）は angular-eslint（Angular の公式の方法）で行い、CI で実行する

- 日付: 2026-11-10
- 状態: 採用（2026-11-10、Issue #28 コメント 6759899229 の No.1。導入は Issue #29）
- 関係する要件: なし（開発の品質の仕組み。特定の機能要件には対応しない）。Issue #24 の人間の決定6（2026-11-10、Issue #24 コメント 6758551367: 「ESLint を Wave 2 の前に導入する。Angular の公式の方法（angular-eslint）を使う。architect が ADR を書き、人間が承認した後に frontend-dev が入れる」）。PR #25 の reviewer の指摘4（lint が CI にない。Angular CLI の標準の構成に ESLint がなく、入れるには ADR が要る）。Issue #28 の No.6

## 背景

- PR #25（Issue #24、フロントエンドの骨組み）で `frontend/` ができた。Angular は **22.2**（`@angular/core` `^22.2.0`、`@angular/cli` `^22.2.6`）、TypeScript は `~6.0.2`、単体テストは Vitest、`devDependencies` に Prettier（`^3.8.1`）がある。プロジェクト名は `catticket`、部品のセレクタの接頭辞（`angular.json` の `prefix`）は `app`。
- CI（`.github/workflows/ci.yml`）の frontend のジョブは `npm ci` → 単体テスト → ビルドだけで、lint がない。Angular CLI の新しいプロジェクトには lint の設定が入っておらず（`ng lint` を使うには lint の仕組みを足す必要がある）、新しいライブラリは ADR の後に入れる規則（CLAUDE.md）のため、PR #25 では入れなかった。
- Wave 2（地図・検索などの画面の実装）でコードが大きく増える前に、型の誤りになりにくい書き方・テンプレートの誤り・アクセシビリティの基本的な誤りを機械で見つけ、レビューの負担を減らしたい。人間は Angular の公式の方法（angular-eslint）を使うと決めた。
- Angular の公式ドキュメント（`ng lint`）は、「`ng lint` を使うには、lint の機能を持つパッケージを `ng add` で加える」と書き、設定の例に angular-eslint の builder（`@angular-eslint/builder:lint`）を示している（Angular 自身は lint の実装を持たない）。

## 決定

1. **ツール**: ESLint と **angular-eslint**（`angular-eslint` パッケージ。`@angular-eslint/eslint-plugin`・`eslint-plugin-template`・`template-parser`・`builder`・`schematics` を含む）と **typescript-eslint** を使う。ほかの ESLint のプラグイン・共有設定（import の並び、Prettier 連携、RxJS の規則など）は入れない（入れるときは新しい ADR）。
2. **入れ方**: `frontend/` で Angular CLI の公式の手順 **`npx ng add angular-eslint@22`** を実行する（プロジェクトが1つで lint の設定がないので、ng add が `add-eslint-to-project` を続けて実行し、`frontend/eslint.config.js`・`angular.json` の `lint` ターゲット・`package.json` の `lint` スクリプト（`ng lint`）を作る）。生成されたファイルを土台にし、手で一から書かない。
3. **バージョン**:
   - angular-eslint の**メジャー番号は Angular のメジャー番号と同じ 22** にする（angular-eslint の方針: v12 以降、メジャーを Angular にそろえる）。確認日 2026-11-10 の最新は **22.6.0**。
   - ng add が `package.json` に書く指定をそのまま使う（`angular-eslint` は `^` なしの固定、`eslint`・`@eslint/js` は `^` 付き、`typescript-eslint` は angular-eslint が使う版）。`package-lock.json` をコミットする。
   - 確認日 2026-11-10 の組み合わせ: `angular-eslint` 22.6.0 の peer は `eslint` `^9.0.0 || ^10.0.0`・`typescript-eslint` `^8.0.0`。`typescript-eslint` 8.72.2 の peer は `typescript` `>=4.8.4 <6.1.0` で、PR #25 の TypeScript `~6.0.2` を含む。`eslint` の最新は 10.14.0（Node.js `^20.19.0 || ^22.13.0 || >=24`。CI の `lts/*` を満たす）。
   - Angular のメジャーを上げるときは、angular-eslint も同じメジャーに `ng update` で上げる（同じ PR）。TypeScript を上げて typescript-eslint の peer の範囲を外れるときは、typescript-eslint を上げてから TypeScript を上げる。
4. **設定（ルールセット）**: ESLint の新しい形式の設定ファイル（flat config）`frontend/eslint.config.js` だけを使う（旧形式の `.eslintrc*` は angular-eslint が対応をやめたので作らない）。中身は schematic が作る標準の構成（`eslint/config` の `defineConfig` で書かれる。typescript-eslint のプリセットは schematic の既定の `recommended` で、プリセットを選ぶオプション（`tseslintPreset`）は指定しない）のまま:
   - `**/*.ts`: ESLint の推奨（`@eslint/js` の recommended）、typescript-eslint の `recommended` と `stylistic`、angular-eslint の `tsRecommended`。インラインのテンプレートにもテンプレートのルールを効かせる `processInlineTemplates` を付ける。
   - `**/*.html`: angular-eslint の `templateRecommended` と `templateAccessibility`。
   - プロジェクトに合わせる設定は、セレクタの規則だけ: `@angular-eslint/component-selector`（要素、接頭辞 `app`、`kebab-case`）、`@angular-eslint/directive-selector`（属性、接頭辞 `app`、`camelCase`）。schematic が `angular.json` の `prefix` から作る値のまま。
   - 型の情報を使うルールセット（typescript-eslint の `recommendedTypeChecked` など）は今回は使わない（標準の構成から外れ、lint が遅くなる。必要になったら新しい ADR）。
5. **ルールの重さと例外**:
   - 上のルールセットの重さ（error / warn）は変えない。CI では**警告も失敗にする**（`angular.json` の `lint` ターゲットの `options` に `"maxWarnings": 0`）。警告がたまって読まれなくなるのを防ぐため。
   - ルールを止める・重さを変えるときは、`eslint.config.js` に理由のコメント（日本語）を付け、PR で reviewer の確認を受ける。
   - コードの中で止めるときは、`// eslint-disable-next-line {ルール名}` と同じ行か直前の行に理由のコメントを書く（1行ごと。ファイル全体の `eslint-disable` とルール名のない `eslint-disable` は使わない）。
6. **対象**: `frontend/src/` の `*.ts`（`*.spec.ts` を含む）と `*.html`（`lint` ターゲットの `lintFilePatterns` は `["src/**/*.ts", "src/**/*.html"]`。schematic の既定）。`frontend/e2e/`（qa-engineer の担当。E2E のツールはまだ決めていない）は今回は対象にしない。E2E を入れるときに、対象に加えるかを決める。生成物（`dist/`・`.angular/`・`coverage/`・`node_modules/`）は対象にしない。
7. **書式（フォーマット）**: 書式は ESLint で扱わない（angular-eslint は書式のルールを持たない方針。上のルールセットにも書式のルールはない）。PR #25 にある Prettier はそのまま使えるが、Prettier の確認を CI に入れるかはこの ADR の範囲外とする。`eslint-config-prettier` などの連携のパッケージは入れない（書式のルールが重ならないので不要）。
8. **CI**: `.github/workflows/ci.yml` の frontend のジョブに、`npm ci` の後・単体テストの前に `Lint` の手順（`npm run lint`）を加える。lint に失敗したら CI は失敗し、PR はマージしない。ESLint のキャッシュは使わない（CI では毎回すべて確認する）。
9. **担当と時期**: Wave 2 の前に、別の Issue（PM が作る）で frontend-dev が入れる（この ADR の承認の後）。
   - frontend-dev: `frontend/` の変更（ng add、`angular.json` の `maxWarnings`、PR #25 のコードの違反の修正）。**違反は導入の PR ですべて直す**（ルールを止めて済ませない。直せない・設計書と食い違う違反があれば、止めて architect に報告する）。
   - 実行 Claude: `.github/workflows/ci.yml` の手順の追加（`.github/` は実行 Claude の担当。CLAUDE.md の役割分担）。同じ PR に含める。
   - 受け入れの確認: `frontend/` で `npm run lint` がエラー・警告0件で終わる。CI の frontend のジョブに Lint の手順があり、通る。

## 検討した選択肢

| 選択肢 | 内容 | 判断 |
|---|---|---|
| **angular-eslint（ESLint + typescript-eslint）** | Angular の公式ドキュメントが lint の方法として案内する。`ng add` と `ng lint` で Angular CLI に組み込まれる。テンプレート（HTML・インラインのテンプレート）とアクセシビリティのルールがある。メジャーが Angular とそろう | **採用を提案。** 人間の決定6 と一致する |
| lint を入れない（型の確認とテストだけ） | TypeScript の `strict` とビルドで型の誤りは見つかる | テンプレートの誤り・アクセシビリティの基本的な誤り・書き方のばらつきは見つからない。人間の決定6 に合わない |
| Biome など ESLint 以外のツール | 速い。書式と lint を1つで扱える | Angular のテンプレートのルールを持たない。Angular の公式の方法ではない |
| ESLint を手で設定する（ng add を使わない） | 必要なプラグインだけを手で入れる | `angular.json` の `lint` ターゲット・版のそろえ方を自分で保守する必要がある。公式の手順から外れ、frontend-dev の判断が増える |

## 結果（良い点・注意点）

### 費用（月0円の範囲か）

| 項目 | 内容 |
|---|---|
| 無料枠 | オープンソース（angular-eslint・ESLint・typescript-eslint は MIT）。料金はない |
| カード登録 | 不要 |
| 上限を超えたとき | 使用量の上限はない（開発のときと CI だけで動き、配布物に含まれない） |
| 上限・アラートの設定（T-01） | 課金がないため不要 |

### 良い点

- Wave 2 の前に入れるので、違反の修正が PR #25 の小さいコードだけで済む。
- テンプレートのアクセシビリティのルール（`templateAccessibility`）で、02 の 3.1 などで決めた `aria-label` の付け忘れのような誤りの一部を機械で見つけられる。
- `ng update` で Angular と一緒に上げられ、版の組み合わせを自分で調べる手間が少ない。

### 注意点

- `devDependencies` が増え、`npm ci` と CI の時間が少し延びる。
- angular-eslint の版は ng add で固定（`^` なし）になるので、Angular のマイナーの更新では上がらない。Angular のメジャーを上げる PR で一緒に上げる（決定3）。
- 01 の R-61 のとおり、アクセシビリティの基準（WCAG など）は要件ではない。`templateAccessibility` は誤りを減らすための補助で、基準を満たすことの確認ではない。
- ルールの既定は angular-eslint・typescript-eslint の版の更新で変わることがある。上げる PR で新しい違反が出たら、その PR で直す。

### 人間が決めること

- この ADR を採用するか（状態を「採用」にするか）。
- 導入の Issue の作成（PM）。

**決まったこと（2026-11-10、Issue #28 コメント 6759899229）**:

- No.1: この ADR を**採用**した（決定1〜9 のとおり。変更なし）。導入は **Issue #29**（決定9 の「別の Issue」）で frontend-dev が行う。
- No.2: `.github/workflows/ci.yml` の frontend のジョブの `Lint` の手順（`npm ci` の後に `npm run lint`）は、Issue #29 の PR の中で実行 Claude が加える（決定8・決定9 のとおり）。`CLAUDE.md` は変えない。

### 確認した公式ドキュメント（確認日: 2026-11-10）

- angular-eslint の README（`ng add angular-eslint`、`add-eslint-to-project`、メジャーを Angular にそろえる方針、ESLint v9 以降・flat config だけ、書式のルールを持たない）: https://github.com/angular-eslint/angular-eslint
- Angular の `ng lint`（lint の機能を持つパッケージを `ng add` で加える、builder の例）: https://angular.dev/cli/lint
- angular-eslint の設定の説明（`tsRecommended`・`processInlineTemplates`・`templateRecommended`・`templateAccessibility`、セレクタの規則の例）: https://github.com/angular-eslint/angular-eslint/blob/main/docs/CONFIGURING_ESLINT.md
- schematic が作る設定と lint ターゲット（`defineConfig`、`recommended` の構成、`tseslintPreset`、builder `@angular-eslint/builder:lint`、`lintFilePatterns` は `{sourceRoot}/**/*.ts`・`{sourceRoot}/**/*.html`）: https://github.com/angular-eslint/angular-eslint/blob/main/packages/schematics/src/utils.ts
- ng add の処理（`angular-eslint` を固定の版、`eslint`・`@eslint/js` を `^` 付きで追加、`lint` スクリプト、1プロジェクトで lint ターゲットがないときに `add-eslint-to-project` を実行）: https://github.com/angular-eslint/angular-eslint/blob/main/packages/schematics/src/ng-add/index.ts
- lint の builder（`@angular-eslint/builder:lint`）のオプション（`lintFilePatterns`、`maxWarnings`（既定 -1）など）: https://github.com/angular-eslint/angular-eslint/blob/main/packages/builder/src/schema.json
- `angular-eslint` 22.6.0 の依存と peer: https://registry.npmjs.org/angular-eslint/latest
- `typescript-eslint` 8.72.2 の peer（TypeScript の範囲）: https://registry.npmjs.org/typescript-eslint/latest
- `eslint` 10.14.0 の Node.js の範囲: https://registry.npmjs.org/eslint/latest
- PR #25 の `frontend/package.json`・`frontend/angular.json`・`.github/workflows/ci.yml`（ブランチ `feature/24-frontend-skeleton`）

---

## 한국어

### 0017. 프론트엔드 정적 분석(lint)은 angular-eslint(Angular 공식 방법)로 하고 CI에서 실행한다

- 날짜: 2026-11-10
- 상태: 채택(2026-11-10, Issue #28 코멘트 6759899229의 No.1. 도입은 Issue #29)
- 관련 요건: 없음(개발 품질의 장치. 특정 기능 요건에는 대응하지 않는다). Issue #24의 사람의 결정 6(2026-11-10, Issue #24 코멘트 6758551367: "ESLint를 Wave 2 전에 도입한다. Angular 공식 방법(angular-eslint)을 쓴다. architect가 ADR을 쓰고 사람이 승인한 뒤 frontend-dev가 넣는다"). PR #25 reviewer 지적 4(lint가 CI에 없다. Angular CLI 표준 구성에 ESLint가 없고, 넣으려면 ADR이 필요). Issue #28의 No.6

### 배경

- PR #25(Issue #24, 프론트엔드 골격)에서 `frontend/`가 생겼다. Angular는 **22.2**(`@angular/core` `^22.2.0`, `@angular/cli` `^22.2.6`), TypeScript는 `~6.0.2`, 단위 테스트는 Vitest, `devDependencies`에 Prettier(`^3.8.1`)가 있다. 프로젝트 이름은 `catticket`, 부품 셀렉터 접두사(`angular.json`의 `prefix`)는 `app`.
- CI(`.github/workflows/ci.yml`)의 frontend 잡은 `npm ci` → 단위 테스트 → 빌드뿐이고 lint가 없다. Angular CLI의 새 프로젝트에는 lint 설정이 들어 있지 않고(`ng lint`를 쓰려면 lint 장치를 더해야 한다), 새 라이브러리는 ADR 뒤에 넣는 규칙(CLAUDE.md) 때문에 PR #25에서는 넣지 않았다.
- Wave 2(지도·검색 등 화면 구현)에서 코드가 크게 늘기 전에, 타입 오류가 나기 쉬운 작성법·템플릿 오류·접근성의 기본적인 오류를 기계로 찾아 리뷰 부담을 줄이고 싶다. 사람은 Angular 공식 방법(angular-eslint)을 쓰기로 정했다.
- Angular 공식 문서(`ng lint`)는 "`ng lint`를 쓰려면 lint 기능을 가진 패키지를 `ng add`로 더한다"고 쓰고, 설정 예에 angular-eslint의 builder(`@angular-eslint/builder:lint`)를 보여 준다(Angular 자체는 lint 구현을 갖지 않는다).

### 결정

1. **도구**: ESLint와 **angular-eslint**(`angular-eslint` 패키지. `@angular-eslint/eslint-plugin`·`eslint-plugin-template`·`template-parser`·`builder`·`schematics` 포함)와 **typescript-eslint**를 쓴다. 그 밖의 ESLint 플러그인·공유 설정(import 순서, Prettier 연동, RxJS 규칙 등)은 넣지 않는다(넣을 때는 새 ADR).
2. **넣는 방법**: `frontend/`에서 Angular CLI 공식 절차 **`npx ng add angular-eslint@22`**를 실행한다(프로젝트가 하나이고 lint 설정이 없으므로 ng add가 이어서 `add-eslint-to-project`를 실행해 `frontend/eslint.config.js`·`angular.json`의 `lint` 타깃·`package.json`의 `lint` 스크립트(`ng lint`)를 만든다). 생성된 파일을 바탕으로 하고, 손으로 처음부터 쓰지 않는다.
3. **버전**:
   - angular-eslint의 **메이저 번호는 Angular의 메이저 번호와 같은 22**로 한다(angular-eslint 방침: v12 이후 메이저를 Angular에 맞춘다). 확인일 2026-11-10의 최신은 **22.6.0**.
   - ng add가 `package.json`에 쓰는 지정을 그대로 쓴다(`angular-eslint`는 `^` 없는 고정, `eslint`·`@eslint/js`는 `^` 붙임, `typescript-eslint`는 angular-eslint가 쓰는 판). `package-lock.json`을 커밋한다.
   - 확인일 2026-11-10의 조합: `angular-eslint` 22.6.0의 peer는 `eslint` `^9.0.0 || ^10.0.0`·`typescript-eslint` `^8.0.0`. `typescript-eslint` 8.72.2의 peer는 `typescript` `>=4.8.4 <6.1.0`으로 PR #25의 TypeScript `~6.0.2`를 포함한다. `eslint` 최신은 10.14.0(Node.js `^20.19.0 || ^22.13.0 || >=24`. CI의 `lts/*`를 충족).
   - Angular 메이저를 올릴 때는 angular-eslint도 같은 메이저로 `ng update`로 올린다(같은 PR). TypeScript를 올려 typescript-eslint의 peer 범위를 벗어날 때는 typescript-eslint를 먼저 올리고 TypeScript를 올린다.
4. **설정(규칙 세트)**: ESLint 새 형식 설정 파일(flat config) `frontend/eslint.config.js`만 쓴다(구형식 `.eslintrc*`는 angular-eslint가 지원을 끝냈으므로 만들지 않는다). 내용은 schematic이 만드는 표준 구성(`eslint/config`의 `defineConfig`로 쓰인다. typescript-eslint 프리셋은 schematic 기본값 `recommended`이며, 프리셋을 고르는 옵션(`tseslintPreset`)은 지정하지 않는다) 그대로:
   - `**/*.ts`: ESLint 권장(`@eslint/js`의 recommended), typescript-eslint의 `recommended`와 `stylistic`, angular-eslint의 `tsRecommended`. 인라인 템플릿에도 템플릿 규칙을 적용하는 `processInlineTemplates`를 붙인다.
   - `**/*.html`: angular-eslint의 `templateRecommended`와 `templateAccessibility`.
   - 프로젝트에 맞추는 설정은 셀렉터 규칙뿐: `@angular-eslint/component-selector`(요소, 접두사 `app`, `kebab-case`), `@angular-eslint/directive-selector`(속성, 접두사 `app`, `camelCase`). schematic이 `angular.json`의 `prefix`로 만드는 값 그대로.
   - 타입 정보를 쓰는 규칙 세트(typescript-eslint의 `recommendedTypeChecked` 등)는 이번에는 쓰지 않는다(표준 구성에서 벗어나고 lint가 느려진다. 필요해지면 새 ADR).
5. **규칙의 무게와 예외**:
   - 위 규칙 세트의 무게(error / warn)는 바꾸지 않는다. CI에서는 **경고도 실패로 한다**(`angular.json`의 `lint` 타깃 `options`에 `"maxWarnings": 0`). 경고가 쌓여 읽히지 않게 되는 것을 막기 위해.
   - 규칙을 끄거나 무게를 바꿀 때는 `eslint.config.js`에 이유 주석(일본어)을 달고, PR에서 reviewer 확인을 받는다.
   - 코드 안에서 끌 때는 `// eslint-disable-next-line {규칙 이름}`과 같은 줄이나 바로 앞 줄에 이유 주석을 쓴다(한 줄씩. 파일 전체 `eslint-disable`과 규칙 이름 없는 `eslint-disable`은 쓰지 않는다).
6. **대상**: `frontend/src/`의 `*.ts`(`*.spec.ts` 포함)와 `*.html`(`lint` 타깃의 `lintFilePatterns`는 `["src/**/*.ts", "src/**/*.html"]`. schematic 기본값). `frontend/e2e/`(qa-engineer 담당. E2E 도구는 아직 정하지 않음)는 이번에는 대상으로 하지 않는다. E2E를 넣을 때 대상에 더할지 정한다. 생성물(`dist/`·`.angular/`·`coverage/`·`node_modules/`)은 대상으로 하지 않는다.
7. **서식(포맷)**: 서식은 ESLint로 다루지 않는다(angular-eslint는 서식 규칙을 갖지 않는 방침. 위 규칙 세트에도 서식 규칙은 없다). PR #25에 있는 Prettier는 그대로 쓸 수 있지만, Prettier 확인을 CI에 넣을지는 이 ADR의 범위 밖으로 한다. `eslint-config-prettier` 같은 연동 패키지는 넣지 않는다(서식 규칙이 겹치지 않으므로 불필요).
8. **CI**: `.github/workflows/ci.yml`의 frontend 잡에서 `npm ci` 뒤·단위 테스트 앞에 `Lint` 단계(`npm run lint`)를 더한다. lint가 실패하면 CI는 실패하고 PR은 머지하지 않는다. ESLint 캐시는 쓰지 않는다(CI에서는 매번 전부 확인한다).
9. **담당과 시기**: Wave 2 전에 별도 Issue(PM이 만든다)에서 frontend-dev가 넣는다(이 ADR 승인 뒤).
   - frontend-dev: `frontend/` 변경(ng add, `angular.json`의 `maxWarnings`, PR #25 코드의 위반 수정). **위반은 도입 PR에서 모두 고친다**(규칙을 꺼서 넘기지 않는다. 고칠 수 없거나 설계서와 어긋나는 위반이 있으면 멈추고 architect에게 보고한다).
   - 실행 Claude: `.github/workflows/ci.yml` 단계 추가(`.github/`는 실행 Claude 담당. CLAUDE.md의 역할 분담). 같은 PR에 넣는다.
   - 완료 확인: `frontend/`에서 `npm run lint`가 오류·경고 0건으로 끝난다. CI frontend 잡에 Lint 단계가 있고 통과한다.

### 검토한 선택지

| 선택지 | 내용 | 판단 |
|---|---|---|
| **angular-eslint(ESLint + typescript-eslint)** | Angular 공식 문서가 lint 방법으로 안내한다. `ng add`와 `ng lint`로 Angular CLI에 통합된다. 템플릿(HTML·인라인 템플릿)과 접근성 규칙이 있다. 메이저가 Angular와 맞춰진다 | **채택 제안.** 사람의 결정 6과 일치한다 |
| lint를 넣지 않는다(타입 확인과 테스트만) | TypeScript `strict`와 빌드로 타입 오류는 찾을 수 있다 | 템플릿 오류·접근성의 기본 오류·작성법의 편차는 못 찾는다. 사람의 결정 6과 맞지 않는다 |
| Biome 등 ESLint 이외의 도구 | 빠르다. 서식과 lint를 하나로 다룬다 | Angular 템플릿 규칙이 없다. Angular 공식 방법이 아니다 |
| ESLint를 손으로 설정(ng add를 쓰지 않음) | 필요한 플러그인만 손으로 넣는다 | `angular.json`의 `lint` 타깃·판 맞추기를 직접 유지해야 한다. 공식 절차에서 벗어나고 frontend-dev의 판단이 늘어난다 |

### 결과 (좋은 점·주의점)

#### 비용 (월 0원 범위인가)

| 항목 | 내용 |
|---|---|
| 무료 한도 | 오픈 소스(angular-eslint·ESLint·typescript-eslint는 MIT). 요금 없음 |
| 카드 등록 | 불필요 |
| 한도를 넘었을 때 | 사용량 상한이 없다(개발 때와 CI에서만 돌고 배포물에 포함되지 않는다) |
| 상한·알림 설정(T-01) | 과금이 없으므로 불필요 |

#### 좋은 점

- Wave 2 전에 넣으므로 위반 수정이 PR #25의 작은 코드만으로 끝난다.
- 템플릿 접근성 규칙(`templateAccessibility`)으로 02의 3.1 등에서 정한 `aria-label`을 빠뜨리는 것 같은 오류의 일부를 기계로 찾을 수 있다.
- `ng update`로 Angular와 함께 올릴 수 있어, 판 조합을 직접 조사하는 수고가 적다.

#### 주의점

- `devDependencies`가 늘어 `npm ci`와 CI 시간이 조금 늘어난다.
- angular-eslint 판은 ng add로 고정(`^` 없음)되므로 Angular 마이너 갱신으로는 올라가지 않는다. Angular 메이저를 올리는 PR에서 함께 올린다(결정 3).
- 01의 R-61대로 접근성 기준(WCAG 등)은 요건이 아니다. `templateAccessibility`는 오류를 줄이기 위한 보조이며, 기준 충족의 확인이 아니다.
- 규칙 기본값은 angular-eslint·typescript-eslint 판 갱신으로 바뀔 수 있다. 올리는 PR에서 새 위반이 나오면 그 PR에서 고친다.

#### 사람이 정할 것

- 이 ADR을 채택할지(상태를 "채택"으로 할지).
- 도입 Issue 작성(PM).

**정해진 것(2026-11-10, Issue #28 코멘트 6759899229)**:

- No.1: 이 ADR을 **채택**했다(결정 1~9대로. 변경 없음). 도입은 **Issue #29**(결정 9의 "별도 Issue")에서 frontend-dev가 한다.
- No.2: `.github/workflows/ci.yml` frontend 잡의 `Lint` 단계(`npm ci` 뒤에 `npm run lint`)는 Issue #29의 PR 안에서 실행 Claude가 더한다(결정 8·결정 9대로). `CLAUDE.md`는 바꾸지 않는다.

#### 확인한 공식 문서 (확인일: 2026-11-10)

- angular-eslint README(`ng add angular-eslint`, `add-eslint-to-project`, 메이저를 Angular에 맞추는 방침, ESLint v9 이후·flat config만, 서식 규칙을 갖지 않음): https://github.com/angular-eslint/angular-eslint
- Angular의 `ng lint`(lint 기능을 가진 패키지를 `ng add`로 더한다, builder 예): https://angular.dev/cli/lint
- angular-eslint 설정 설명(`tsRecommended`·`processInlineTemplates`·`templateRecommended`·`templateAccessibility`, 셀렉터 규칙 예): https://github.com/angular-eslint/angular-eslint/blob/main/docs/CONFIGURING_ESLINT.md
- schematic이 만드는 설정과 lint 타깃(`defineConfig`, `recommended` 구성, `tseslintPreset`, builder `@angular-eslint/builder:lint`, `lintFilePatterns`는 `{sourceRoot}/**/*.ts`·`{sourceRoot}/**/*.html`): https://github.com/angular-eslint/angular-eslint/blob/main/packages/schematics/src/utils.ts
- ng add 처리(`angular-eslint`를 고정 판, `eslint`·`@eslint/js`를 `^` 붙여 추가, `lint` 스크립트, 프로젝트 하나에 lint 타깃이 없을 때 `add-eslint-to-project` 실행): https://github.com/angular-eslint/angular-eslint/blob/main/packages/schematics/src/ng-add/index.ts
- lint builder(`@angular-eslint/builder:lint`) 옵션(`lintFilePatterns`, `maxWarnings`(기본 -1) 등): https://github.com/angular-eslint/angular-eslint/blob/main/packages/builder/src/schema.json
- `angular-eslint` 22.6.0의 의존성과 peer: https://registry.npmjs.org/angular-eslint/latest
- `typescript-eslint` 8.72.2의 peer(TypeScript 범위): https://registry.npmjs.org/typescript-eslint/latest
- `eslint` 10.14.0의 Node.js 범위: https://registry.npmjs.org/eslint/latest
- PR #25의 `frontend/package.json`·`frontend/angular.json`·`.github/workflows/ci.yml`(브랜치 `feature/24-frontend-skeleton`)
