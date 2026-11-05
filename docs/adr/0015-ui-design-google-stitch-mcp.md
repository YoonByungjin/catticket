# 0015. 画面のデザインは Google Stitch を公式の MCP サーバー経由で使い、使えないときは Angular Material の標準の見た目で作る

- 日付: 2026-11-05
- 状態: 採用（2026-11-05、人間が決定）
- 関係する要件: 画面設計全般（F-01〜F-35 の画面, N-02）。開発の道具であり、本番の運用（N-07）には含まれない。Issue #14 の追加項目（2026-11-05 人間の決定）

## 背景

- 画面の見た目（デザイン）の担当と道具を決めることになった（Issue #14 の追加項目）。人間の希望は、**Google Stitch を公式の MCP サーバー経由で ui-designer エージェントから使う**こと。
- Google Stitch は Google Labs の実験（experiment）として公開された、文章や画像から画面のデザインとフロントエンドのコードを作るサービス（Google Developers Blog、2025-05-20）。
- 仕様の正は `docs/design/` の設計書だけ（CLAUDE.md）。画面の項目・操作・状態は画面設計（02、architect）で決める。
- 画面の部品は Angular Material を使う提案（0014）。
- 費用は月0円（U-12, N-07）。開発の道具も有料のプランを使わない。
- ui-designer エージェントと Stitch の MCP 接続は別の Issue（chore）で用意する。**この ADR では方式だけを決め、`.claude/` などの設定は変えない。**

## 決定

1. 画面のデザインは **Google Stitch** で作る。ui-designer エージェントが、Google の**公式のリモート MCP サーバー**（`https://stitch.googleapis.com/mcp`。公式の SDK の既定値）を経由して使う。非公式の MCP サーバー・プロキシ（コミュニティの npm パッケージなど）は使わない。
2. 認証は Stitch の **API キー**を使う。API キーは秘密情報として扱い、`.env` → 環境変数で渡し、リポジトリにコミットしない（CLAUDE.md のセキュリティ）。キーはデザインの作業（開発 PC）だけで使い、`frontend/`・`backend/` には入れない。
3. **Stitch の生成物の位置づけ**:
   - 生成物（画面の画像・HTML・CSS）は**見た目の参考**であり、仕様ではない。画面の項目・操作・状態は 02 が正で、生成物と 02 が食い違うときは 02 に従う。
   - 生成された HTML・CSS は `frontend/` にそのまま貼らない。frontend-dev が Angular Material（0014）の部品とテーマで作り直す。
   - Stitch への指示（プロンプト）には、Angular Material（Material 3）の部品で作れる形にすること、PC とスマートフォンの両方（N-02）の画面を作ることを含める。
4. **生成回数の管理（T-01 の代わり）**: Stitch には有料のプランがなく、生成回数を買い足す方法もない（Google のフォーラムでの Google 側の回答、2025-08-25）。そのため請求は発生せず、カード登録・予算アラートは不要。上限は Stitch 側でかかる。こちらで決めること:
   - 作業の前に、人間が Stitch の画面で、その時点の上限と残りの回数を確かめる（公式のドキュメントで上限の数字を確認できなかったため。下の「確認したこと」）。
   - ui-designer は作業の報告に、使った生成の回数を書く。
   - 上限に達した（MCP の呼び出しが `RATE_LIMITED` などで失敗した）ときは、**作業を止めて報告する**。別のアカウントを使って上限を超えることはしない。
5. **Stitch が使えなくなったときの代わり（Labs のサービスであることのリスク）**: Stitch の仕様の変更・上限の変更・終了で使えなくなったときは、次の順で進める。
   1. 一時的な上限なら、回数が戻るのを待つ。
   2. 長く使えない・終了したときは、**デザインツールを使わず、Angular Material の標準の見た目（0014 の Material 3 のテーマ）で作る。** 画面の配置は 02 の項目と画面遷移で決まるため、デザインツールがなくても実装できる。
   3. 別のデザインツール（例: Figma の MCP サーバー）が必要になったときは、新しい ADR を書く（下の「検討した選択肢」のとおり、無料の範囲では制限が大きい）。
   - Stitch が終了しても作ったデザインが失われないように、**生成物は作ったときにリポジトリへ保存する**（置き場所は「人間・実行 Claude が別 Issue で決めること」）。

