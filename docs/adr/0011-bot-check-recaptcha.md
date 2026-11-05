# 0011. 自動操作ではないことの確認は Google reCAPTCHA（課金を有効にしない専用プロジェクト）で行う

- 日付: 2026-11-05
- 状態: 提案
- 関係する要件: F-14 ④(a)⑤, N-05 ③, N-07（R-19、7.3 の申し送り）

## 背景

- ロック中のアカウントは、(a)「自動操作ではないことの確認の通過」と「メールアドレスの再認証」の**両方**を完了するか、(b) パスワードを再設定すると解除される（F-14 ④⑤）。
- 人間の希望は Google reCAPTCHA（R-19）。費用は月0円の範囲（N-07）。
- 確認を使うのはロックの解除のときだけで、回数は少ない。
- reCAPTCHA は Google Cloud のプロジェクトで管理され、**課金を有効にしたプロジェクトでは有料の Premium として扱われる**（公式）。地図（0006）の Google Cloud プロジェクトは課金を有効にする。

## 決定

1. 確認には **Google reCAPTCHA** を使う。キーの種類は「ウェブサイト」で、**チェックボックスの確認（「私はロボットではありません」）** を使う（通過したかどうかがはっきり分かるため。点数だけを返す方式は使わない）。
2. reCAPTCHA は**地図とは別の Google Cloud プロジェクト**に作り、そのプロジェクトは**請求先アカウントにつながない（課金を有効にしない）**。これにより無料の Essentials（月 10,000 回まで）として扱われる。
3. サーバーでの検証は **reCAPTCHA Enterprise API の `CreateAssessment`** で行う。
   - 理由: 無料の上限を超えたとき、`CreateAssessment` は **エラー（429 / RESOURCE_EXHAUSTED）を返して失敗する**（安全側）。古い `siteverify` は上限を超えると `success:true` を返して**通過扱いになる**（公式）ため使わない。
   - 検証に使う API キーは、そのプロジェクトの reCAPTCHA Enterprise API だけに制限し、サーバーの `.env` に置く。
   - サイトキー（ブラウザに出る）は、使えるドメインを本番のドメイン（0010）と開発用の `localhost` に制限する。
4. 確認が失敗・エラーになったときは、(a) の解除はできない。利用者は (b) パスワードの再設定（F-15）で解除できる。
5. 呼び出しは REST API を Spring の HTTP クライアントで行う（Google のクライアントライブラリは入れない）。画面側で reCAPTCHA の部品をどう読み込むか（Angular 用のライブラリを使うか）は frontend の実装のときに判断し、ライブラリが必要なら別の ADR を書く。

## 検討した選択肢

| 選択肢 | 無料の範囲 | カード | 上限を超えたとき | 判断 |
|---|---|---|---|---|
| **Google reCAPTCHA（課金なしのプロジェクト、Essentials）** | 月 10,000 回（組織ごと。課金なしのプロジェクトどうしで共有） | 不要（課金を有効にしない） | `CreateAssessment` は 429 で失敗する。課金されない | **採用を提案**（人間の希望。上限を超えても請求されない） |
| Google reCAPTCHA（地図と同じ課金ありのプロジェクト） | 課金を有効にすると Premium になる | 必要 | Premium の料金で請求される | 使用量が少なくても請求の可能性が残るため採らない |
| Cloudflare Turnstile | 無料プランは確認の回数が無制限、ウィジェット 20 個まで | 不要。Cloudflare の DNS・プロキシを使わないサイトでも使える | — | 月0円を確実に満たす。人間の希望が reCAPTCHA なので第2候補 |
| 自作の確認（画像の文字など） | — | — | — | 自動操作への強さを保証できない |

## 結果（良い点・注意点）

### 費用（月0円の範囲か）

| 項目 | 内容 |
|---|---|
| 無料枠 | Essentials: 月 10,000 回の評価（組織ごと） |
| カード登録 | 不要（専用プロジェクトを請求先アカウントにつながない） |
| 上限を超えたとき | `CreateAssessment` が 429 を返す。課金されない。上限に近づいた・超えたときにメールで知らせが来る（公式） |
| 使用量の上限 | 課金を有効にしないことで、無料の上限がそのまま使用量の上限になる |
| 予算アラート | 請求先アカウントにつながないため予算は作れない（請求が発生しない）。代わりに、**このプロジェクトに請求先アカウントがつながっていないこと**を運用手順の確認項目にする |

