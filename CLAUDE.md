# CatTicket 開発ルール

AI（Claude Code とそのサブエージェント）と人間が同じルールで開発するための規約。作業の前に必ず読むこと。

## プロダクト

**企画中。** テーマ（仮）: ソウルの公演情報を地図で探せる Web サービス。
内容は `docs/design/00-concept.md`（企画書）と `01-requirements.md`（要件定義書）で決める。それまで機能の実装はしない。

**仕様の正（Single Source of Truth）は `docs/design/` の設計書だけ。** 設計書にないことは実装しない。
`README.md`・`docs/kopis-api.md`・`legacy` ブランチは参考資料であり、仕様ではない。

## 技術スタック（確定）

| 区分 | 技術 |
|---|---|
| バックエンド | Java 21, Spring Boot 4.1, MyBatis（JPA は使わない）, Flyway, Gradle |
| フロントエンド | Angular |
| DB | MySQL 8.4（utf8mb4, Asia/Seoul） |
| 開発環境 | Docker Compose（MySQL）, VS Code, GitHub Actions |

選定理由は `docs/adr/`。地図・外部データ API は要件定義の後で決める（ADR 0003・0004 は「提案」）。
新しいライブラリ・方式を入れるときは ADR を追加する。

## 開発プロセス

### 工程とゲート

各工程の最後に**ゲート（関門）**を置く。`reviewer` の確認と**人間の承認**がなければ次の工程に進まない。

| No. | 工程 | 成果物 | ゲートの通過条件 |
|---|---|---|---|
| 0 | ハーネス構築 | ルール、エージェント、テンプレート、CI | ① 定義どうしに矛盾がない（reviewer 承認） ② 各エージェントを1回試し、担当外のファイルを変更せず、決められた形式で報告した ③ CI 通過 |
| 1 | 企画 | `00-concept.md` | 誰のどんな課題を解くかが一文で書かれ、スコープ（やる / やらない）と未決事項がある |
| 2 | 要件定義 | `01-requirements.md` | 全機能に ID・優先度（Must / Should / Could）・テストで確認できる受け入れ基準がある。未決事項が解消されている |
| 3 | 基本設計 | `02`〜`05`（画面・API・テーブル・バッチ）、ADR | 要件 ID → 画面 → API → テーブルの対応表に漏れ・余分がない |
| 4 | 詳細設計 | `04`（テーブル定義）、`03`（API 仕様の詳細）、`06-classes.md`（クラス設計） | 実装者が質問せずに作れる粒度 |
| 5 | 実装 | Issue 単位の PR（コード + テスト） | CI 通過 + reviewer 承認 + 人間のマージ |
| 6 | テスト | `docs/test/`、結合・E2E テスト | 要件の全項目の合否が出ている |
| 7 | リリース | デプロイ、`docs/ops/`（運用手順） | 本番 URL で動作する |

前の工程の成果物が変わったら、後ろの成果物も見直す。

### エージェント体制

定義は `.claude/agents/`。メインの Claude は **PM・取りまとめ役**として作業を割り振り、結果を人間に報告する。

| 担当 | 工程 | 役割 | 書き込んでよい場所 |
|---|---|---|---|
| `requirements-analyst` | 1〜2 | 企画書・要件定義書 | `docs/design/00-concept.md`, `01-requirements.md`, `docs/design/README.md` の状態欄 |
| `architect` | 3〜4 | アーキテクチャ、画面・API・バッチ・クラス設計、ADR | `docs/design/02`, `03`, `05`, `06`, `docs/design/README.md` の状態欄, `docs/adr/` |
| `db-designer` | 3〜4 | ER 図、テーブル定義、Flyway SQL | `docs/design/04-tables.md`, `docs/design/README.md` の状態欄, `backend/src/main/resources/db/migration/` |
| `backend-dev` | 5 | Spring Boot + MyBatis の実装と単体テスト | `backend/`（`db/migration/` と `src/test/java/com/catticket/it/` を除く） |
| `frontend-dev` | 5 | Angular の実装と単体テスト | `frontend/`（`frontend/e2e/` を除く）。`frontend/` を作る PR では `.github/workflows/ci.yml` に frontend ジョブを追加する |
| `qa-engineer` | 6 | テスト計画、結合・E2E テスト | `docs/test/`, `backend/src/test/java/com/catticket/it/`, `frontend/e2e/` |
| `reviewer` | 全ゲート | 設計書・ルールに照らした確認 | **なし（読み取りのみ）** |
| メインの Claude | 全工程 | 割り振り、Issue・ブランチ・PR の作成、push、CI の保守、リリース（工程7） | `.github/`, `CLAUDE.md`, `.claude/`, `compose.yaml`, `docs/ops/`, `README.md` |

