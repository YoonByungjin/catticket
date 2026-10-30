# CatTicket 開発ルール

AI（Claude Code）と人間が同じルールで開発するための規約。実装・レビューの前に必ず読むこと。

## プロダクト

ソウルの公演（コンサート・ミュージカル）を地図に表示し、会場周辺の地下鉄コインロッカーを探せる Web サービス。
仕様の正（Single Source of Truth）は `docs/design/` の設計書。設計書にないことを実装しない。

## 技術スタック

| 区分 | 技術 |
|---|---|
| バックエンド | Java 21, Spring Boot 4.1, MyBatis（JPA は使わない）, Flyway, Gradle |
| フロントエンド | Angular, Google Maps JavaScript API |
| DB | MySQL 8.4（utf8mb4, Asia/Seoul） |
| データ取得 | KOPIS Open API（`docs/kopis-api.md`） |

技術選定の理由は `docs/adr/` を参照。新しいライブラリ・方式を入れるときは ADR を追加する。

## ディレクトリ

```
backend/src/main/java/com/catticket/
├── event/      公演（機能ごとのパッケージ）
├── venue/      会場
├── locker/     コインロッカー
├── kopis/      KOPIS API 連携・定期収集
└── common/     共通（例外処理、設定）
backend/src/main/resources/
├── db/migration/          Flyway（V{番号}__{内容}.sql）
└── mapper/{機能}/         MyBatis XML
docs/design/    設計書（要件定義・画面・API・テーブル・バッチ）
docs/adr/       技術選定の記録
```

## コーディング規約（バックエンド）

### レイヤー

`Controller → Service → Mapper` の一方向のみ。逆方向・層の飛ばしは禁止。

- **Controller**: リクエストの受け取り・入力チェック（Bean Validation）・レスポンス変換のみ。業務ロジックを書かない。
- **Service**: 業務ロジックとトランザクション（`@Transactional`）。
- **Mapper**: MyBatis の `@Mapper` インターフェース。SQL は XML に書く（アノテーションに SQL を書かない）。

### 命名・型

- パッケージは機能単位（`com.catticket.event`）。クラスは `EventController` / `EventService` / `EventMapper`。
- DB カラムは `snake_case`、Java フィールドは `camelCase`（`map-underscore-to-camel-case: true`）。
- API のリクエスト・レスポンスは Java の `record` で DTO を作る。DB の行をそのまま API に返さない。
- 依存性はコンストラクタ注入（フィールドの `@Autowired` は使わない）。

### SQL / Flyway

- 適用済みのマイグレーションファイルは**絶対に編集しない**。変更は新しい `V{次の番号}__*.sql` で行う。
- テーブル・カラムには `COMMENT` を付ける。
- 外部キー・一意制約は DB 側でも定義する（アプリだけに頼らない）。
- MyBatis の値の埋め込みは `#{}` のみ。`${}` は使わない（SQL インジェクション対策）。

### セキュリティ

- 秘密情報（DB パスワード、KOPIS キーなど）はコードに書かない。`.env` → 環境変数で渡す。
- KOPIS は http のみで認証キーが平文で送られるため、**サーバー側からだけ**呼ぶ。フロントエンドから呼ばない。
- Google Maps の API キーはブラウザに出るため、Google Cloud 側で HTTP リファラーと API の制限をかける。

## テスト

- 新しい機能には必ずテストを付ける。テストが通らない PR はマージしない。
- Mapper: 実際の MySQL に対してテスト（`@MybatisTest`）。
- Service: Mapper・外部 API をモックした単体テスト。
- Controller: `@WebMvcTest`。
- 外部 API（KOPIS）を呼ぶテストは書かない。レスポンスはテスト用の XML ファイルで再現する。

## 開発の流れ

1. `docs/design/` の設計書を書く・更新する
2. GitHub Issue を作る（受け入れ条件を書く）
3. ブランチ `feature/{Issue番号}-{内容}` を切る（例: `feature/12-event-api`）
4. 実装 + テスト
5. PR を作る（本文に `Closes #{Issue番号}`、確認した内容を書く）
6. CI（GitHub Actions）が通ることを確認
7. **人間がレビューしてマージ**。AI はマージしない。

### 禁止事項

- `main` に直接 push しない。
- `legacy` ブランチ（2025年の Django 初期版）は保存用。変更・マージしない。
- 設計書と違う実装をしない。設計を変えるときは、先に設計書を更新する。

## コマンド

```bash
docker compose up -d                      # MySQL 起動（開発時は DB だけコンテナ）
cd backend && ./gradlew bootRun           # アプリ起動（http://127.0.0.1:8080）
cd backend && ./gradlew test              # テスト（MySQL の起動が必要）
docker compose --profile app up -d --build  # アプリもコンテナで起動
```

Windows（PowerShell）は `.\gradlew.bat`。
この開発 PC では Java の AF_UNIX ソケットがユーザーフォルダ配下で失敗するため、
ユーザー環境変数 `JDK_JAVA_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\jtmp` を設定済み。

## ドキュメントの言語

- 設計書・ADR・PR・Issue: 日本語（下に韓国語を併記する）
- コード中のコメント: 日本語（短く、「なぜ」を書く）
- コミットメッセージ: 英語（1 行目は命令形で 50 文字程度）