## 検討した選択肢

| 選択肢 | エージェントからの利用 | 費用・カード | 判断 |
|---|---|---|---|
| **Google Stitch（公式のリモート MCP サーバー）** | Google の公式の SDK の既定の接続先がリモート MCP サーバー。API キー、または OAuth のアクセストークン + Google Cloud のプロジェクトで認証する | 無料（Google Labs）。有料のプランはなく、回数を買い足す方法もない。カード不要 | **採用を提案（人間の希望）。** 費用がかからない。ただし Labs の実験のため、変更・終了のリスクがある |
| Figma（Starter プラン + Figma の MCP サーバー） | 公式のリモート MCP サーバーは Starter プランでも使える。ただし Starter の View・Collab のシートは**月20回まで**。キャンバスへの書き込みは、下書き以外では Full シート（有料）が必要。書き込みの機能はベータで「いずれ従量課金の有料機能になる」 | Starter は無料。書き込み・回数を増やすには有料のシート | 無料の範囲では回数が少なく、将来は有料になると公式にあるため、代わりの候補にとどめる |
| デザインツールを使わない（Angular Material の標準の見た目 + 02 の画面設計） | ツールは不要 | 無料 | Stitch が使えないときの代わりにする（決定 5）。見た目の工夫は少なくなる |
| 人間が手で描く・ほかのツールで描く | エージェントから使えない | ツールによる | 人間の希望（エージェントから使う）と合わない |

## 結果（良い点・注意点）

### 費用（月0円の範囲か）

| 項目 | 内容 |
|---|---|
| 無料枠 | Google Labs として無料で公開されている。**月の生成回数の上限は、公式のドキュメントで数字を確認できなかった**（下の「確認したこと」） |
| カード登録 | 不要（有料のプランがない） |
| 上限を超えたとき | 生成ができなくなる（Stitch 側の制限）。課金はされない。回数を買い足す方法はない |
| 上限・アラートの設定（T-01） | 課金がないため予算アラートは不要。回数は決定 4 のとおり報告で管理し、上限に達したら作業を止める |
| 本番の運用費（N-07） | 開発のときだけ使う道具で、本番のサーバー・画面からは呼ばない。運用費に含まれない |

### 良い点

- エージェントから直接デザインを作れ、費用がかからない。
- 生成物を見た目の参考にとどめ、実装は Angular Material で行うため、Stitch が終わっても画面の実装は影響を受けない。

### 注意点

- **Labs の実験のサービス**であり、仕様・上限が予告なく変わる、またはサービスが終了するおそれがある。公式の SDK のリポジトリにも「Google の公式にサポートされた製品ではない」とある。決定 5 の代わりの方法で対応する。
- 上限の数字は公式で確認できず、非公式の記事どうしでも数字が違う（例: 月に Standard 350 回・Experimental 50 回、Experimental 200 回、1日のクレジット制など）。Google 側はフォーラムで「上限を常に見直している」と書いている。数字に頼らず、決定 4 のとおり作業の前に Stitch の画面で確かめる。
- Stitch への指示や画像は Google のサービスに送られる。指示には秘密情報・個人情報（API キー、利用者のデータなど）を入れない。Stitch・Google Labs の利用規約は、ページが JavaScript で表示されるため本文を確認できなかった。接続を用意する chore の Issue で、人間が規約を確認する。
- 生成された HTML・CSS は Angular Material の部品で作られていない。見た目の参考として使い、実装は作り直す（決定 3）。
- MCP のツールの一覧・引数は、公式の SDK の README では一部（`create_project`・`generate_screen_from_text`・`get_screen` など）しか確認できなかった。実際の一覧は接続したときに確かめる。

### 人間・実行 Claude が別 Issue で決めること

この ADR では決めない。CLAUDE.md のエージェント体制・書き込み範囲に関わるため、ui-designer エージェントと MCP 接続を用意する chore の Issue で決める。

