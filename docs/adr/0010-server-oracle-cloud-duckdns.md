# 0010. サーバーは Oracle Cloud の Always Free（A1 を優先）、ドメインは DuckDNS にする

- 日付: 2026-11-05
- 状態: 提案
- 関係する要件: N-05 ①, N-06, N-07, N-08（U-22）

## 背景

- 公開 URL で誰でも使え、ドメインを取得して公開する（N-08）。通信は HTTPS（N-05 ①）。可用性の目標は置かず、障害は手動で復旧する（N-06）。
- 運用コストは月0円（N-07）。メモリなどサーバーの制約の値も、ここで決める（U-22）。
- 人間の決定 T-03: **Oracle Cloud の無料枠の ARM（A1）を優先し、作れない場合は AMD（1GB）にする。** 人間の希望: ドメインは DuckDNS。
- 構成: Spring Boot（Java 21）、Angular（ビルドした静的ファイル）、MySQL 8.4。ADR 0005 で、アプリもコンテナで動かせる（`docker compose --profile app`）。

## 決定

### 1. アカウントとリージョン

- Oracle Cloud の**Free Tier のアカウント**を作り、**有料のアカウント（Pay As You Go）にアップグレードしない**。Free Tier のアカウントは、アップグレードしない限りカードに請求されない（公式）。
- ホームリージョンは **South Korea Central（ソウル）** にする（利用者がソウルにいるため。Always Free のインスタンスはホームリージョンにしか作れず、ホームリージョンは後から変えられない）。

### 2. サーバー（T-03）

**第1候補: Ampere A1（VM.Standard.A1.Flex）を1台、2 OCPU・12 GB メモリ**（Always Free の上限をすべて1台に使う）。

| 項目 | 値 |
|---|---|
| OS | Ubuntu 24.04 LTS（Arm 版） |
| ブートボリューム | 50 GB（Always Free のブロックボリュームは合計 200 GB まで） |
| 動かすもの | Nginx（ホストに直接）、アプリと MySQL（Docker Compose のコンテナ） |

**メモリの割り当て（A1、12 GB）**:

| 対象 | 制約の値 |
|---|---|
| アプリのコンテナ | メモリの上限 3 GB。JVM ヒープ `-Xms2g -Xmx2g`（`-XX:+AlwaysPreTouch` で起動時に確保する） |
| MySQL のコンテナ | メモリの上限 3 GB。`innodb_buffer_pool_size=2G` |
| Nginx・OS など | 残り（約 6 GB） |

JVM ヒープを起動時に確保するのは、下の「アイドルのインスタンスの回収」の条件（メモリの使用率 20% 未満）に当たらないようにするため（合計で約 4.5 GB 以上を使う）。

**第2候補（A1 を作れないとき）: AMD（VM.Standard.E2.1.Micro）を2台**（1/8 OCPU・1 GB メモリずつ。Always Free で2台まで）。

| 対象 | 制約の値 |
|---|---|
| 1台目: Nginx + アプリ | JVM ヒープ `-Xmx384m`、アプリのコンテナのメモリの上限 640 MB、スワップ 2 GB |
| 2台目: MySQL | `innodb_buffer_pool_size=256M`、`performance_schema=OFF`、コンテナのメモリの上限 700 MB、スワップ 2 GB |

A1 を作るときに「容量不足（out of host capacity）」が出た場合は、時間をおいて何度か作り直す。それでも作れなければ第2候補にする（いつ切り替えるかは人間が決める）。

### 3. ドメインと HTTPS

- ドメインは **DuckDNS** のサブドメイン（`{名前}.duckdns.org`。名前は人間が決める）。
- IP アドレスは、インスタンスに**予約済みのパブリック IP** を付けて変わらないようにする。変わったときは DuckDNS の更新 API（HTTPS の GET）で更新する。
- 証明書は **Let's Encrypt**（無料）。HTTP-01 の確認には 80 番ポートが必要なので、Nginx で 80・443 番を開け、80 番は HTTPS へ転送する。証明書の更新は自動にする。
- Nginx の役割: HTTPS の終端、Angular の静的ファイルの配信、`/api` をアプリに転送（同じオリジンにする。0013 の CSRF・Cookie の前提）。

