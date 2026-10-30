# ADR（Architecture Decision Record）

技術的な判断を「なぜそうしたか」とともに残す記録。一度書いた ADR は書き換えず、判断を変えるときは新しい ADR を追加して古いものを「置き換え済み」にする。

| No. | タイトル | 状態 |
|---|---|---|
| [0001](0001-backend-spring-boot.md) | バックエンドを Spring Boot 4.1 + Java 21 にする | 採用 |
| [0002](0002-mybatis-flyway.md) | DB アクセスは MyBatis、スキーマ管理は Flyway | 採用 |
| [0003](0003-data-source-kopis.md) | 公演データは KOPIS Open API から取得する | 提案 |
| [0004](0004-map-google-maps.md) | 地図は Google Maps を使う | 提案 |
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

---

## 한국어

기술적인 판단을 "왜 그렇게 했는지"와 함께 남기는 기록. 한 번 쓴 ADR은 고치지 않고, 판단을 바꿀 때는 새 ADR을 추가하고 예전 것을 "대체됨"으로 표시한다.

| No. | 제목 | 상태 |
|---|---|---|
| 0001 | 백엔드를 Spring Boot 4.1 + Java 21로 한다 | 채택 |
| 0002 | DB 접근은 MyBatis, 스키마 관리는 Flyway | 채택 |
| 0003 | 공연 데이터는 KOPIS Open API에서 가져온다 | 제안 |
| 0004 | 지도는 Google Maps를 쓴다 | 제안 |
| 0005 | 개발 환경은 Docker Compose의 MySQL + IDE 실행 | 채택 |
