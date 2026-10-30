# 0001. バックエンドを Spring Boot 4.1 + Java 21 にする

- 日付: 2026-10-30
- 状態: 採用

## 背景

2025年の初期版は Django で作成した（`legacy` ブランチ）。作り直しも最初は Django（Django REST Framework）で始めたが、ドメインのコードを書く前に方針を見直した。

- 目的は日本の SES 現場の面談で使えるポートフォリオ。
- 日本の業務システムでは Java、特に Spring Boot + Angular の構成が多い。
- 開発者は Spring Boot の経験があり、勤務先のフロントエンドは Angular。

## 決定

- バックエンドは **Spring Boot 4.1**、言語は **Java 21（LTS）**、ビルドは **Gradle** とする。
- Django 版のコードは移行せず、`legacy` ブランチに保存する。

## 検討した選択肢

| 選択肢 | 判断 |
|---|---|
| Django を続ける | 現場での需要が少ない。Python は主にデータ・AI 系 |
| Spring Boot 3.5 | 3.x 最終版だが **2026-06-30 にサポート終了**。Spring Initializr でも選べない |
| **Spring Boot 4.1** | 2027-07-31 までサポート。3.x と考え方（Controller / Service / 設定）はほぼ同じ |
| Java 25 | 最新 LTS だが、現場では 17・21 が中心 |
| Maven | 現場では多いが、Gradle の方が設定が短い。考え方は共通なので現場でも対応できる |

## 結果

- 良い点: 現場の技術と一致。Angular との組み合わせも一般的。
- 注意点:
  - JVM は Django よりメモリを使う。1GB のサーバーでは JVM・MySQL のメモリ設定が必要。
  - Spring Boot 4 からスターター名が変わった（`spring-boot-starter-web` → `spring-boot-starter-webmvc` など）。3.x の記事をそのまま使えない場合がある。