### 4. 使用量の上限・予算アラート（T-01）

- 上限: Free Tier のアカウントのまま使う（アップグレードしない）。**Always Free の範囲を超えるリソースは作れず、請求も発生しない**。A1 は合計 2 OCPU・12 GB を超えて作らない。
- 予算アラート: OCI の「予算（Budgets）」をルートコンパートメントに作る。月の予算 1 USD、実際の費用が 1% を超えたらこのプロジェクト専用のメールアドレスに通知する。Free Tier のアカウントで予算を作れなかった場合は、その旨を運用手順に書く（アップグレードしない限り請求されないため、上限は保たれる）。

## 検討した選択肢

| 選択肢 | 無料の範囲 | カード | 判断 |
|---|---|---|---|
| **Oracle Cloud Always Free（A1 → AMD）** | A1: 月 1,500 OCPU 時間・9,000 GB 時間（2 OCPU・12 GB 相当）。AMD: 2台（1 GB ずつ）。ブロックボリューム合計 200 GB。外向きの通信 月 10 TB | 登録に必要。アップグレードしない限り請求されない | **採用を提案**（T-03 の決定と人間の希望どおり） |
| 他のクラウドの無料枠（期間限定のもの） | 12 か月などの期限がある | 必要 | 期限の後は有料になり、月0円を続けられない |
| 自宅の PC で公開 | — | — | 常時起動・回線・セキュリティの負担が大きい。公開 URL での安定した公開に向かない |

| ドメインの選択肢 | 費用 | 判断 |
|---|---|---|
| **DuckDNS のサブドメイン** | 無料 | **採用を提案**（人間の希望）。A レコードと TXT レコードを API で更新できる |
| 独自ドメインの購入 | 年額の費用がかかる | 月0円を満たさない |

## 結果（良い点・注意点）

### 費用（月0円の範囲か）

| 項目 | 内容 |
|---|---|
| 無料枠 | 上の表のとおり（Always Free はアカウントがある限り期限なし） |
| カード登録 | Oracle Cloud: 必要（本人確認）。DuckDNS・Let's Encrypt: 不要 |
| 上限を超えたとき | Free Tier のアカウントでは Always Free を超えるリソースを使えない（請求されない）。無料トライアルの終わりに A1 が上限を超えていると、インスタンスは無効にされ 30 日後に削除される（公式） |
| 予算アラート | OCI の予算 1 USD、1% で通知。**予算は支出を止めない**（公式）。止めるのは「アップグレードしない」ことで行う |

### 良い点

- 期限なく無料で使え、アップグレードしない限り請求が発生しない。
- A1 なら 12 GB あり、Spring Boot と MySQL を1台で余裕を持って動かせる。

### 注意点

- **アイドルのインスタンスの回収**: 7 日間、CPU 使用率（95 パーセンタイル）20% 未満・ネットワーク使用率 20% 未満・メモリ使用率 20% 未満（A1 だけ）がすべて当てはまると、Oracle に回収されることがある（公式）。A1 ではメモリの割り当てでこれを避けるが、Oracle の測り方の細部は公開されていない。**MySQL のバックアップを毎日取り、サーバーを作り直す手順を運用手順（`docs/ops/`）に書く**（N-06 の手動復旧）。AMD ではメモリの条件がないため、回収されることがありうる。
- A1 はホームリージョンで容量不足になることがある（公式）。
- アップグレードしないため、Free Tier のアカウントの制限（VCN は2つまで、Object Storage は合計 20 GB など）の中で使う。
- 本番では Docker Compose でアプリと MySQL を動かす（ADR 0005 の `--profile app` と同じイメージ）。サーバーの具体的な作り方は運用手順で書く。
- DuckDNS は `duckdns.org` のサブドメインであり、自分で登録したドメインではない。N-08 の「ドメインを取得して公開する」をこれで満たすとしてよいか、人間に確認する。
- DuckDNS は SLA などを公開していない。止まったときは手動で対応する（N-06）。
- OCI は 2021 年 6 月以降に作ったテナンシーの外向きの 25 番ポートを止めている。メール送信は 587 番を使う（0012）。