テストの分担: **単体テスト**は実装したエージェントが書く（backend: `backend/src/test/java/com/catticket/{機能}/`、frontend: 実装の横の `*.spec.ts`）。**結合・E2E テスト**は qa-engineer が書く。

エージェント運用のルール:

- エージェント同士は会話ではなく**ドキュメントで受け渡す**。必要な前提はすべて設計書・ADR に書く。
- 作る役と確認する役を分ける。自分の成果物を自分でゲート通過させない。
- 設計書の矛盾・不足に気づいたら、推測で埋めずに**作業を止めて報告**する。
- 質問が多い工程（企画・要件定義）は、メインの Claude が人間と対話して情報を集め、エージェントは文書化を担当する。
- 作業の最後に「変更したファイル / 確認したこと / 未決事項」を報告する。

### Git の流れ

役割: **Issue・ブランチ・PR の作成と push はメインの Claude**。サブエージェントは、指示されたブランチでファイルを変更し、コミットまで行う（push しない）。**マージは人間**。

1. 設計書を書く・更新する
2. GitHub Issue を作る（要件 ID と受け入れ基準を書く）
3. ブランチを切る: 機能は `feature/{Issue番号}-{内容}`、設計書は `docs/{内容}`、環境・ルールは `chore/{内容}`
4. 作業 + テスト
5. PR を作る（`Closes #{Issue番号}`、確認した内容）。push する前に、そのブランチの PR がまだ開いているか確認する
6. CI が通ることを確認し、reviewer の確認を受ける
7. **人間がレビューしてマージ**

禁止事項:

- `main` に直接 push しない（ブランチ保護で禁止済み）。AI はマージしない。
- マージ済みの PR のブランチに追加で push しない。続きは新しいブランチ・PR で行う。
- `legacy` ブランチ（2025年の Django 初期版）は保存用。変更・マージしない。
- 設計書と違う実装をしない。設計を変えるときは先に設計書を更新する。

## コーディング規約（バックエンド）

### レイヤー

`Controller → Service → Mapper` の一方向のみ。逆方向・層の飛ばしは禁止。

- **Controller**: リクエストの受け取り・入力チェック（Bean Validation）・レスポンス変換のみ。業務ロジックを書かない。
- **Service**: 業務ロジックとトランザクション（`@Transactional`）。
- **Mapper**: MyBatis の `@Mapper` インターフェース。SQL は XML に書く（アノテーションに SQL を書かない）。

### 命名・型

- パッケージは機能単位（`com.catticket.{機能}`）。機能名は基本設計で決める。クラスは `{機能}Controller` / `{機能}Service` / `{機能}Mapper`。
- DB カラムは `snake_case`、Java フィールドは `camelCase`（`map-underscore-to-camel-case: true`）。
- API のリクエスト・レスポンスは Java の `record` で DTO を作る。DB の行をそのまま API に返さない。
- 依存性はコンストラクタ注入（フィールドの `@Autowired` は使わない）。

### SQL / Flyway

- 適用済みのマイグレーションファイルは**絶対に編集しない**。変更は新しい `V{次の番号}__*.sql` で行う。
- テーブル・カラムには `COMMENT` を付ける。
- 外部キー・一意制約は DB 側でも定義する（アプリだけに頼らない）。
- MyBatis の値の埋め込みは `#{}` のみ。`${}` は使わない（SQL インジェクション対策）。

### セキュリティ

- 秘密情報（DB パスワード、API キーなど）はコードに書かない。`.env` → 環境変数で渡す。
- サーバー用の API キーはフロントエンドに出さない。ブラウザに出るキーは提供元の管理画面で利用制限をかける。

## テスト

- 新しい機能には必ずテストを付ける。テストが通らない PR はマージしない。
- Mapper: 実際の MySQL に対してテスト（`@MybatisTest`）。
- Service: Mapper・外部 API をモックした単体テスト。
- Controller: `@WebMvcTest`。
- 外部 API を実際に呼ぶテストは書かない。レスポンスはテスト用ファイルで再現する。
- **テスト用 DB は `test_catticket`**（開発用の `catticket` を汚さない）。`./gradlew test` が DB 名を切り替え、DB がなければ作る。Flyway はテスト起動時にこの DB へマイグレーションを適用する。CI も同じ DB 名を使う。

