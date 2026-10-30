# ADR（Architecture Decision Record）

技術的な判断を「なぜそうしたか」とともに残す記録。一度書いた ADR は書き換えず、判断を変えるときは新しい ADR を追加して古いものを「置き換え済み」にする。

| No. | タイトル | 状態 |
|---|---|---|
| [0001](0001-backend-spring-boot.md) | バックエンドを Spring Boot 4.1 + Java 21 にする | 採用 |
| [0002](0002-mybatis-flyway.md) | DB アクセスは MyBatis、スキーマ管理は Flyway | 採用 |
| [0003](0003-data-source-kopis.md) | 公演データは KOPIS Open API から取得する | 採用 |
| [0004](0004-map-google-maps.md) | 地図は Google Maps を使う | 採用 |
| [0005](0005-dev-environment-docker.md) | 開発環境は Docker Compose の MySQL + IDE 実行 | 採用 |

## テンプレート

```markdown
# {番号}. {タイトル}

- 日付: YYYY-MM-DD
- 状態: 提案 / 採用 / 置き換え済み（→ {番号}）

## 背景
## 決定
## 検討した選択肢
## 結果（良い点・注意点）
```