### 確認した公式ドキュメント（確認日: 2026-11-05）

- Always Free のリソース（A1 の時間数と 2 OCPU・12 GB 相当、AMD 2台、ブロックボリューム 200 GB、外向き 10 TB、アイドルの回収の条件、容量不足、Free Tier の制限）: https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm
- Free Tier（アップグレードしない限りカードに請求されない、トライアル終了時の A1 の扱い、登録にカードが必要）: https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier.htm
- 予算（予算は通知のみ、24 時間ごとに評価、ルートコンパートメントに作る）: https://docs.oracle.com/en-us/iaas/Content/Billing/Concepts/budgetsoverview.htm
- パブリック IP（予約済みのパブリック IP は削除するまで残る）: https://docs.oracle.com/en-us/iaas/Content/Network/Tasks/managingpublicIPs.htm
- 外向きの SMTP（25 番）の制限: https://docs.oracle.com/en-us/iaas/releasenotes/changes/f7e95770-9844-43db-916c-6ccbaf2cfe24 （検索結果で確認）
- DuckDNS（無料、TXT・IPv6 の更新 API）: https://www.duckdns.org/about.jsp 、 https://www.duckdns.org/spec.jsp
- Let's Encrypt（無料、HTTP-01 は 80 番のみ）: https://letsencrypt.org/docs/challenge-types/

---

## 한국어

### 0010. 서버는 Oracle Cloud Always Free(A1 우선), 도메인은 DuckDNS로 한다

- 날짜: 2026-11-05
- 상태: 제안
- 관련 요건: N-05 ①, N-06, N-07, N-08 (U-22)

### 배경

- 공개 URL로 누구나 쓸 수 있고, 도메인을 받아 공개한다(N-08). 통신은 HTTPS(N-05 ①). 가용성 목표는 두지 않고 장애는 수동으로 복구한다(N-06).
- 운영 비용은 월 0원(N-07). 메모리 등 서버 제약 값도 여기서 정한다(U-22).
- 사람의 결정 T-03: **Oracle Cloud 무료 티어의 ARM(A1)을 우선하고, 만들 수 없으면 AMD(1GB)로 한다.** 사람의 희망: 도메인은 DuckDNS.
- 구성: Spring Boot(Java 21), Angular(빌드한 정적 파일), MySQL 8.4. ADR 0005에서 앱도 컨테이너로 돌릴 수 있다(`docker compose --profile app`).

### 결정

#### 1. 계정과 리전

- Oracle Cloud **Free Tier 계정**을 만들고 **유료 계정(Pay As You Go)으로 업그레이드하지 않는다**. Free Tier 계정은 업그레이드하지 않는 한 카드에 청구되지 않는다(공식).
- 홈 리전은 **South Korea Central(서울)**로 한다(이용자가 서울에 있으므로. Always Free 인스턴스는 홈 리전에만 만들 수 있고, 홈 리전은 나중에 바꿀 수 없다).

#### 2. 서버 (T-03)

**1순위: Ampere A1(VM.Standard.A1.Flex) 1대, 2 OCPU·12 GB 메모리**(Always Free 상한을 모두 1대에 쓴다).

| 항목 | 값 |
|---|---|
| OS | Ubuntu 24.04 LTS(Arm판) |
| 부트 볼륨 | 50 GB(Always Free 블록 볼륨은 합계 200 GB까지) |
| 돌리는 것 | Nginx(호스트에 직접), 앱과 MySQL(Docker Compose 컨테이너) |

**메모리 배분(A1, 12 GB)**:

| 대상 | 제약 값 |
|---|---|
| 앱 컨테이너 | 메모리 상한 3 GB. JVM 힙 `-Xms2g -Xmx2g`(`-XX:+AlwaysPreTouch`로 시작할 때 확보) |
| MySQL 컨테이너 | 메모리 상한 3 GB. `innodb_buffer_pool_size=2G` |
| Nginx·OS 등 | 나머지(약 6 GB) |