### 良い点

- 人間の希望どおり reCAPTCHA を使い、請求が発生しない形にできる。
- 上限を超えても通過扱いにならず、安全側に失敗する。

### 注意点

- **地図のプロジェクトで reCAPTCHA のキーを作らないこと。** 課金ありのプロジェクトでは Premium として請求される。
- 無料の 10,000 回は組織の中の課金なしのプロジェクトで共有される。このアカウントで他に reCAPTCHA を使わない。
- 上限を超えて (a) が使えない月も、(b) パスワードの再設定でロックを解除できる。
- Google Cloud のプロジェクトと API キーの作成が必要になる（専用の Google アカウントで作る）。

### 確認した公式ドキュメント（確認日: 2026-11-05）

- reCAPTCHA の階層（Essentials は月 10,000 回まで無料、組織ごと）: https://docs.cloud.google.com/recaptcha/docs/compare-tiers
- reCAPTCHA の課金（課金なしのプロジェクトは 10,000 回を共有、超えると `CreateAssessment` は 429、課金を有効にすると Premium）: https://docs.cloud.google.com/recaptcha/docs/billing-information
- reCAPTCHA の割り当て（`siteverify` は上限超過で `success:true`・点数 0.9 を返す。`CreateAssessment` はエラー）: https://docs.cloud.google.com/recaptcha/quotas
- Cloudflare Turnstile の料金プラン: https://developers.cloudflare.com/turnstile/plans/
- Cloudflare Turnstile の始め方（Cloudflare のプロキシを使わないサイトでも使える）: https://developers.cloudflare.com/turnstile/get-started/

---

## 한국어

### 0011. 자동 조작이 아님을 확인하는 것은 Google reCAPTCHA(결제를 켜지 않은 전용 프로젝트)로 한다

- 날짜: 2026-11-05
- 상태: 제안
- 관련 요건: F-14 ④(a)⑤, N-05 ③, N-07 (R-19, 7.3 인계)

### 배경

- 잠긴 계정은 (a) "자동 조작이 아님 확인 통과"와 "이메일 재인증"을 **둘 다** 마치거나, (b) 비밀번호를 재설정하면 풀린다(F-14 ④⑤).
- 사람의 희망은 Google reCAPTCHA(R-19). 비용은 월 0원 범위(N-07).
- 확인은 잠금 해제 때만 쓰고 횟수가 적다.
- reCAPTCHA는 Google Cloud 프로젝트로 관리되며, **결제를 켠 프로젝트에서는 유료 Premium으로 취급된다**(공식). 지도(0006)의 Google Cloud 프로젝트는 결제를 켠다.

### 결정

1. 확인에는 **Google reCAPTCHA**를 쓴다. 키 종류는 "웹사이트"이고 **체크박스 확인("로봇이 아닙니다")**을 쓴다(통과 여부가 분명하기 때문. 점수만 돌려주는 방식은 쓰지 않는다).
2. reCAPTCHA는 **지도와 다른 Google Cloud 프로젝트**에 만들고, 그 프로젝트는 **결제 계정에 연결하지 않는다(결제를 켜지 않는다)**. 그러면 무료 Essentials(월 10,000회까지)로 취급된다.
3. 서버 검증은 **reCAPTCHA Enterprise API의 `CreateAssessment`**로 한다.
   - 이유: 무료 상한을 넘으면 `CreateAssessment`는 **오류(429 / RESOURCE_EXHAUSTED)를 돌려주며 실패한다**(안전한 쪽). 예전 `siteverify`는 상한을 넘으면 `success:true`를 돌려줘 **통과로 처리된다**(공식)므로 쓰지 않는다.
   - 검증에 쓰는 API 키는 그 프로젝트의 reCAPTCHA Enterprise API로만 제한하고 서버 `.env`에 둔다.
   - 사이트 키(브라우저에 노출)는 쓸 수 있는 도메인을 운영 도메인(0010)과 개발용 `localhost`로 제한한다.