## コマンド

```bash
docker compose up -d                        # MySQL 起動（開発時は DB だけコンテナ）
cd backend && ./gradlew bootRun             # アプリ起動（http://127.0.0.1:8080）。終了しないプロセス
cd backend && ./gradlew test                # テスト（MySQL の起動が必要。test_catticket を使う）
cd backend && ./gradlew build               # ビルド + テスト（CI と同じ）
docker compose --profile app up -d --build  # アプリもコンテナで起動
```

Windows（PowerShell）は `.\gradlew.bat`。
この開発 PC では Java の AF_UNIX ソケットがユーザーフォルダ配下で失敗するため、
ユーザー環境変数 `JDK_JAVA_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\jtmp` を設定済み。

## ドキュメント

### 言語

- 設計書・ADR・PR・Issue: 日本語（下に韓国語を併記する）
- コード中のコメント: 日本語（短く、「なぜ」を書く）
- コミットメッセージ: 英語（1 行目は命令形で 50 文字程度）
- 人間への報告・会話: 韓国語

### 用語

- **受け入れ基準**（완료 기준）: 要件・Issue が完了したと判断する条件。テストで確認できる文で書く。
- **要件 ID**: `F-01`（機能要件）、`N-01`（非機能要件）。画面・API・テーブル・テストはこの ID で要件と対応づける。

---

# CatTicket 개발 규칙 (한국어)

AI(Claude Code와 서브 에이전트)와 사람이 같은 규칙으로 개발하기 위한 규약. 작업 전에 반드시 읽을 것.

## 프로덕트

**기획 중.** 테마(가칭): 서울의 공연 정보를 지도로 찾는 웹 서비스.
내용은 `docs/design/00-concept.md`(기획서)와 `01-requirements.md`(요건정의서)에서 정한다. 그 전까지 기능 구현은 하지 않는다.

**사양의 기준(Single Source of Truth)은 `docs/design/`의 설계서뿐.** 설계서에 없는 것은 구현하지 않는다.
`README.md`·`docs/kopis-api.md`·`legacy` 브랜치는 참고 자료이며 사양이 아니다.

## 기술 스택 (확정)

| 구분 | 기술 |
|---|---|
| 백엔드 | Java 21, Spring Boot 4.1, MyBatis(JPA는 쓰지 않음), Flyway, Gradle |
| 프론트엔드 | Angular |
| DB | MySQL 8.4 (utf8mb4, Asia/Seoul) |
| 개발 환경 | Docker Compose(MySQL), VS Code, GitHub Actions |

선정 이유는 `docs/adr/`. 지도·외부 데이터 API는 요건정의 후에 정한다(ADR 0003·0004는 "제안").
새 라이브러리·방식을 도입할 때는 ADR을 추가한다.

## 개발 프로세스

### 공정과 관문

각 공정 끝에 **관문(ゲート)**을 둔다. `reviewer`의 확인과 **사람의 승인**이 없으면 다음 공정으로 가지 않는다.

| No. | 공정 | 산출물 | 관문 통과 조건 |
|---|---|---|---|
| 0 | 하네스 구축 | 규칙, 에이전트, 템플릿, CI | ① 정의 간 모순 없음(reviewer 승인) ② 각 에이전트를 한 번씩 시험해 담당 밖 파일을 바꾸지 않고 정해진 형식으로 보고함 ③ CI 통과 |
| 1 | 기획 | `00-concept.md` | 누구의 어떤 문제를 푸는지 한 문장으로 쓰여 있고, 범위(하는 것/안 하는 것)와 미결 사항이 있음 |
| 2 | 요건정의 | `01-requirements.md` | 모든 기능에 ID·우선순위(Must/Should/Could)·테스트로 확인 가능한 완료 기준이 있음. 미결 사항이 해소됨 |
| 3 | 기본설계 | `02`~`05`(화면·API·테이블·배치), ADR | 요건 ID → 화면 → API → 테이블 대응표에 누락·불필요한 것이 없음 |
| 4 | 상세설계 | `04`(테이블 정의), `03`(API 사양 상세), `06-classes.md`(클래스 설계) | 구현자가 질문 없이 만들 수 있는 수준 |
| 5 | 구현 | Issue 단위 PR (코드 + 테스트) | CI 통과 + reviewer 승인 + 사람의 머지 |
| 6 | 테스트 | `docs/test/`, 결합·E2E 테스트 | 요건 전 항목의 합격 여부가 나옴 |
| 7 | 릴리스 | 배포, `docs/ops/`(운영 절차) | 운영(프로덕션) URL에서 동작 |