JVM 힙을 시작할 때 확보하는 것은 아래 "유휴 인스턴스 회수" 조건(메모리 사용률 20% 미만)에 해당하지 않게 하기 위해서다(합계 약 4.5 GB 이상을 쓴다).

**2순위(A1을 만들 수 없을 때): AMD(VM.Standard.E2.1.Micro) 2대**(1/8 OCPU·1 GB 메모리씩. Always Free로 2대까지).

| 대상 | 제약 값 |
|---|---|
| 1번: Nginx + 앱 | JVM 힙 `-Xmx384m`, 앱 컨테이너 메모리 상한 640 MB, 스왑 2 GB |
| 2번: MySQL | `innodb_buffer_pool_size=256M`, `performance_schema=OFF`, 컨테이너 메모리 상한 700 MB, 스왑 2 GB |

A1을 만들 때 "용량 부족(out of host capacity)"이 나오면 시간을 두고 몇 번 다시 만든다. 그래도 안 되면 2순위로 한다(언제 바꿀지는 사람이 정한다).

#### 3. 도메인과 HTTPS

- 도메인은 **DuckDNS** 서브도메인(`{이름}.duckdns.org`. 이름은 사람이 정한다).
- IP 주소는 인스턴스에 **예약된 공인 IP**를 붙여 바뀌지 않게 한다. 바뀌면 DuckDNS 갱신 API(HTTPS GET)로 갱신한다.
- 인증서는 **Let's Encrypt**(무료). HTTP-01 확인에는 80번 포트가 필요하므로 Nginx에서 80·443번을 열고, 80번은 HTTPS로 넘긴다. 인증서 갱신은 자동으로 한다.
- Nginx 역할: HTTPS 종단, Angular 정적 파일 제공, `/api`를 앱으로 전달(같은 오리진으로 한다. 0013의 CSRF·쿠키 전제).

#### 4. 사용량 상한·예산 알림 (T-01)

- 상한: Free Tier 계정 그대로 쓴다(업그레이드하지 않는다). **Always Free 범위를 넘는 리소스는 만들 수 없고 청구도 생기지 않는다**. A1은 합계 2 OCPU·12 GB를 넘겨 만들지 않는다.
- 예산 알림: OCI "예산(Budgets)"을 루트 컴파트먼트에 만든다. 월 예산 1 USD, 실제 비용이 1%를 넘으면 이 프로젝트 전용 메일 주소로 알린다. Free Tier 계정에서 예산을 만들 수 없으면 그 사실을 운영 절차에 적는다(업그레이드하지 않는 한 청구되지 않으므로 상한은 지켜진다).

### 검토한 선택지

| 선택지 | 무료 범위 | 카드 | 판단 |
|---|---|---|---|
| **Oracle Cloud Always Free(A1 → AMD)** | A1: 월 1,500 OCPU 시간·9,000 GB 시간(2 OCPU·12 GB 상당). AMD: 2대(1 GB씩). 블록 볼륨 합계 200 GB. 외부로 나가는 통신 월 10 TB | 가입에 필요. 업그레이드하지 않는 한 청구되지 않음 | **채택 제안**(T-03 결정과 사람의 희망대로) |
| 다른 클라우드 무료 티어(기간 한정) | 12개월 등 기한이 있음 | 필요 | 기한 후 유료가 되어 월 0원을 이어갈 수 없다 |
| 집 PC로 공개 | — | — | 상시 가동·회선·보안 부담이 크다. 공개 URL의 안정적 공개에 맞지 않는다 |

| 도메인 선택지 | 비용 | 판단 |
|---|---|---|
| **DuckDNS 서브도메인** | 무료 | **채택 제안**(사람의 희망). A 레코드와 TXT 레코드를 API로 갱신할 수 있다 |
| 자체 도메인 구매 | 연간 비용이 든다 | 월 0원을 충족하지 못한다 |

