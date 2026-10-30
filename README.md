# CatTicket

ソウルで開催中のコンサート・ミュージカルを地図上に表示し、会場周辺（半径500m）の地下鉄コインロッカーを探せるWebサービスです。

> **ステータス: 開発中**
> 2025年に Django で作成した初期版を、Spring Boot + Angular で設計から作り直しています。初期版のコードは [`legacy`](../../tree/legacy) ブランチに残しています。

## 主な機能（予定を含む）

| 機能 | 状態 |
|---|---|
| 公演情報の自動収集（KOPIS Open API、毎日更新） | 予定 |
| 公演・会場を地図上に表示（Google Maps） | 予定 |
| ジャンル絞り込み・公演名検索 | 予定 |
| 会場周辺のコインロッカー検索 | 予定 |
| 日本語 / 韓国語の画面切り替え | 予定 |

## 技術スタック

| 区分 | 技術 |
|---|---|
| バックエンド | Java 21, Spring Boot 4.1, MyBatis, Flyway, Gradle |
| フロントエンド | Angular, Google Maps JavaScript API（予定） |
| データベース | MySQL 8.4 |
| 開発環境 | Docker, Docker Compose, VS Code |
| デプロイ | Oracle Cloud, Nginx, GitHub Actions（予定） |

## 構成

```
ブラウザ ── Angular（画面・地図）
              │  REST API (JSON)
              ▼
     Spring Boot（Controller → Service → MyBatis Mapper）── MySQL
              ▲
     毎日の自動収集（@Scheduled → KOPIS Open API）
```

## ディレクトリ構成

```
catticket/
├── backend/                         Spring Boot（API サーバー）
│   ├── src/main/java/com/catticket/ Java コード
│   └── src/main/resources/
│       ├── application.yml          設定（DB 接続など）
│       ├── db/migration/            Flyway のテーブル定義 SQL（V1__*.sql …）
│       └── mapper/                  MyBatis の SQL（XML）
├── frontend/                        Angular（予定）
├── docker/mysql/initdb/             MySQL 初回起動時のスクリプト
├── docs/                            設計・API メモ
└── compose.yaml                     ローカル開発用
```

## ローカルでの起動方法

### 必要なもの

- Docker Desktop
- JDK 21（例: Eclipse Temurin）
- KOPIS Open API の認証キー（データ収集を動かす場合のみ）: https://www.kopis.or.kr

### 手順

1. 環境変数ファイルを作成し、`DB_PASSWORD`・`DB_ROOT_PASSWORD` を任意のランダムな文字列に変更します。

   ```bash
   cp .env.example .env
   ```

2. MySQL を起動します。

   ```bash
   docker compose up -d
   ```

3. Spring Boot を起動します。テーブルは Flyway が起動時に自動で作成します。

   ```bash
   cd backend
   ./gradlew bootRun
   ```

   Windows（PowerShell）では `.\gradlew.bat bootRun` です。API は http://127.0.0.1:8080 で動きます。

Spring Boot もコンテナで動かす場合は、手順 2・3 の代わりに次を実行します。

```bash
docker compose --profile app up -d --build
```

### よく使うコマンド

```bash
cd backend && ./gradlew test          # テスト（MySQL の起動が必要）
docker compose logs -f db             # MySQL のログ
docker compose down                   # 停止（DB データは volume に残ります）
```

## データ出典

公演情報は **(財)芸術経営支援センター 公演芸術統合電算網（KOPIS, www.kopis.or.kr）** の Open API を利用しています。

출처: (재)예술경영지원센터 공연예술통합전산망(www.kopis.or.kr)

---

## 한국어

서울에서 열리는 콘서트·뮤지컬을 지도에 표시하고, 공연장 주변(반경 500m)의 지하철 물품보관함을 찾을 수 있는 웹 서비스입니다.

> **상태: 개발 중**
> 2025년에 Django로 만든 첫 버전을 Spring Boot + Angular로 설계부터 다시 만들고 있습니다. 첫 버전의 코드는 [`legacy`](../../tree/legacy) 브랜치에 남겨두었습니다.

### 주요 기능 (예정 포함)

| 기능 | 상태 |
|---|---|
| 공연 정보 자동 수집 (KOPIS Open API, 매일 갱신) | 예정 |
| 공연·공연장을 지도에 표시 (Google Maps) | 예정 |
| 장르 필터·공연명 검색 | 예정 |
| 공연장 주변 물품보관함 검색 | 예정 |
| 일본어 / 한국어 화면 전환 | 예정 |

### 기술 스택

| 구분 | 기술 |
|---|---|
| 백엔드 | Java 21, Spring Boot 4.1, MyBatis, Flyway, Gradle |
| 프론트엔드 | Angular, Google Maps JavaScript API (예정) |
| 데이터베이스 | MySQL 8.4 |
| 개발 환경 | Docker, Docker Compose, VS Code |
| 배포 | Oracle Cloud, Nginx, GitHub Actions (예정) |

### 로컬 실행 방법

필요한 것: Docker Desktop, JDK 21, (데이터 수집을 돌릴 경우) KOPIS Open API 인증키

1. 환경변수 파일을 만들고 `DB_PASSWORD`, `DB_ROOT_PASSWORD`를 임의의 무작위 문자열로 바꿔주세요.

   ```bash
   cp .env.example .env
   ```

2. MySQL을 실행합니다.

   ```bash
   docker compose up -d
   ```

3. Spring Boot를 실행합니다. 테이블은 Flyway가 시작할 때 자동으로 만듭니다.

   ```bash
   cd backend
   ./gradlew bootRun
   ```

   Windows(PowerShell)에서는 `.\gradlew.bat bootRun`입니다. API는 http://127.0.0.1:8080 에서 동작합니다.

Spring Boot도 컨테이너로 실행하려면 2·3단계 대신 `docker compose --profile app up -d --build`를 실행합니다.

### 데이터 출처

공연 정보는 (재)예술경영지원센터 공연예술통합전산망(KOPIS, www.kopis.or.kr)의 Open API를 이용합니다.
