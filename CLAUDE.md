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

- 設計書・ADR・PR・Issue: 日本語
- コード中のコメント: 日本語（短く、「なぜ」を書く）
- コミットメッセージ: 英語（1 行目は命令形で 50 文字程度）
