# CatTicket 開発ルール

AI（Claude Code とそのサブエージェント）と人間が同じルールで開発するための規約。作業の前に必ず読むこと。

## プロダクト

**企画中。** テーマ（仮）: ソウルの公演情報を地図で探せる Web サービス。
内容は `docs/design/00-concept.md`（企画書）と `01-requirements.md`（要件定義書）で決める。それまで機能の実装はしない。

仕様の正（Single Source of Truth）は `docs/design/` の設計書。設計書にないことは実装しない。

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
| 0 | ハーネス構築 | ルール、エージェント、テンプレート、CI | エージェントがルールどおりに動く |
| 1 | 企画 | `00-concept.md` | 誰のどんな課題を解くかを一文で説明できる |
| 2 | 要件定義 | `01-requirements.md` | 全機能に優先度（Must / Should / Could）と受け入れ基準がある |
| 3 | 基本設計 | `02`〜`05`（画面・API・テーブル・バッチ）、ADR | 画面 → API → テーブルが漏れなくつながる |
| 4 | 詳細設計 | テーブル定義、API 仕様、クラス設計 | 実装者が質問せずに作れる粒度 |
| 5 | 実装 | Issue 単位の PR（コード + テスト） | CI 通過 + レビュー承認 |
| 6 | テスト | テスト計画、結合・E2E テスト | 要件の全項目を確認済み |
| 7 | リリース | デプロイ、運用手順 | 本番 URL で動作 |

前の工程の成果物が変わったら、後ろの成果物も見直す。

### エージェント体制

定義は `.claude/agents/`。メインの Claude は **PM・取りまとめ役**として作業を割り振り、結果を人間に報告する。

| エージェント | 工程 | 担当 | 書き込み |
|---|---|---|---|
| `requirements-analyst` | 1〜2 | 企画書・要件定義書 | `docs/design/00`, `01` |
| `architect` | 3〜4 | アーキテクチャ、画面・API・バッチ設計、ADR | `docs/design/02`, `03`, `05`, `docs/adr/` |
| `db-designer` | 3〜4 | ER 図、テーブル定義、Flyway SQL | `docs/design/04`, `db/migration/` |
| `backend-dev` | 5 | Spring Boot + MyBatis の実装と単体テスト | `backend/` |
| `frontend-dev` | 5 | Angular の実装とテスト | `frontend/` |
| `qa-engineer` | 6 | テスト計画、結合・E2E テスト | `docs/test/`, テストコード |
| `reviewer` | 全ゲート | 設計書・ルールに照らした確認 | **なし（読み取りのみ）** |

エージェント運用のルール:

- エージェント同士は会話ではなく**ドキュメントで受け渡す**。必要な前提はすべて設計書・ADR に書く。
- 作る役と確認する役を分ける。自分の成果物を自分でゲート通過させない。
- 設計書の矛盾・不足に気づいたら、推測で埋めずに**作業を止めて報告**する。
- 質問が多い工程（企画・要件定義）は、メインの Claude が人間と対話して情報を集め、エージェントは文書化を担当する。
- 作業の最後に「変更したファイル / 確認したこと / 未決事項」を報告する。

### Git の流れ

1. 設計書を書く・更新する
2. GitHub Issue を作る（受け入れ条件を書く）
3. ブランチを切る: `feature/{Issue番号}-{内容}`、設計書は `docs/{内容}`、環境は `chore/{内容}`
4. 作業 + テスト
5. PR を作る（`Closes #{Issue番号}`、確認した内容）
6. CI が通ることを確認
7. **人間がレビューしてマージ**

禁止事項:

- `main` に直接 push しない。AI はマージしない。
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

## コマンド

```bash
docker compose up -d                        # MySQL 起動（開発時は DB だけコンテナ）
cd backend && ./gradlew bootRun             # アプリ起動（http://127.0.0.1:8080）
cd backend && ./gradlew test                # テスト（MySQL の起動が必要）
docker compose --profile app up -d --build  # アプリもコンテナで起動
```

Windows（PowerShell）は `.\gradlew.bat`。
この開発 PC では Java の AF_UNIX ソケットがユーザーフォルダ配下で失敗するため、
ユーザー環境変数 `JDK_JAVA_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\jtmp` を設定済み。

## ドキュメントの言語

- 設計書・ADR・PR・Issue: 日本語（下に韓国語を併記する）
- コード中のコメント: 日本語（短く、「なぜ」を書く）
- コミットメッセージ: 英語（1 行目は命令形で 50 文字程度）
- 人間への報告・会話: 韓国語

---

# CatTicket 개발 규칙 (한국어)

AI(Claude Code와 서브 에이전트)와 사람이 같은 규칙으로 개발하기 위한 규약. 작업 전에 반드시 읽을 것.

## 프로덕트

**기획 중.** 테마(가칭): 서울의 공연 정보를 지도로 찾는 웹 서비스.
내용은 `docs/design/00-concept.md`(기획서)와 `01-requirements.md`(요건정의서)에서 정한다. 그 전까지 기능 구현은 하지 않는다.

사양의 기준(Single Source of Truth)은 `docs/design/`의 설계서. 설계서에 없는 것은 구현하지 않는다.

## 기술 스택 (확정)

| 구분 | 기술 |
|---|---|
| 백엔드 | Java 21, Spring Boot 4.1, MyBatis(JPA는 쓰지 않음), Flyway, Gradle |
| 프론트엔드 | Angular |
| DB | MySQL 8.4 (utf8mb4, Asia/Seoul) |
| 개발 환경 | Docker Compose(MySQL), VS Code, GitHub Actions |

선정 이유는 `docs/adr/`. 지도·외부 데이터 API는 요건정의 후에 정한다(ADR 0003·0004는 "제안" 상태).

