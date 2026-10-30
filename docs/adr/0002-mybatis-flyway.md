# 0002. DB アクセスは MyBatis、スキーマ管理は Flyway

- 日付: 2026-10-30
- 状態: 採用

## 背景

Spring Boot での DB アクセスには JPA（Hibernate）と MyBatis がある。日本の SI・SES 現場では、SQL を直接書く MyBatis が多く使われている。

## 決定

- DB アクセスは **MyBatis のみ**（JPA は使わない）。SQL は XML（`resources/mapper/`）に書く。
- テーブルの作成・変更は **Flyway** で管理する（`resources/db/migration/V{番号}__*.sql`）。

## 検討した選択肢

| 選択肢 | 判断 |
|---|---|
| JPA のみ | コード量は少ないが、現場で求められる SQL・MyBatis の経験にならない |
| JPA + MyBatis | 両方の経験になるが、個人開発では方式が混ざって規約が複雑になる |
| **MyBatis のみ** | 現場に最も近い。SQL を自分で書くので、テーブル設計・インデックスの理解が深まる |

MyBatis はテーブルを自動で作らないため、スキーマの変更履歴を管理する Flyway を組み合わせる。手作業の `CREATE TABLE` だと、環境ごと（開発 PC・サーバー・CI）に状態がずれる。

## 結果

- 良い点: アプリ起動時に Flyway が未適用の SQL を順番に実行するので、どの環境でも同じテーブル構成になる。
- 注意点:
  - `mybatis-spring-boot-starter` は Spring Boot のバージョン管理外。Spring Boot のバージョンに合ったものを明記する（現在 4.1.0）。
  - 適用済みのマイグレーションファイルは編集できない（チェックサムが変わり起動エラーになる）。