4. 확인이 실패·오류가 나면 (a) 해제는 할 수 없다. 이용자는 (b) 비밀번호 재설정(F-15)으로 해제할 수 있다.
5. 호출은 REST API를 Spring HTTP 클라이언트로 한다(Google 클라이언트 라이브러리는 넣지 않는다). 화면에서 reCAPTCHA 부품을 어떻게 불러올지(Angular용 라이브러리를 쓸지)는 frontend 구현 때 판단하고, 라이브러리가 필요하면 별도 ADR을 쓴다.

### 검토한 선택지

| 선택지 | 무료 범위 | 카드 | 한도를 넘었을 때 | 판단 |
|---|---|---|---|---|
| **Google reCAPTCHA(결제 없는 프로젝트, Essentials)** | 월 10,000회(조직별. 결제 없는 프로젝트끼리 공유) | 불필요(결제를 켜지 않음) | `CreateAssessment`는 429로 실패. 과금되지 않음 | **채택 제안**(사람의 희망. 한도를 넘어도 청구되지 않음) |
| Google reCAPTCHA(지도와 같은 결제 있는 프로젝트) | 결제를 켜면 Premium이 됨 | 필요 | Premium 요금으로 청구 | 사용량이 적어도 청구 가능성이 남으므로 쓰지 않는다 |
| Cloudflare Turnstile | 무료 플랜은 확인 횟수 무제한, 위젯 20개까지 | 불필요. Cloudflare DNS·프록시를 쓰지 않는 사이트에서도 쓸 수 있음 | — | 월 0원을 확실히 충족. 사람의 희망이 reCAPTCHA이므로 2순위 |
| 직접 만든 확인(이미지 글자 등) | — | — | — | 자동 조작에 대한 강도를 보장할 수 없다 |

### 결과 (좋은 점·주의점)

#### 비용 (월 0원 범위인가)

| 항목 | 내용 |
|---|---|
| 무료 한도 | Essentials: 월 10,000회 평가(조직별) |
| 카드 등록 | 불필요(전용 프로젝트를 결제 계정에 연결하지 않음) |
| 한도를 넘었을 때 | `CreateAssessment`가 429를 돌려준다. 과금되지 않는다. 한도에 가까워지거나 넘으면 메일로 알림이 온다(공식) |
| 사용량 상한 | 결제를 켜지 않으므로 무료 한도가 그대로 사용량 상한이 된다 |
| 예산 알림 | 결제 계정에 연결하지 않으므로 예산을 만들 수 없다(청구가 생기지 않음). 대신 **이 프로젝트에 결제 계정이 연결되어 있지 않은지**를 운영 절차의 확인 항목으로 한다 |

#### 좋은 점

- 사람의 희망대로 reCAPTCHA를 쓰면서 청구가 생기지 않게 할 수 있다.
- 한도를 넘어도 통과로 처리되지 않고 안전한 쪽으로 실패한다.

#### 주의점

- **지도 프로젝트에서 reCAPTCHA 키를 만들지 말 것.** 결제 있는 프로젝트에서는 Premium으로 청구된다.
- 무료 10,000회는 조직 안의 결제 없는 프로젝트끼리 공유된다. 이 계정으로 다른 곳에 reCAPTCHA를 쓰지 않는다.
- 한도를 넘어 (a)를 쓸 수 없는 달에도 (b) 비밀번호 재설정으로 잠금을 풀 수 있다.
- Google Cloud 프로젝트와 API 키를 만들어야 한다(전용 Google 계정으로 만든다).

#### 확인한 공식 문서 (확인일: 2026-11-05)

- reCAPTCHA 등급(Essentials는 월 10,000회까지 무료, 조직별): https://docs.cloud.google.com/recaptcha/docs/compare-tiers
- reCAPTCHA 과금(결제 없는 프로젝트는 10,000회를 공유, 넘으면 `CreateAssessment`는 429, 결제를 켜면 Premium): https://docs.cloud.google.com/recaptcha/docs/billing-information
- reCAPTCHA 할당량(`siteverify`는 한도 초과 시 `success:true`·점수 0.9를 돌려줌. `CreateAssessment`는 오류): https://docs.cloud.google.com/recaptcha/quotas
- Cloudflare Turnstile 요금 플랜: https://developers.cloudflare.com/turnstile/plans/
- Cloudflare Turnstile 시작하기(Cloudflare 프록시를 쓰지 않는 사이트에서도 사용 가능): https://developers.cloudflare.com/turnstile/get-started/