- ui-designer の担当工程と書き込んでよい場所（生成物の保存先。例: `docs/design/` の下に置くか。02 は architect の担当なので、ui-designer が 02 を変えてよいか）。
- 作業の順番（例: architect が 02 を書く → ui-designer がデザインを作る → frontend-dev が実装する）と、デザインを reviewer・人間が確認するゲートの扱い。
- MCP サーバーの設定の置き場所（Claude Code のプロジェクト用の `.mcp.json` か、各自のローカルの設定か）と、API キーを環境変数から渡す方法。
- サブエージェントはバックグラウンドで動き権限の承認を求められない（CLAUDE.md）ため、Stitch の MCP のツールを `.claude/settings.json` で許可するか。Claude Code はプロジェクト用の `.mcp.json` のサーバーを使う前に承認を求める（公式）ことへの対応。
- Stitch に使う Google アカウント（このプロジェクト専用のものを使うか）と、API キーの発行の手順（公式の手順を確認する）。

### 確認したこと（確認日: 2026-11-05）

公式・Google 側の資料:

- Stitch の発表（Google Labs の実験、Gemini を使う、Figma への貼り付け・フロントエンドのコードの出力。2025-05-20）: https://developers.googleblog.com/stitch-a-new-way-to-design-uis/
- Stitch の公式の SDK（`google-labs-code/stitch-sdk`。MCP サーバーの既定の URL `https://stitch.googleapis.com/mcp`、`STITCH_API_KEY` または OAuth のアクセストークン + `GOOGLE_CLOUD_PROJECT` での認証、`RATE_LIMITED` のエラー、「Google の公式にサポートされた製品ではない」、Apache 2.0）: https://github.com/google-labs-code/stitch-sdk
- Google AI Developers Forum の Stitch の質問への Google 側の回答（Experimental モードには上限がある（2025-06-17）、回数を支払いで増やす方法はない（2025-08-25）、上限を常に見直している（2025-08-29））: https://discuss.ai.google.dev/t/is-there-rate-limiting-stitch-stopped-working-on-one-email-had-to-open-incognito-with-a-different-email-to-make-it-work/88262
- Claude Code の MCP（HTTP のリモートサーバーの追加、ヘッダーの指定、local・project（`.mcp.json`）・user の範囲、プロジェクト用のサーバーは使う前に承認を求める、サブエージェントの `tools` での指定）: https://code.claude.com/docs/en/mcp
- Figma の MCP サーバー（使えるプラン、書き込みには Full シート、書き込みはベータで将来は従量課金）: https://help.figma.com/hc/en-us/articles/32132100833559-Guide-to-the-Figma-MCP-server
- Figma の MCP サーバーの回数の上限（Starter の View・Collab は月20回）: https://developers.figma.com/docs/figma-mcp-server/rate-limits-access/

確認できなかったこと:

- Stitch の公式のドキュメント（`https://stitch.withgoogle.com/docs/mcp/setup` など）と利用規約は、ページが JavaScript で表示されるため本文を読めなかった。**月の生成回数の上限、API キーの発行の手順、利用規約（データの扱い・終了のときの扱い）は公式で確認できていない。**
- 非公式の記事の上限の数字（参考。仕様の根拠にしない）: https://www.toolworthy.ai/tool/stitch-by-google 、https://www.banani.co/blog/google-stitch-pricing-and-credits

---

## 한국어

### 0015. 화면 디자인은 Google Stitch를 공식 MCP 서버를 통해 쓰고, 쓸 수 없을 때는 Angular Material 기본 겉모습으로 만든다

- 날짜: 2026-11-05
- 상태: 채택(2026-11-05, 사람이 결정)
- 관련 요건: 화면 설계 전반(F-01~F-35의 화면, N-02). 개발 도구이며 운영(N-07)에는 포함되지 않는다. Issue #14의 추가 항목(2026-11-05 사람의 결정)

### 배경

