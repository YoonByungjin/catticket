# 0005. 開発環境は Docker Compose の MySQL + IDE 実行

- 日付: 2026-10-30
- 状態: 採用

## 背景

開発 PC（Windows）とデプロイ先（Linux）で、同じバージョン・同じ設定の MySQL を使いたい。

## 決定

- MySQL 8.4 は **Docker Compose** で起動する（`compose.yaml`）。文字コード utf8mb4、タイムゾーン Asia/Seoul。
- 日常の開発では **Spring Boot は IDE（VS Code）から起動**し、DB だけコンテナにする。デバッグしやすいため。
- アプリもコンテナで動かすときは `--profile app` を付ける（デプロイと同じイメージを確認できる）。
- 接続情報などはリポジトリ直下の `.env` に置き、Git には入れない（`.env.example` を共有）。
- リポジトリは `backend/` と `frontend/` に分ける（モノレポ）。
- 改行コードは LF に統一する（`.gitattributes`）。Linux コンテナで CRLF のスクリプトが動かないため。

## 検討した選択肢

| 選択肢 | 判断 |
|---|---|
| MySQL を Windows に直接インストール | 環境ごとにバージョン・設定がずれる |
| **MySQL だけ Docker** | 設定がファイル（`compose.yaml`）に残り、誰でも同じ環境を再現できる |
| アプリも常に Docker | Java のデバッグ・ホットリロードが面倒 |

## 結果

- 良い点: `docker compose up -d` だけで DB が用意できる。データは volume に残る。
- 注意点:
  - 勤務先 PC では Docker Desktop の利用規約（大企業の業務利用は有料）を確認する。
  - MySQL の初期化スクリプト（`docker/mysql/initdb/`）は volume が空のときだけ実行される。

---

## 한국어

### 배경

개발 PC(Windows)와 배포 서버(Linux)에서 같은 버전·같은 설정의 MySQL을 쓰고 싶다.

### 결정

- MySQL 8.4는 **Docker Compose**로 실행한다(`compose.yaml`). 문자셋 utf8mb4, 시간대 Asia/Seoul.
- 평소 개발에서는 **Spring Boot를 IDE(VS Code)에서 실행**하고 DB만 컨테이너로 띄운다. 디버깅이 쉽기 때문.
- 앱도 컨테이너로 실행할 때는 `--profile app`을 붙인다(배포와 같은 이미지를 확인할 수 있음).
- 접속 정보 등은 저장소 루트의 `.env`에 두고 Git에는 넣지 않는다(`.env.example`을 공유).
- 저장소는 `backend/`와 `frontend/`로 나눈다(모노레포).
- 줄바꿈은 LF로 통일한다(`.gitattributes`). 리눅스 컨테이너에서 CRLF 스크립트가 실행되지 않기 때문.

### 검토한 선택지

| 선택지 | 판단 |
|---|---|
| MySQL을 Windows에 직접 설치 | 환경마다 버전·설정이 어긋난다 |
| **MySQL만 Docker** | 설정이 파일(`compose.yaml`)로 남아 누구나 같은 환경을 재현할 수 있다 |
| 앱도 항상 Docker | Java 디버깅·핫 리로드가 번거롭다 |

### 결과

- 좋은 점: `docker compose up -d`만으로 DB가 준비된다. 데이터는 볼륨에 남는다.
- 주의점:
  - 근무처 PC에서는 Docker Desktop 이용 약관(대기업 업무 이용은 유료)을 확인한다.
  - MySQL 초기화 스크립트(`docker/mysql/initdb/`)는 볼륨이 비어 있을 때만 실행된다.