### 결과 (좋은 점·주의점)

#### 비용 (월 0원 범위인가)

| 항목 | 내용 |
|---|---|
| 무료 한도 | 위 표대로(Always Free는 계정이 있는 한 기한 없음) |
| 카드 등록 | Oracle Cloud: 필요(본인 확인). DuckDNS·Let's Encrypt: 불필요 |
| 한도를 넘었을 때 | Free Tier 계정에서는 Always Free를 넘는 리소스를 쓸 수 없다(청구되지 않음). 무료 체험이 끝날 때 A1이 상한을 넘으면 인스턴스는 비활성화되고 30일 뒤 삭제된다(공식) |
| 예산 알림 | OCI 예산 1 USD, 1%에서 알림. **예산은 지출을 멈추지 않는다**(공식). 멈추는 것은 "업그레이드하지 않는 것"으로 한다 |

#### 좋은 점

- 기한 없이 무료로 쓸 수 있고, 업그레이드하지 않는 한 청구가 생기지 않는다.
- A1이면 12 GB가 있어 Spring Boot와 MySQL을 1대에서 여유 있게 돌릴 수 있다.

#### 주의점

- **유휴 인스턴스 회수**: 7일 동안 CPU 사용률(95 백분위) 20% 미만·네트워크 사용률 20% 미만·메모리 사용률 20% 미만(A1만)이 모두 해당하면 Oracle이 회수할 수 있다(공식). A1에서는 메모리 배분으로 이를 피하지만, Oracle의 측정 방법 세부는 공개되어 있지 않다. **MySQL 백업을 매일 받고, 서버를 다시 만드는 절차를 운영 절차(`docs/ops/`)에 쓴다**(N-06 수동 복구). AMD에는 메모리 조건이 없어 회수될 수 있다.
- A1은 홈 리전에서 용량 부족이 될 수 있다(공식).
- 업그레이드하지 않으므로 Free Tier 계정 제한(VCN 2개까지, Object Storage 합계 20 GB 등) 안에서 쓴다.
- 운영에서는 Docker Compose로 앱과 MySQL을 돌린다(ADR 0005의 `--profile app`과 같은 이미지). 서버를 만드는 구체적인 방법은 운영 절차에 쓴다.
- DuckDNS는 `duckdns.org`의 서브도메인이며 직접 등록한 도메인이 아니다. N-08의 "도메인을 받아서 공개한다"를 이것으로 충족한다고 해도 되는지 사람에게 확인한다.
- DuckDNS는 SLA 등을 공개하지 않는다. 멈추면 수동으로 대응한다(N-06).
- OCI는 2021년 6월 이후 만든 테넌시의 외부 25번 포트를 막는다. 메일 발송은 587번을 쓴다(0012).

#### 확인한 공식 문서 (확인일: 2026-11-05)

- Always Free 리소스(A1 시간과 2 OCPU·12 GB 상당, AMD 2대, 블록 볼륨 200 GB, 외부 10 TB, 유휴 회수 조건, 용량 부족, Free Tier 제한): https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm
- Free Tier(업그레이드하지 않는 한 카드에 청구되지 않음, 체험 종료 때 A1 처리, 가입에 카드 필요): https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier.htm
- 예산(알림만, 24시간마다 평가, 루트 컴파트먼트에 만듦): https://docs.oracle.com/en-us/iaas/Content/Billing/Concepts/budgetsoverview.htm
- 공인 IP(예약된 공인 IP는 삭제할 때까지 남음): https://docs.oracle.com/en-us/iaas/Content/Network/Tasks/managingpublicIPs.htm
- 외부 SMTP(25번) 제한: https://docs.oracle.com/en-us/iaas/releasenotes/changes/f7e95770-9844-43db-916c-6ccbaf2cfe24 (검색 결과로 확인)
- DuckDNS(무료, TXT·IPv6 갱신 API): https://www.duckdns.org/about.jsp , https://www.duckdns.org/spec.jsp
- Let's Encrypt(무료, HTTP-01은 80번만): https://letsencrypt.org/docs/challenge-types/