---

# CatTicket 개발 규칙 (한국어)

AI(Claude Code)와 사람이 같은 규칙으로 개발하기 위한 규약. 구현·리뷰 전에 반드시 읽을 것.

## 프로덕트

서울의 공연(콘서트·뮤지컬)을 지도에 표시하고, 공연장 주변의 지하철 물품보관함을 찾을 수 있는 웹 서비스.
사양의 기준(Single Source of Truth)은 `docs/design/`의 설계서. 설계서에 없는 것은 구현하지 않는다.

## 기술 스택

| 구분 | 기술 |
|---|---|
| 백엔드 | Java 21, Spring Boot 4.1, MyBatis(JPA는 쓰지 않음), Flyway, Gradle |
| 프론트엔드 | Angular, Google Maps JavaScript API |
| DB | MySQL 8.4 (utf8mb4, Asia/Seoul) |
| 데이터 수집 | KOPIS Open API (`docs/kopis-api.md`) |

기술 선정 이유는 `docs/adr/` 참고. 새 라이브러리·방식을 도입할 때는 ADR을 추가한다.

## 폴더 구조

위 일본어 섹션의 트리와 같음. 기능 단위 패키지(`event`, `venue`, `locker`, `kopis`, `common`), Flyway SQL은 `db/migration/`, MyBatis XML은 `mapper/{기능}/`, 설계서는 `docs/design/`, 결정 기록은 `docs/adr/`.

## 코딩 규약 (백엔드)

### 계층

`Controller → Service → Mapper` 한 방향만. 역방향·계층 건너뛰기 금지.

- **Controller**: 요청 받기·입력 검사(Bean Validation)·응답 변환만. 업무 로직을 쓰지 않는다.
- **Service**: 업무 로직과 트랜잭션(`@Transactional`).
- **Mapper**: MyBatis `@Mapper` 인터페이스. SQL은 XML에 쓴다(어노테이션에 SQL을 쓰지 않음).

### 명명·타입

- 패키지는 기능 단위(`com.catticket.event`). 클래스는 `EventController` / `EventService` / `EventMapper`.
- DB 컬럼은 `snake_case`, Java 필드는 `camelCase` (`map-underscore-to-camel-case: true`).
- API 요청·응답은 Java `record`로 DTO를 만든다. DB 행을 그대로 API로 반환하지 않는다.
- 의존성은 생성자 주입(필드 `@Autowired`는 쓰지 않음).

### SQL / Flyway

- 이미 적용된 마이그레이션 파일은 **절대 수정하지 않는다**. 변경은 새 `V{다음 번호}__*.sql`로 한다.
- 테이블·컬럼에 `COMMENT`를 붙인다.
- 외래 키·유일 제약은 DB에도 정의한다(앱에만 의존하지 않음).
- MyBatis 값 바인딩은 `#{}`만 쓴다. `${}`는 쓰지 않는다(SQL 인젝션 방지).

### 보안

- 비밀 정보(DB 비밀번호, KOPIS 키 등)는 코드에 쓰지 않는다. `.env` → 환경변수로 전달.
- KOPIS는 http만 지원해 인증키가 평문으로 전송되므로 **서버에서만** 호출한다. 프론트엔드에서 호출하지 않는다.
- Google Maps API 키는 브라우저에 노출되므로 Google Cloud에서 HTTP 리퍼러·API 제한을 건다.

## 테스트

- 새 기능에는 반드시 테스트를 붙인다. 테스트가 통과하지 않는 PR은 머지하지 않는다.
- Mapper: 실제 MySQL로 테스트(`@MybatisTest`).
- Service: Mapper·외부 API를 목(mock)으로 대체한 단위 테스트.
- Controller: `@WebMvcTest`.
- 외부 API(KOPIS)를 실제로 호출하는 테스트는 쓰지 않는다. 응답은 테스트용 XML 파일로 재현한다.

## 개발 흐름

1. `docs/design/`의 설계서를 쓰거나 갱신
2. GitHub Issue 생성 (완료 조건 작성)
3. 브랜치 `feature/{Issue번호}-{내용}` 생성 (예: `feature/12-event-api`)
4. 구현 + 테스트
5. PR 생성 (본문에 `Closes #{Issue번호}`, 확인한 내용 작성)
6. CI(GitHub Actions) 통과 확인
7. **사람이 리뷰하고 머지**. AI는 머지하지 않는다.

### 금지 사항

- `main`에 직접 push하지 않는다.
- `legacy` 브랜치(2025년 Django 첫 버전)는 보관용. 변경·머지하지 않는다.
- 설계서와 다르게 구현하지 않는다. 설계를 바꿀 때는 먼저 설계서를 갱신한다.

## 명령어

위 일본어 섹션과 같음. 이 개발 PC는 Java의 AF_UNIX 소켓이 사용자 폴더 아래에서 실패하므로, 사용자 환경변수 `JDK_JAVA_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\jtmp`를 설정해 둠.

## 문서 언어

- 설계서·ADR·PR·Issue: 일본어로 쓰고, **아래에 한국어를 함께 적는다**
- 코드 주석: 일본어 (짧게, "왜"를 쓴다)
- 커밋 메시지: 영어 (첫 줄은 명령형, 50자 정도)
