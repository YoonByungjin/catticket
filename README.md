# CatTicket

ソウルで開催中のコンサート・ミュージカルを地図上に表示し、会場周辺（半径500m）の地下鉄コインロッカーを探せるWebサービスです。

> **ステータス: 開発中**
> 2025年に作成した初期版を、設計から作り直しています。初期版のコードは [`legacy`](../../tree/legacy) ブランチに残しています。

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
| バックエンド | Python 3.12, Django 5.2, Django REST Framework |
| フロントエンド | Angular, Google Maps JavaScript API（予定） |
| データベース | MySQL 8.4 |
| 開発環境 | Docker, Docker Compose |
| デプロイ | Oracle Cloud, Nginx, Gunicorn, GitHub Actions（予定） |

## 構成

```
ブラウザ ── Angular（画面・地図）
              │  REST API (JSON)
              ▼
          Django + DRF ── MySQL
              ▲
          毎日の自動収集（KOPIS Open API）
```

## ディレクトリ構成

```
catticket/
├── backend/        Django（API サーバー）
│   ├── config/     プロジェクト設定
│   └── events/     公演・会場・ロッカーのアプリ
├── frontend/       Angular（予定）
├── docs/           設計・API メモ
└── compose.yaml    ローカル開発用（MySQL + Django）
```

## ローカルでの起動方法

### 必要なもの

- Docker Desktop
- KOPIS Open API の認証キー（データ収集を動かす場合のみ）: https://www.kopis.or.kr

### 手順

1. 環境変数ファイルを作成し、値を設定します。

   ```bash
   cp .env.example .env
   ```

   `DJANGO_SECRET_KEY`・`DB_PASSWORD`・`DB_ROOT_PASSWORD` は任意のランダムな文字列に変更してください。

2. コンテナを起動します。

   ```bash
   docker compose up -d --build
   ```

3. データベースのテーブルを作成します。

   ```bash
   docker compose exec web python manage.py migrate
   ```

4. ブラウザで確認します。

   | URL | 内容 |
   |---|---|
   | http://127.0.0.1:8000/admin/ | Django 管理画面 |
   | http://127.0.0.1:8000/api-auth/login/ | DRF ログイン画面 |

### よく使うコマンド

```bash
docker compose logs -f web      # Django のログ
docker compose down             # 停止（DB データは volume に残ります）
docker compose exec web python manage.py createsuperuser
```

## データ出典

公演情報は **(財)芸術経営支援センター 公演芸術統合電算網（KOPIS, www.kopis.or.kr）** の Open API を利用しています。

출처: (재)예술경영지원센터 공연예술통합전산망(www.kopis.or.kr)

---

## 한국어

서울에서 열리는 콘서트·뮤지컬을 지도에 표시하고, 공연장 주변(반경 500m)의 지하철 물품보관함을 찾을 수 있는 웹 서비스입니다.

> **상태: 개발 중**
> 2025년에 만든 첫 버전을 설계부터 다시 만들고 있습니다. 첫 버전의 코드는 [`legacy`](../../tree/legacy) 브랜치에 남겨두었습니다.

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
| 백엔드 | Python 3.12, Django 5.2, Django REST Framework |
| 프론트엔드 | Angular, Google Maps JavaScript API (예정) |
| 데이터베이스 | MySQL 8.4 |
| 개발 환경 | Docker, Docker Compose |
| 배포 | Oracle Cloud, Nginx, Gunicorn, GitHub Actions (예정) |

### 로컬 실행 방법

필요한 것: Docker Desktop, (데이터 수집을 돌릴 경우) KOPIS Open API 인증키

1. 환경변수 파일을 만들고 값을 설정합니다. `DJANGO_SECRET_KEY`, `DB_PASSWORD`, `DB_ROOT_PASSWORD`는 임의의 무작위 문자열로 바꿔주세요.

   ```bash
   cp .env.example .env
   ```

2. 컨테이너를 실행합니다.

   ```bash
   docker compose up -d --build
   ```

3. DB 테이블을 만듭니다.

   ```bash
   docker compose exec web python manage.py migrate
   ```

4. 브라우저에서 http://127.0.0.1:8000/admin/ (Django 관리자 화면)을 엽니다.

### 데이터 출처

공연 정보는 (재)예술경영지원센터 공연예술통합전산망(KOPIS, www.kopis.or.kr)의 Open API를 이용합니다.