앞 공정의 산출물이 바뀌면 뒤 산출물도 다시 검토한다.

### 에이전트 체제

정의는 `.claude/agents/`. 메인 Claude는 **PM·조율자**로서 일을 나눠 맡기고 결과를 사람에게 보고한다.

| 담당 | 공정 | 역할 | 써도 되는 곳 |
|---|---|---|---|
| `requirements-analyst` | 1~2 | 기획서·요건정의서 | `docs/design/00-concept.md`, `01-requirements.md`, `docs/design/README.md`의 상태란 |
| `architect` | 3~4 | 아키텍처, 화면·API·배치·클래스 설계, ADR | `docs/design/02`, `03`, `05`, `06`, `docs/design/README.md`의 상태란, `docs/adr/` |
| `db-designer` | 3~4 | ER 다이어그램, 테이블 정의, Flyway SQL | `docs/design/04-tables.md`, `docs/design/README.md`의 상태란, `backend/src/main/resources/db/migration/` |
| `backend-dev` | 5 | Spring Boot + MyBatis 구현과 단위 테스트 | `backend/` (`db/migration/`와 `src/test/java/com/catticket/it/` 제외) |
| `frontend-dev` | 5 | Angular 구현과 단위 테스트 | `frontend/` (`frontend/e2e/` 제외). `frontend/`를 만드는 PR에서 `.github/workflows/ci.yml`에 frontend 잡 추가 |
| `qa-engineer` | 6 | 테스트 계획, 결합·E2E 테스트 | `docs/test/`, `backend/src/test/java/com/catticket/it/`, `frontend/e2e/` |
| `reviewer` | 모든 관문 | 설계서·규칙 기준 검토 | **없음 (읽기 전용)** |
| 메인 Claude | 전 공정 | 작업 분배, Issue·브랜치·PR 생성, push, CI 관리, 릴리스(공정 7) | `.github/`, `CLAUDE.md`, `.claude/`, `compose.yaml`, `docs/ops/`, `README.md` |

테스트 분담: **단위 테스트**는 구현한 에이전트가 쓴다(backend: `backend/src/test/java/com/catticket/{기능}/`, frontend: 구현 옆의 `*.spec.ts`). **결합·E2E 테스트**는 qa-engineer가 쓴다.

에이전트 운영 규칙:

- 에이전트끼리는 대화가 아니라 **문서로 주고받는다**. 필요한 전제는 모두 설계서·ADR에 쓴다.
- 만드는 역할과 확인하는 역할을 나눈다. 자기 산출물을 스스로 관문 통과시키지 않는다.
- 설계서의 모순·누락을 발견하면 추측으로 메우지 말고 **작업을 멈추고 보고**한다.
- 질문이 많은 공정(기획·요건정의)은 메인 Claude가 사람과 대화해 정보를 모으고, 에이전트는 문서화를 맡는다.
- 작업 끝에 "변경한 파일 / 확인한 것 / 미결 사항"을 보고한다.

### Git 흐름

역할: **Issue·브랜치·PR 생성과 push는 메인 Claude**. 서브 에이전트는 지정된 브랜치에서 파일을 바꾸고 커밋까지만 한다(push하지 않음). **머지는 사람**.

1. 설계서 작성·갱신
2. GitHub Issue 생성 (요건 ID와 완료 기준 작성)
3. 브랜치: 기능은 `feature/{Issue번호}-{내용}`, 설계서는 `docs/{내용}`, 환경·규칙은 `chore/{내용}`
4. 작업 + 테스트
5. PR 생성 (`Closes #{Issue번호}`, 확인한 내용). push 전에 그 브랜치의 PR이 아직 열려 있는지 확인한다
6. CI 통과 확인, reviewer 확인
7. **사람이 리뷰하고 머지**

금지 사항:

- `main`에 직접 push하지 않는다(브랜치 보호로 금지됨). AI는 머지하지 않는다.
- 머지된 PR의 브랜치에 추가로 push하지 않는다. 이어지는 작업은 새 브랜치·PR로 한다.
- `legacy` 브랜치(2025년 Django 첫 버전)는 보관용. 변경·머지하지 않는다.
- 설계서와 다르게 구현하지 않는다. 설계를 바꿀 때는 먼저 설계서를 갱신한다.

## 코딩 규약 (백엔드)

### 계층

`Controller → Service → Mapper` 한 방향만. 역방향·계층 건너뛰기 금지.

- **Controller**: 요청 받기·입력 검사(Bean Validation)·응답 변환만. 업무 로직을 쓰지 않는다.
- **Service**: 업무 로직과 트랜잭션(`@Transactional`).
- **Mapper**: MyBatis `@Mapper` 인터페이스. SQL은 XML에 쓴다(어노테이션에 SQL을 쓰지 않음).

### 명명·타입

- 패키지는 기능 단위(`com.catticket.{기능}`). 기능명은 기본설계에서 정한다. 클래스는 `{기능}Controller` / `{기능}Service` / `{기능}Mapper`.
- DB 컬럼은 `snake_case`, Java 필드는 `camelCase` (`map-underscore-to-camel-case: true`).
- API 요청·응답은 Java `record`로 DTO를 만든다. DB 행을 그대로 API로 반환하지 않는다.
- 의존성은 생성자 주입(필드 `@Autowired`는 쓰지 않음).

### SQL / Flyway

- 이미 적용된 마이그레이션 파일은 **절대 수정하지 않는다**. 변경은 새 `V{다음 번호}__*.sql`로 한다.
- 테이블·컬럼에 `COMMENT`를 붙인다.
- 외래 키·유일 제약은 DB에도 정의한다(앱에만 의존하지 않음).
- MyBatis 값 바인딩은 `#{}`만 쓴다. `${}`는 쓰지 않는다(SQL 인젝션 방지).

### 보안

- 비밀 정보(DB 비밀번호, API 키 등)는 코드에 쓰지 않는다. `.env` → 환경변수로 전달.
- 서버용 API 키는 프론트엔드에 노출하지 않는다. 브라우저에 노출되는 키는 제공처 관리 화면에서 사용 제한을 건다.

## 테스트

- 새 기능에는 반드시 테스트를 붙인다. 테스트가 통과하지 않는 PR은 머지하지 않는다.
- Mapper: 실제 MySQL로 테스트(`@MybatisTest`).
- Service: Mapper·외부 API를 목(mock)으로 대체한 단위 테스트.
- Controller: `@WebMvcTest`.
- 외부 API를 실제로 호출하는 테스트는 쓰지 않는다. 응답은 테스트용 파일로 재현한다.
- **테스트용 DB는 `test_catticket`** (개발용 `catticket`을 더럽히지 않음). `./gradlew test`가 DB 이름을 바꾸고 DB가 없으면 만든다. Flyway는 테스트 시작 시 이 DB에 마이그레이션을 적용한다. CI도 같은 DB 이름을 쓴다.

## 명령어

```bash
docker compose up -d                        # MySQL 실행 (개발 중에는 DB만 컨테이너)
cd backend && ./gradlew bootRun             # 앱 실행 (http://127.0.0.1:8080). 끝나지 않는 프로세스
cd backend && ./gradlew test                # 테스트 (MySQL 실행 필요. test_catticket 사용)
cd backend && ./gradlew build               # 빌드 + 테스트 (CI와 같음)
docker compose --profile app up -d --build  # 앱도 컨테이너로 실행
```

Windows(PowerShell)는 `.\gradlew.bat`.
이 개발 PC는 Java의 AF_UNIX 소켓이 사용자 폴더 아래에서 실패하므로,
사용자 환경변수 `JDK_JAVA_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\jtmp`를 설정해 둠.

## 문서

### 언어

- 설계서·ADR·PR·Issue: 일본어로 쓰고 **아래에 한국어를 함께 적는다**
- 코드 주석: 일본어 (짧게, "왜"를 쓴다)
- 커밋 메시지: 영어 (첫 줄은 명령형, 50자 정도)
- 사람에게 보고·대화: 한국어

### 용어

- **완료 기준**(受け入れ基準): 요건·Issue가 끝났다고 판단하는 조건. 테스트로 확인할 수 있는 문장으로 쓴다.
- **요건 ID**: `F-01`(기능 요건), `N-01`(비기능 요건). 화면·API·테이블·테스트는 이 ID로 요건과 대응시킨다.
