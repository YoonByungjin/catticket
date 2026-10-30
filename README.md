# CatTicket

ソウルの公演情報を地図で探せる Web サービス（仮）。

> **ステータス: 企画中**
> 2025年に Django で作成した初期版（[`legacy`](../../tree/legacy) ブランチ）をもとに、企画・設計からやり直しています。
> サービスの内容は [`docs/design/`](docs/design/README.md) の企画書・要件定義書で決めます。

## 開発の進め方

実務と同じ工程（企画 → 要件定義 → 基本設計 → 詳細設計 → 実装 → テスト → リリース）で進め、各工程の最後にレビューの関門を置いています。
設計・実装・テスト・レビューは、役割ごとに定義した AI エージェント（[`.claude/agents/`](.claude/agents/)）が担当し、人間（開発者）が要件の決定・レビュー・マージを行います。

| 資料 | 内容 |
|---|---|
| [`CLAUDE.md`](CLAUDE.md) | 開発ルール（工程とゲート、エージェント体制、コーディング規約、Git の流れ） |
| [`docs/design/`](docs/design/README.md) | 設計書（企画書・要件定義書・画面・API・テーブル・バッチ・クラス） |
| [`docs/adr/`](docs/adr/README.md) | 技術選定の記録（なぜその技術にしたか） |
| [Pull requests](../../pulls?q=is%3Apr) | 変更ごとの目的・内容・確認結果 |

## 技術スタック

| 区分 | 技術 |
|---|---|
| バックエンド | Java 21, Spring Boot 4.1, MyBatis, Flyway, Gradle |
| フロントエンド | Angular |
| データベース | MySQL 8.4 |
| 開発環境 | Docker Compose, VS Code, GitHub Actions（CI） |

地図や外部データ API は要件定義の後で決めます。

## ディレクトリ構成

```
catticket/
├── backend/            Spring Boot（API サーバー）
├── frontend/           Angular（予定）
├── docs/
│   ├── design/         設計書
│   └── adr/            技術選定の記録
├── .claude/agents/     AI エージェントの定義
├── docker/mysql/       MySQL 初回起動時のスクリプト
└── compose.yaml        ローカル開発用（MySQL）
```

## ローカルでの起動方法

必要なもの: Docker Desktop、JDK 21（例: Eclipse Temurin）

1. 環境変数ファイルを作成し、`DB_PASSWORD`・`DB_ROOT_PASSWORD` を任意のランダムな文字列に変更します。

   ```bash
   cp .env.example .env
   ```

2. MySQL を起動します。

   ```bash
   docker compose up -d
   ```

3. Spring Boot を起動します（Windows の PowerShell では `.\gradlew.bat bootRun`）。テーブルは Flyway が起動時に作成します。

   ```bash
   cd backend
   ./gradlew bootRun
   ```

テストは `cd backend && ./gradlew test`（テスト専用 DB `test_catticket` を使います）。

---

## 한국어

서울의 공연 정보를 지도로 찾는 웹 서비스(가칭).

> **상태: 기획 중**
> 2025년에 Django로 만든 첫 버전([`legacy`](../../tree/legacy) 브랜치)을 바탕으로 기획·설계부터 다시 하고 있습니다.
> 서비스 내용은 [`docs/design/`](docs/design/README.md)의 기획서·요건정의서에서 정합니다.

### 개발 진행 방식

실무와 같은 공정(기획 → 요건정의 → 기본설계 → 상세설계 → 구현 → 테스트 → 릴리스)으로 진행하고, 각 공정 끝에 리뷰 관문을 둡니다.
설계·구현·테스트·리뷰는 역할별로 정의한 AI 에이전트([`.claude/agents/`](.claude/agents/))가 맡고, 사람(개발자)이 요건 결정·리뷰·머지를 합니다.

| 자료 | 내용 |
|---|---|
| [`CLAUDE.md`](CLAUDE.md) | 개발 규칙 (공정과 관문, 에이전트 체제, 코딩 규약, Git 흐름) |
| [`docs/design/`](docs/design/README.md) | 설계서 (기획서·요건정의서·화면·API·테이블·배치·클래스) |
| [`docs/adr/`](docs/adr/README.md) | 기술 선정 기록 (왜 그 기술을 골랐는지) |
| [Pull requests](../../pulls?q=is%3Apr) | 변경마다의 목적·내용·확인 결과 |

### 기술 스택

| 구분 | 기술 |
|---|---|
| 백엔드 | Java 21, Spring Boot 4.1, MyBatis, Flyway, Gradle |
| 프론트엔드 | Angular |
| 데이터베이스 | MySQL 8.4 |
| 개발 환경 | Docker Compose, VS Code, GitHub Actions(CI) |

지도와 외부 데이터 API는 요건정의 후에 정합니다.

### 로컬 실행 방법

필요한 것: Docker Desktop, JDK 21 (예: Eclipse Temurin)

1. `cp .env.example .env`로 환경변수 파일을 만들고 `DB_PASSWORD`, `DB_ROOT_PASSWORD`를 임의의 무작위 문자열로 바꿉니다.
2. `docker compose up -d`로 MySQL을 실행합니다.
3. `cd backend` 후 `./gradlew bootRun`으로 Spring Boot를 실행합니다(Windows PowerShell은 `.\gradlew.bat bootRun`). 테이블은 Flyway가 시작할 때 만듭니다.

테스트는 `cd backend && ./gradlew test` (테스트 전용 DB `test_catticket` 사용).
