# ADR（Architecture Decision Record）

技術的な判断を「なぜそうしたか」とともに残す記録。

ルール:

- **決定内容（背景・決定・検討した選択肢・結果）は書き換えない。** 判断を変えるときは新しい ADR を追加し、古いものの状態を「置き換え済み（→ 新しい番号）」にする。
- **状態欄だけは更新してよい**（提案 → 採用 / 却下、採用 → 置き換え済み）。更新したら一覧表も合わせる。
- 状態: **提案**（検討中） / **採用** / **却下** / **置き換え済み**

| No. | タイトル | 状態 |
|---|---|---|
| [0001](0001-backend-spring-boot.md) | バックエンドを Spring Boot 4.1 + Java 21 にする | 採用 |
| [0002](0002-mybatis-flyway.md) | DB アクセスは MyBatis、スキーマ管理は Flyway | 採用 |
| [0003](0003-data-source-kopis.md) | 公演データは KOPIS Open API から取得する | 置き換え済み（→ 0008） |
| [0004](0004-map-google-maps.md) | 地図は Google Maps を使う | 置き換え済み（→ 0006） |
| [0005](0005-dev-environment-docker.md) | 開発環境は Docker Compose の MySQL + IDE 実行 | 採用 |
| [0006](0006-map-google-maps-cost-control.md) | 地図は Google Maps JavaScript API を使い、使用量の上限と予算アラートを設定する（0004 を置き換える） | 採用 |
| [0007](0007-translation-azure-translator.md) | 外部データの翻訳は Azure AI Translator の無料レベル（F0）を使う | 採用 |
| [0008](0008-kopis-data-scope.md) | KOPIS Open API のデータ範囲と、人気急上昇の順位の変化の計算方式（0003 を置き換える） | 採用 |
| [0009](0009-station-locker-data-source.md) | 駅・コインロッカーのデータは、ソウル交通公社の公開データから作る | 採用 |
| [0010](0010-server-oracle-cloud-duckdns.md) | サーバーは Oracle Cloud の Always Free（A1 を優先）、ドメインは DuckDNS にする | 採用 |
| [0011](0011-bot-check-recaptcha.md) | 自動操作ではないことの確認は Google reCAPTCHA（課金を有効にしない専用プロジェクト）で行う | 採用 |
| [0012](0012-mail-gmail-smtp.md) | メールは専用の Gmail アカウントから SMTP（アプリパスワード）で送る | 採用 |
| [0013](0013-auth-server-session.md) | 認証はサーバーのセッション（Spring Security）で行う | 採用 |

## テンプレート

```markdown
# {番号}. {タイトル}

- 日付: YYYY-MM-DD
- 状態: 提案 / 採用 / 却下 / 置き換え済み（→ {番号}）

## 背景
## 決定
## 検討した選択肢
## 結果（良い点・注意点）

---

## 한국어

<!-- 上の内容の韓国語訳 / 위 내용의 한국어 번역 -->
```

---

## 한국어

기술적인 판단을 "왜 그렇게 했는지"와 함께 남기는 기록.

규칙:

- **결정 내용(배경·결정·검토한 선택지·결과)은 고치지 않는다.** 판단을 바꿀 때는 새 ADR을 추가하고, 예전 것의 상태를 "대체됨(→ 새 번호)"으로 바꾼다.
- **상태란만은 갱신해도 된다**(제안 → 채택/기각, 채택 → 대체됨). 갱신하면 목록 표도 맞춘다.
- 상태: **제안**(검토 중) / **채택** / **기각** / **대체됨**

| No. | 제목 | 상태 |
|---|---|---|
| 0001 | 백엔드를 Spring Boot 4.1 + Java 21로 한다 | 채택 |
| 0002 | DB 접근은 MyBatis, 스키마 관리는 Flyway | 채택 |
| 0003 | 공연 데이터는 KOPIS Open API에서 가져온다 | 대체됨(→ 0008) |
| 0004 | 지도는 Google Maps를 쓴다 | 대체됨(→ 0006) |
| 0005 | 개발 환경은 Docker Compose의 MySQL + IDE 실행 | 채택 |
| 0006 | 지도는 Google Maps JavaScript API를 쓰고, 사용량 상한과 예산 알림을 설정한다(0004를 대체) | 채택 |
| 0007 | 외부 데이터 번역은 Azure AI Translator 무료 등급(F0)을 쓴다 | 채택 |
| 0008 | KOPIS Open API 데이터 범위와 인기 급상승 순위 변화 계산 방식(0003을 대체) | 채택 |
| 0009 | 역·물품보관함 데이터는 서울교통공사의 공개 데이터로 만든다 | 채택 |
| 0010 | 서버는 Oracle Cloud Always Free(A1 우선), 도메인은 DuckDNS로 한다 | 채택 |
| 0011 | 자동 조작이 아님을 확인하는 것은 Google reCAPTCHA(결제를 켜지 않은 전용 프로젝트)로 한다 | 채택 |
| 0012 | 메일은 전용 Gmail 계정에서 SMTP(앱 비밀번호)로 보낸다 | 채택 |
| 0013 | 인증은 서버 세션(Spring Security)으로 한다 | 채택 |