## 개발 프로세스

### 공정과 관문

각 공정 끝에 **관문(ゲート)**을 둔다. `reviewer`의 확인과 **사람의 승인**이 없으면 다음 공정으로 가지 않는다.

| No. | 공정 | 산출물 | 관문 통과 조건 |
|---|---|---|---|
| 0 | 하네스 구축 | 규칙, 에이전트, 템플릿, CI | 에이전트가 규칙대로 동작 |
| 1 | 기획 | `00-concept.md` | 누구의 어떤 문제를 푸는지 한 문장으로 설명 가능 |
| 2 | 요건정의 | `01-requirements.md` | 모든 기능에 우선순위(Must/Should/Could)와 완료 기준 |
| 3 | 기본설계 | `02`~`05`(화면·API·테이블·배치), ADR | 화면 → API → 테이블이 빠짐없이 연결 |
| 4 | 상세설계 | 테이블 정의, API 사양, 클래스 설계 | 구현자가 질문 없이 만들 수 있는 수준 |
| 5 | 구현 | Issue 단위 PR (코드 + 테스트) | CI 통과 + 리뷰 승인 |
| 6 | 테스트 | 테스트 계획, 결합·E2E 테스트 | 요건 전 항목 확인 |
| 7 | 릴리스 | 배포, 운영 절차 | 실제 URL에서 동작 |

앞 공정의 산출물이 바뀌면 뒤 산출물도 다시 검토한다.

### 에이전트 체제

정의는 `.claude/agents/`. 메인 Claude는 **PM·조율자**로서 일을 나눠 맡기고 결과를 사람에게 보고한다.

| 에이전트 | 공정 | 담당 | 쓰기 권한 |
|---|---|---|---|
| `requirements-analyst` | 1~2 | 기획서·요건정의서 | `docs/design/00`, `01` |
| `architect` | 3~4 | 아키텍처, 화면·API·배치 설계, ADR | `docs/design/02`, `03`, `05`, `docs/adr/` |
| `db-designer` | 3~4 | ER 다이어그램, 테이블 정의, Flyway SQL | `docs/design/04`, `db/migration/` |
| `backend-dev` | 5 | Spring Boot + MyBatis 구현과 단위 테스트 | `backend/` |
| `frontend-dev` | 5 | Angular 구현과 테스트 | `frontend/` |
| `qa-engineer` | 6 | 테스트 계획, 결합·E2E 테스트 | `docs/test/`, 테스트 코드 |
| `reviewer` | 모든 관문 | 설계서·규칙 기준 검토 | **없음 (읽기 전용)** |

에이전트 운영 규칙:

- 에이전트끼리는 대화가 아니라 **문서로 주고받는다**. 필요한 전제는 모두 설계서·ADR에 쓴다.
- 만드는 역할과 확인하는 역할을 나눈다. 자기 산출물을 스스로 관문 통과시키지 않는다.
- 설계서의 모순·누락을 발견하면 추측으로 메우지 말고 **작업을 멈추고 보고**한다.
- 질문이 많은 공정(기획·요건정의)은 메인 Claude가 사람과 대화해 정보를 모으고, 에이전트는 문서화를 맡는다.
- 작업 끝에 "변경한 파일 / 확인한 것 / 미결 사항"을 보고한다.

### Git 흐름

1. 설계서 작성·갱신
2. GitHub Issue 생성 (완료 조건 작성)
3. 브랜치: `feature/{Issue번호}-{내용}`, 설계서는 `docs/{내용}`, 환경은 `chore/{내용}`
4. 작업 + 테스트
5. PR 생성 (`Closes #{Issue번호}`, 확인한 내용)
6. CI 통과 확인
7. **사람이 리뷰하고 머지**

금지 사항: `main` 직접 push 금지, AI는 머지하지 않음 / `legacy` 브랜치는 보관용, 변경·머지 금지 / 설계서와 다른 구현 금지(설계 변경은 설계서 먼저).

## 코딩 규약 (백엔드)

- **계층**: `Controller → Service → Mapper` 한 방향만. Controller는 요청·입력 검사·응답 변환만, Service는 업무 로직과 트랜잭션, Mapper는 MyBatis 인터페이스(SQL은 XML).
- **명명·타입**: 기능 단위 패키지 `com.catticket.{기능}`(기능명은 기본설계에서 결정). DB는 `snake_case`, Java는 `camelCase`. API 입출력은 `record` DTO. 생성자 주입.
- **SQL / Flyway**: 적용된 마이그레이션은 절대 수정 금지(새 `V{번호}`로 변경). 테이블·컬럼에 `COMMENT`. 외래 키·유일 제약은 DB에도 정의. MyBatis 바인딩은 `#{}`만(`${}` 금지).
- **보안**: 비밀 정보는 코드에 쓰지 않고 `.env` → 환경변수. 서버용 API 키는 프론트에 노출 금지. 브라우저에 노출되는 키는 제공처 관리 화면에서 사용 제한.

## 테스트

새 기능에는 반드시 테스트. Mapper는 실제 MySQL(`@MybatisTest`), Service는 목(mock) 단위 테스트, Controller는 `@WebMvcTest`. 외부 API를 실제로 호출하는 테스트는 쓰지 않고 테스트용 파일로 재현.

## 명령어

위 일본어 섹션과 같음. 이 PC는 Java AF_UNIX 소켓이 사용자 폴더 아래에서 실패하므로 사용자 환경변수 `JDK_JAVA_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\jtmp`를 설정해 둠.

## 문서 언어

- 설계서·ADR·PR·Issue: 일본어로 쓰고 **아래에 한국어를 함께 적는다**
- 코드 주석: 일본어 (짧게, "왜"를 쓴다)
- 커밋 메시지: 영어
- 사람에게 보고·대화: 한국어
