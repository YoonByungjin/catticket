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

---

## 한국어

### 배경

Spring Boot의 DB 접근 방식에는 JPA(Hibernate)와 MyBatis가 있다. 일본 SI·SES 현장에서는 SQL을 직접 쓰는 MyBatis가 많이 쓰인다.

### 결정

- DB 접근은 **MyBatis만** 쓴다(JPA는 쓰지 않음). SQL은 XML(`resources/mapper/`)에 쓴다.
- 테이블 생성·변경은 **Flyway**로 관리한다(`resources/db/migration/V{번호}__*.sql`).

### 검토한 선택지

| 선택지 | 판단 |
|---|---|
| JPA만 | 코드량은 적지만, 현장에서 요구하는 SQL·MyBatis 경험이 되지 않음 |
| JPA + MyBatis | 둘 다 경험할 수 있지만, 개인 개발에서는 방식이 섞여 규약이 복잡해짐 |
| **MyBatis만** | 현장에 가장 가깝다. SQL을 직접 쓰므로 테이블 설계·인덱스 이해가 깊어진다 |

MyBatis는 테이블을 자동으로 만들지 않으므로, 스키마 변경 이력을 관리하는 Flyway를 함께 쓴다. 손으로 `CREATE TABLE`을 실행하면 환경마다(개발 PC·서버·CI) 상태가 어긋난다.

### 결과

- 좋은 점: 앱 시작 시 Flyway가 아직 적용 안 된 SQL을 순서대로 실행하므로, 어느 환경이든 같은 테이블 구성이 된다.
- 주의점:
  - `mybatis-spring-boot-starter`는 Spring Boot의 버전 관리 밖. Spring Boot 버전에 맞는 것을 명시한다(현재 4.1.0).
  - 이미 적용된 마이그레이션 파일은 수정할 수 없다(체크섬이 바뀌어 시작 오류가 남).
