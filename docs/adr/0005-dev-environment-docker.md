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