- 화면 겉모습(디자인)의 담당과 도구를 정하게 되었다(Issue #14의 추가 항목). 사람의 희망은 **Google Stitch를 공식 MCP 서버를 통해 ui-designer 에이전트에서 쓰는 것**.
- Google Stitch는 Google Labs의 실험(experiment)으로 공개된, 글이나 이미지로 화면 디자인과 프론트엔드 코드를 만드는 서비스(Google Developers Blog, 2025-05-20).
- 사양의 기준은 `docs/design/`의 설계서뿐(CLAUDE.md). 화면의 항목·조작·상태는 화면 설계(02, architect)에서 정한다.
- 화면 부품은 Angular Material을 쓰는 제안(0014).
- 비용은 월 0원(U-12, N-07). 개발 도구도 유료 플랜을 쓰지 않는다.
- ui-designer 에이전트와 Stitch MCP 연결은 별도 Issue(chore)에서 준비한다. **이 ADR에서는 방식만 정하고 `.claude/` 등의 설정은 바꾸지 않는다.**

### 결정

1. 화면 디자인은 **Google Stitch**로 만든다. ui-designer 에이전트가 Google의 **공식 원격 MCP 서버**(`https://stitch.googleapis.com/mcp`. 공식 SDK의 기본값)를 통해 쓴다. 비공식 MCP 서버·프록시(커뮤니티 npm 패키지 등)는 쓰지 않는다.
2. 인증은 Stitch **API 키**를 쓴다. API 키는 비밀 정보로 다루고 `.env` → 환경변수로 넘기며 저장소에 커밋하지 않는다(CLAUDE.md의 보안). 키는 디자인 작업(개발 PC)에서만 쓰고 `frontend/`·`backend/`에는 넣지 않는다.
3. **Stitch 생성물의 위치**:
   - 생성물(화면 이미지·HTML·CSS)은 **겉모습의 참고**이며 사양이 아니다. 화면의 항목·조작·상태는 02가 기준이고, 생성물과 02가 다르면 02를 따른다.
   - 생성된 HTML·CSS는 `frontend/`에 그대로 붙이지 않는다. frontend-dev가 Angular Material(0014)의 부품과 테마로 다시 만든다.
   - Stitch에 주는 지시(프롬프트)에는 Angular Material(Material 3) 부품으로 만들 수 있는 형태로 할 것, PC와 스마트폰 둘 다(N-02)의 화면을 만들 것을 넣는다.
4. **생성 횟수 관리(T-01 대신)**: Stitch에는 유료 플랜이 없고 생성 횟수를 더 살 방법도 없다(Google 포럼에서의 Google 측 답변, 2025-08-25). 그래서 청구는 생기지 않고 카드 등록·예산 알림은 필요 없다. 상한은 Stitch 쪽에서 걸린다. 우리가 정하는 것:
   - 작업 전에 사람이 Stitch 화면에서 그 시점의 상한과 남은 횟수를 확인한다(공식 문서에서 상한 숫자를 확인하지 못했기 때문. 아래 "확인한 것").
   - ui-designer는 작업 보고에 쓴 생성 횟수를 적는다.
   - 상한에 닿았을 때(MCP 호출이 `RATE_LIMITED` 등으로 실패했을 때)는 **작업을 멈추고 보고한다**. 다른 계정을 써서 상한을 넘지 않는다.
5. **Stitch를 쓸 수 없게 됐을 때의 대안(Labs 서비스라서 생기는 위험)**: Stitch의 사양 변경·상한 변경·종료로 쓸 수 없게 되면 다음 순서로 진행한다.
   1. 일시적인 상한이면 횟수가 돌아오기를 기다린다.
   2. 오래 쓸 수 없거나 종료됐으면 **디자인 도구를 쓰지 않고 Angular Material 기본 겉모습(0014의 Material 3 테마)으로 만든다.** 화면 배치는 02의 항목과 화면 전이로 정해지므로 디자인 도구가 없어도 구현할 수 있다.
   3. 다른 디자인 도구(예: Figma MCP 서버)가 필요해지면 새 ADR을 쓴다(아래 "검토한 선택지"대로 무료 범위에서는 제한이 크다).
   - Stitch가 종료돼도 만든 디자인이 사라지지 않도록 **생성물은 만들 때 저장소에 저장한다**(위치는 "사람·실행 Claude가 별도 Issue에서 정할 것").

### 검토한 선택지

| 선택지 | 에이전트에서의 이용 | 비용·카드 | 판단 |
|---|---|---|---|
| **Google Stitch(공식 원격 MCP 서버)** | Google 공식 SDK의 기본 접속처가 원격 MCP 서버. API 키, 또는 OAuth 액세스 토큰 + Google Cloud 프로젝트로 인증 | 무료(Google Labs). 유료 플랜이 없고 횟수를 더 살 방법도 없다. 카드 불필요 | **채택 제안(사람의 희망).** 비용이 들지 않는다. 다만 Labs 실험이라 변경·종료 위험이 있다 |
| Figma(Starter 플랜 + Figma MCP 서버) | 공식 원격 MCP 서버는 Starter 플랜에서도 쓸 수 있다. 다만 Starter의 View·Collab 시트는 **월 20회까지**. 캔버스 쓰기는 초안 외에는 Full 시트(유료)가 필요. 쓰기 기능은 베타이며 "언젠가 종량 과금 유료 기능이 된다" | Starter는 무료. 쓰기·횟수를 늘리려면 유료 시트 | 무료 범위에서는 횟수가 적고, 장래 유료가 된다고 공식에 있어 대안 후보에 그친다 |
| 디자인 도구를 쓰지 않는다(Angular Material 기본 겉모습 + 02 화면 설계) | 도구 불필요 | 무료 | Stitch를 쓸 수 없을 때의 대안으로 한다(결정 5). 겉모습의 고민은 적어진다 |
| 사람이 손으로 그리거나 다른 도구로 그린다 | 에이전트에서 쓸 수 없다 | 도구에 따라 다름 | 사람의 희망(에이전트에서 쓴다)과 맞지 않는다 |

### 결과 (좋은 점·주의점)

#### 비용 (월 0원 범위인가)

| 항목 | 내용 |
|---|---|
| 무료 한도 | Google Labs로 무료 공개되어 있다. **월 생성 횟수 상한은 공식 문서에서 숫자를 확인하지 못했다**(아래 "확인한 것") |
| 카드 등록 | 불필요(유료 플랜이 없음) |
| 한도를 넘었을 때 | 생성을 할 수 없게 된다(Stitch 쪽 제한). 과금되지 않는다. 횟수를 더 살 방법은 없다 |
| 상한·알림 설정(T-01) | 과금이 없으므로 예산 알림은 불필요. 횟수는 결정 4대로 보고로 관리하고 상한에 닿으면 작업을 멈춘다 |
| 운영비(N-07) | 개발 때만 쓰는 도구이며 운영 서버·화면에서는 호출하지 않는다. 운영비에 포함되지 않는다 |

#### 좋은 점

- 에이전트에서 바로 디자인을 만들 수 있고 비용이 들지 않는다.
- 생성물은 겉모습 참고에 그치고 구현은 Angular Material로 하므로, Stitch가 끝나도 화면 구현은 영향을 받지 않는다.

#### 주의점

- **Labs 실험 서비스**라서 사양·상한이 예고 없이 바뀌거나 서비스가 끝날 수 있다. 공식 SDK 저장소에도 "Google이 공식 지원하는 제품이 아니다"라고 되어 있다. 결정 5의 대안으로 대응한다.
- 상한 숫자는 공식으로 확인하지 못했고, 비공식 기사끼리도 숫자가 다르다(예: 월 Standard 350회·Experimental 50회, Experimental 200회, 하루 크레딧 제도 등). Google 측은 포럼에서 "상한을 계속 검토하고 있다"고 썼다. 숫자에 기대지 말고 결정 4대로 작업 전에 Stitch 화면에서 확인한다.
- Stitch에 보내는 지시나 이미지는 Google 서비스로 전송된다. 지시에는 비밀 정보·개인 정보(API 키, 이용자 데이터 등)를 넣지 않는다. Stitch·Google Labs 이용 약관은 페이지가 JavaScript로 표시되어 본문을 확인하지 못했다. 연결을 준비하는 chore Issue에서 사람이 약관을 확인한다.
- 생성된 HTML·CSS는 Angular Material 부품으로 만들어진 것이 아니다. 겉모습 참고로 쓰고 구현은 다시 만든다(결정 3).
- MCP 도구 목록·인수는 공식 SDK README에서 일부(`create_project`·`generate_screen_from_text`·`get_screen` 등)만 확인했다. 실제 목록은 연결할 때 확인한다.

#### 사람·실행 Claude가 별도 Issue에서 정할 것

이 ADR에서는 정하지 않는다. CLAUDE.md의 에이전트 체제·쓰기 범위와 관련되므로, ui-designer 에이전트와 MCP 연결을 준비하는 chore Issue에서 정한다.

- ui-designer의 담당 공정과 써도 되는 곳(생성물 저장 위치. 예: `docs/design/` 아래에 둘지. 02는 architect 담당이므로 ui-designer가 02를 바꿔도 되는지).
- 작업 순서(예: architect가 02를 쓴다 → ui-designer가 디자인을 만든다 → frontend-dev가 구현한다)와, 디자인을 reviewer·사람이 확인하는 관문의 처리.
- MCP 서버 설정 위치(Claude Code 프로젝트용 `.mcp.json`인지, 각자의 로컬 설정인지)와 API 키를 환경변수로 넘기는 방법.
- 서브 에이전트는 백그라운드에서 돌아 권한 승인을 받을 수 없으므로(CLAUDE.md), Stitch MCP 도구를 `.claude/settings.json`에서 허용할지. Claude Code는 프로젝트용 `.mcp.json`의 서버를 쓰기 전에 승인을 요구한다(공식)는 점에 대한 대응.
- Stitch에 쓸 Google 계정(이 프로젝트 전용 계정을 쓸지)과 API 키 발급 절차(공식 절차를 확인한다).

#### 확인한 것 (확인일: 2026-11-05)

공식·Google 측 자료:

- Stitch 발표(Google Labs 실험, Gemini 사용, Figma에 붙여넣기·프론트엔드 코드 출력. 2025-05-20): https://developers.googleblog.com/stitch-a-new-way-to-design-uis/
- Stitch 공식 SDK(`google-labs-code/stitch-sdk`. MCP 서버 기본 URL `https://stitch.googleapis.com/mcp`, `STITCH_API_KEY` 또는 OAuth 액세스 토큰 + `GOOGLE_CLOUD_PROJECT`로 인증, `RATE_LIMITED` 오류, "Google이 공식 지원하는 제품이 아니다", Apache 2.0): https://github.com/google-labs-code/stitch-sdk
- Google AI Developers Forum의 Stitch 질문에 대한 Google 측 답변(Experimental 모드에는 상한이 있다(2025-06-17), 결제로 횟수를 늘리는 방법은 없다(2025-08-25), 상한을 계속 검토하고 있다(2025-08-29)): https://discuss.ai.google.dev/t/is-there-rate-limiting-stitch-stopped-working-on-one-email-had-to-open-incognito-with-a-different-email-to-make-it-work/88262
- Claude Code MCP(HTTP 원격 서버 추가, 헤더 지정, local·project(`.mcp.json`)·user 범위, 프로젝트용 서버는 쓰기 전에 승인을 요구, 서브 에이전트 `tools`에서의 지정): https://code.claude.com/docs/en/mcp
- Figma MCP 서버(쓸 수 있는 플랜, 쓰기에는 Full 시트, 쓰기는 베타이며 장래 종량 과금): https://help.figma.com/hc/en-us/articles/32132100833559-Guide-to-the-Figma-MCP-server
- Figma MCP 서버 횟수 상한(Starter의 View·Collab은 월 20회): https://developers.figma.com/docs/figma-mcp-server/rate-limits-access/

확인하지 못한 것:

- Stitch 공식 문서(`https://stitch.withgoogle.com/docs/mcp/setup` 등)와 이용 약관은 페이지가 JavaScript로 표시되어 본문을 읽지 못했다. **월 생성 횟수 상한, API 키 발급 절차, 이용 약관(데이터 처리·종료 때의 처리)은 공식으로 확인하지 못했다.**
- 비공식 기사의 상한 숫자(참고. 사양 근거로 쓰지 않음): https://www.toolworthy.ai/tool/stitch-by-google , https://www.banani.co/blog/google-stitch-pricing-and-credits
