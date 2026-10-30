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

---

## 한국어

### 배경

2025년 첫 버전은 Django로 만들었다(`legacy` 브랜치). 다시 만들 때도 처음에는 Django(DRF)로 시작했지만, 도메인 코드를 쓰기 전에 방침을 바꿨다.

- 목적은 일본 SES 현장 면담에서 쓸 포트폴리오.
- 일본 업무 시스템은 Java, 특히 Spring Boot + Angular 구성이 많다.
- 개발자는 Spring Boot 경험이 있고, 근무처 프론트엔드는 Angular.

### 결정

- 백엔드는 **Spring Boot 4.1**, 언어는 **Java 21(LTS)**, 빌드는 **Gradle**.
- Django 버전 코드는 옮기지 않고 `legacy` 브랜치에 보관한다.

### 검토한 선택지

| 선택지 | 판단 |
|---|---|
| Django 유지 | 현장 수요가 적다. Python은 주로 데이터·AI 쪽 |
| Spring Boot 3.5 | 3.x 마지막 버전이지만 **2026-06-30 지원 종료**. Spring Initializr에서도 고를 수 없음 |
| **Spring Boot 4.1** | 2027-07-31까지 지원. 3.x와 개념(Controller / Service / 설정)은 거의 같음 |
| Java 25 | 최신 LTS지만 현장은 17·21이 중심 |
| Maven | 현장에 많지만 Gradle 설정이 더 짧다. 개념이 같아서 현장에서도 대응 가능 |

### 결과

- 좋은 점: 현장 기술과 일치. Angular와의 조합도 일반적.
- 주의점:
  - JVM은 Django보다 메모리를 많이 쓴다. 1GB 서버에서는 JVM·MySQL 메모리 설정이 필요.
  - Spring Boot 4부터 스타터 이름이 바뀌었다(`spring-boot-starter-web` → `spring-boot-starter-webmvc` 등). 3.x 기준 글을 그대로 쓸 수 없는 경우가 있다.
