# 0018. 外部データの翻訳は Google Cloud Translation（Basic、v2）を使う（0007 を置き換える）

- 日付: 2026-11-19
- 状態: 採用（2026-11-19、人間が決定。Issue #39）
- 関係する要件: F-02 ⑥, F-11, N-01, N-07（U-14, U-13, R-58）
- 置き換える ADR: 0007（Azure AI Translator F0）

## 背景

- 0007 は Azure AI Translator の無料レベル（F0）を選んだが、人間は Microsoft のアカウントを作らないことにした（2026-11-19）。
- 次に DeepL API Free を選んだ（2026-11-19）が、architect の調べで、DeepL の公式（サポートの記事、更新 2026-11-18）に「The DeepL API Free plan can no longer be purchased.」とあり、今の無料のプラン（Developer）は**累計 100 万文字で、月ごとに戻らない**ことが分かった。0007 の見積もり（最初の月 約 92 万文字、2か月目以降 約 45 万文字/月）では、本番の収集で約 5 週間で使い切る。規約にも無料の API での個人データの処理の禁止（§8.3.11）がある（下の「検討した選択肢」）。
- PM が比べ直し（Issue #39 のコメント）、人間が **Google Cloud Translation** に決めた（2026-11-19、2回目）。理由: 無料の範囲（月 50 万文字）が毎月戻るので日本語の表示が続く。地図（0006）と同じ Google Cloud の請求先アカウントを使え、新しいアカウントが要らない。DeepL の規約の制限がない。
- 0007 の背景（収集のときに翻訳して保存する必要、毎日の収集、月0円と T-01）は変わらない。

## 決定

番号は 0007 の決定と同じ番号にそろえた（下の「0007 との対応」）。

1. 翻訳 API は **Google Cloud Translation - Basic（v2）**の `translate`（モデルは既定の NMT）を使う（韓国語 `ko` → 日本語 `ja`）。Advanced（v3）は使わない（v3 は API キーを使えず、サービスアカウントの認証のために新しいライブラリか署名の自作が要るため。下の「確認したこと」）。
2. **収集のときに翻訳して保存する**（B-01 の S7。05 の 3.7）。画面の表示のたびに API を呼ばない（0007 決定2 を引き継ぐ）。
3. **翻訳の量を減らす**（0007 決定3 をそのまま引き継ぐ）。
   - **同じ原文は二度翻訳しない**（原文のハッシュごとに翻訳結果を保存して使い回す。04 の DB-05）。
   - 公演の項目が変わったときは、**変わった項目だけ**翻訳し直す。
   - ジャンル・公演の状態など、値の種類が決まっているものは API で翻訳せず、画面の文言と同じ方法（多言語の辞書）で訳す。
   - 駅名は、ソウル交通公社の公式の日本語表記を使い、API で翻訳しない（0009）。
4. **使用量の上限（T-01）**。Google 側とアプリ側の二重で止める。
   - **Google 側**: Google Cloud コンソールの「割り当て（Quotas）」で、Cloud Translation API の**1日の文字数を 15,000 文字**に下げる（無料の範囲 50 万文字 ÷ 31 日 ≒ 16,129 文字。0006 決定4 と同じく 31 で割り、余裕を持たせて少なくする。31 日 × 15,000 = 46.5 万文字で、無料の範囲の 93%）。超えると 403（`Daily Limit Exceeded`）で止まり、その日の分は課金されない。1日は太平洋時間の 0 時に戻る（公式）。
   - **アプリ側（1回の実行）**: S7 の1回の実行で送る文字数を **15,000 文字**までにする（B-01 は1日1回。05 の 3.7）。普段はアプリが先に止まり、Google の割り当てに当たらない。
   - **アプリ側（月）**: 1か月の翻訳文字数を数え、**月 45 万文字**（無料の範囲 50 万文字の 90%）に達したらその月は翻訳を止める。月は Asia/Seoul の暦の月（P-09 のまま。理由は「注意点」）。
   - 止めた後に翻訳できなかった文は、次の収集で再び翻訳する（その間は韓国語の原文を表示する。02 の 3.4 の P-06。F-11 ①・R-58）。
5. **予算アラート（T-01）**: 0006 決定5 の予算（請求先アカウント全体、1 USD 相当、1%・50%・100% で通知）をそのまま使う。翻訳の課金もこの予算に含まれる。予算の「割引・クレジットを含める」は既定（含める）のままにする（無料の範囲は月 10 USD のクレジットとして引かれるので、含めないと無料の範囲の中でも通知が来る）。予算は使用量を止めない（公式）ので、止めるのは 4. で行う。
6. **キーと Google Cloud のプロジェクト**
   - 翻訳**専用の Google Cloud プロジェクト**を作り、地図（0006）と同じ請求先アカウントにつなぐ（課金を有効にする。Cloud Translation は課金の有効化が必須（公式））。0006 決定7（地図のプロジェクトは地図専用）と 0011（reCAPTCHA は課金なしの別プロジェクト）を変えないため、地図・reCAPTCHA のプロジェクトでは Cloud Translation API を有効にしない。
   - 認証は **API キー**（v2 は API キーを使える（公式））。キーには次の制限をかける: **API の制限**を Cloud Translation API だけにする。**アプリケーションの制限**を IP アドレスにし、本番のサーバーの予約済みのパブリック IP（0010）だけを許す。
   - キーはサーバーの環境変数（`.env` の `GOOGLE_TRANSLATE_API_KEY`）にだけ置く。ブラウザから翻訳 API を呼ばない。リクエストではキーを HTTP ヘッダー `x-goog-api-key` で送る（クエリ `key` は URL に出て漏れやすいと公式が書いているため使わない）。
7. 呼び出しは REST API を Spring の `RestClient` で行う。Google の SDK（クライアントライブラリ）は入れない（0007 決定7 を引き継ぐ。新しいライブラリを増やさない）。
8. **帰属の表示と HTML のマークアップ（Google Cloud の Service Specific Terms の 25.）**: 機械翻訳を表示するときは、Google の帰属の要件（「powered by Google Translate」の画像を翻訳の近くに置く、`translate.google.com` へのリンク、免責の文をサイトに置く）と HTML のマークアップの要件（機械翻訳の文の要素に `lang="ja-x-mtfrom-ko"`）を守る。画面のどこに何を出すかは 02 の 3.4（P-19）、機械翻訳かどうかを画面が見分ける項目は 03 の 7.2（`LocalizedText.machineTranslated`）、見た目は 07 で決める。

### 0007 との対応（他の文書が 0007 の番号で参照している箇所の読み替え）

| 0007 | 0018 | 変わったか |
|---|---|---|
| 決定1（Azure F0） | 決定1（Google Cloud Translation Basic v2） | 置き換え |
| 決定2（収集のときに翻訳して保存） | 決定2 | 同じ |
| 決定3・「3.」（同じ原文は一度だけ、変わった項目だけ、決まった値は画面の辞書、駅名は公式表記） | 決定3 | 同じ（そのまま引き継ぐ） |
| 決定4（月 180 万文字、`403001`） | 決定4（1日 15,000・1回 15,000・月 45 万文字、403） | 値と止め方を置き換え |
| 決定5（Azure の予算） | 決定5（0006 の予算を使う） | 置き換え |
| 決定6（キーはサーバーの `.env` だけ） | 決定6 | 方針は同じ。キーの制限・プロジェクトを追加 |
| 決定7（SDK を入れず HTTP クライアントで呼ぶ） | 決定7 | 同じ |
| 翻訳する文字数の見積もり | 下の「見積もりと上限の比較」 | 見積もりの値はそのまま引き継ぐ |
| — | 決定8（帰属の表示・マークアップ） | 追加 |

- 01・04 などが「ADR 0007 3.」「ADR 0007 決定3」と書いている内容（同じ原文は二度翻訳しない、決まった値は画面の辞書）は、この ADR の決定3 と同じ。参照を書き直すまでは、0007 の決定3 を 0018 の決定3 と読み替える。

## 見積もりと上限の比較

見積もりは 0007 の「翻訳する文字数の見積もり」をそのまま使う（仮定も同じ。実装の最初の収集で実測し、この ADR を見直す）。

| 時期 | 0007 の見積もり | この ADR の上限で翻訳できる量 | 結果 |
|---|---|---|---|
| 最初の月 | 約 92 万文字（初回 約 47 万 + 毎日 約 1.5 万 × 30） | 1回 15,000 × 1日1回 → 月 約 45 万文字（月の上限 45 万文字） | 約 47 万文字が翌月以降に残る。その間、残った文は韓国語の原文で表示する（人間が受け入れた。Issue #39） |
| 2か月目以降 | 約 45 万文字/月（毎日 約 1.5 万） | 月 約 45 万文字 | 見積もりとほぼ同じで、余裕がない。最初の月に残った分はほとんど減らない |

- 翻訳の順番は公演名 → 会場名 → 駅名 → それ以外（05 の 3.7）なので、上限に当たっても日本語のキーワード検索（F-02 ⑥）と原文を併記する項目（F-11 ②）が先に翻訳される。
- 実測が見積もりより多く、残りが増え続けるときは、この ADR を見直す（例: 翻訳する項目を減らす要件の変更、有料の範囲を認めるか）。人間が決める。

### 上限を超えそうなときの動き

| 状況 | 動き | 課金 |
|---|---|---|
| S7 の1回で 15,000 文字に達する | その回の S7 を止める（結果は成功、記録に「1回の上限」）。翌日の実行で続きを翻訳する | なし |
| 今月（Asia/Seoul）の合計が 45 万文字に達する | その月の S7 を止める（結果は成功、記録に「月の上限」）。翌月の最初の実行で続きを翻訳する | なし |
| アプリの数え間違い・手での再実行で、Google の1日の割り当て（15,000 文字）を超える | Google が 403 を返す。その回の S7 を止める（記録は失敗）。太平洋時間の 0 時に戻る | なし（割り当てを超えた要求は翻訳されない） |
| 割り当てを設定し忘れた・外した | アプリ側の上限だけで止まる。無料の範囲を超えた分は 100 万文字あたり 20 USD。予算アラート（決定5）で気づく | あり（防ぐため、運用手順の確認項目にする） |

## 検討した選択肢

| 選択肢 | 無料の範囲 | 上限を超えたとき | カード | 判断 |
|---|---|---|---|---|
| **Google Cloud Translation（Basic v2）** | 月 50 万文字（月 10 USD のクレジット。Basic と Advanced の合計。繰り越さない） | 超えた分は 100 万文字あたり 20 USD。割り当て（1日の文字数）を下げて止める | 必要（課金の有効化が必須。地図と同じ請求先アカウント） | **採用**（人間の決定）。毎月戻る。新しいアカウントが要らない |
| DeepL API Developer（無料） | **累計 100 万文字。戻らない**（公式。DeepL API Free は新しく申し込めない） | 使い切ったら有料の Growth へ移る必要がある | 公式に記載なし | 本番の収集で約 5 週間で使い切る。規約: §8.3.11（無料の API で個人データを処理しない。出演者名は実在の人の名前）、§8.3.3（表示する翻訳に DeepL のロゴか名前を示す）、§3.3.2（送った文と翻訳を DeepL が無期限に保存できる）、§3.3.3（無料の API の機能を予告なく変える・やめることがある） |
| DeepL API Growth | なし（有料。月額と使用量の課金） | 使用量で課金 | 必要 | 月0円（U-12, N-07）に合わない。料金の金額は公式で確かめていない |
| Azure AI Translator F0（0007） | 月 200 万文字 | `403001` で止まり課金されない | 必要（Azure のアカウント） | Microsoft のアカウントを作らない（人間の決定） |
| Amazon Translate | 月 200 万文字。**最初の翻訳の要求から 12 か月だけ** | 100 万文字あたり 15 USD | 必要（AWS のアカウント） | 1年後から有料。新しいクラウドのアカウントが増える |
| Papago（NAVER Cloud） | 公式で無料の範囲の記載を確認できなかった（PM の比較では無料の範囲なし） | 文字数で課金（公式） | 必要 | 月0円に合わない。新しいアカウントが増える |
| LibreTranslate（自分のサーバーで動かす） | 制限なし（OSS、AGPL v3。翻訳は Argos Translate） | — | 不要 | 韓国語・日本語の対応と必要なメモリは公式の README に記載がない。翻訳の品質（韓国語→日本語）の確認と、0010 のサーバーのメモリの見直しが要る |
| 翻訳しない | — | — | — | F-11 ①（すべて翻訳）を満たせない |

## 結果（良い点・注意点）

### 費用（月0円の範囲か）

| 項目 | 内容 |
|---|---|
| 無料の範囲 | 月 50 万文字（言語の検出と NMT の翻訳。月 10 USD のクレジットとして引かれ、繰り越さない。Basic と Advanced の合計） |
| 超えたときの料金 | 100 万文字あたり 20 USD |
| 数え方 | Unicode のコードポイントの数。空白も数える。空の要求も 1 文字として課金される（公式）。アプリも同じくコードポイントで数え、空の文は送らない |
| カード登録 | 必要（請求先アカウント。地図と同じもの） |
| 止める方法 | Google の1日の割り当て 15,000 文字（決定4）+ アプリの1回 15,000・月 45 万文字。**支出の上限（spend cap）の予算は Cloud Translation に使えない**（対象は Gemini API・Agent Platform・Cloud Run・Cloud Run functions だけ。公式） |
| 予算アラート | 0006 の予算（請求先アカウント全体、1 USD 相当）。使用量は止めない |

### 良い点

- 無料の範囲が毎月戻るので、上限で止まっても翌月に翻訳が続く。
- 地図と同じ請求先アカウント・予算で管理できる。新しいクラウドのアカウントが要らない。
- 送った文は Google の翻訳の学習・改善に使われない（公式）。
- 割り当てで1日の量を強制的に止められるので、アプリに誤りがあっても1か月 46.5 万文字（無料の範囲の 93%）を超えない。

### 注意点

- **余裕がない**: 2か月目以降の見積もり（約 45 万文字/月）が上限とほぼ同じ。最初の月に残った約 47 万文字はほとんど減らない。公演名から先に翻訳するので、残るのは主に出演者・チケット価格などの長い文になる。実測で見直す。
- **月の区切りのずれ（P-09）**: Google の請求の月は太平洋時間で区切られる（公式）。アプリの月（Asia/Seoul）とは 16〜17 時間ずれる。B-01 は 06:00（Asia/Seoul）に1日1回動き、1回の上限が 15,000 文字なので、ずれで Google の1つの月に入るのは最大で1回分（15,000 文字）多いだけ（45 万 + 1.5 万 = 46.5 万文字 < 50 万文字）。Google の1日の割り当ても太平洋時間の日で数えるので、Google の月の合計は 31 日 × 15,000 = 46.5 万文字を超えない。そのため P-09（Asia/Seoul の暦の月で数える）は変えない（理由だけを 02・05 で書き直した）。
- **割り当ての反映の遅れ**: 割り当ての適用には遅れがある（公式「Quota enforcement has a latency period」）。そのため 1日の割り当ては 16,129 ではなく 15,000 にした。
- **帰属の表示**が必要（決定8）。画面に「powered by Google Translate」の画像・リンク・免責の文の置き場所が増える（02・07）。
- 無料の範囲のクレジットが請求先アカウントごとかプロジェクトごとかは、公式に記載がない。安全のため、同じ請求先アカウントの他のプロジェクトで Cloud Translation を使わない。
- 1回の要求の送り方: v2 の応答が要求と同じ順に並ぶとは、v2 のリファレンスに明記がない（v3 の文書には明記がある）。そのため **1回の要求で原文を1つだけ送る**（05 の 3.7）。1日 15,000 文字なので、要求の数（1日 数百回）は制限（v2 は1分 300,000 回）より十分少ない。
- 機械翻訳の固有名詞の品質は、原文の併記で補う（U-13 の決定。F-11 ②）。
- Google は Cloud Translation の機能・料金を変えることがある。変わったらこの ADR を見直す。

### 確認したこと（確認日: 2026-11-19）

| 項目 | 内容 | 公式の出典 |
|---|---|---|
| 無料の範囲・料金 | 最初の 50 万文字/月は無料（月 10 USD のクレジットとして適用）。超えた分は 100 万文字あたり 20 USD。クレジットは Basic と Advanced の合計に適用、最大 10 USD、繰り越さない、書式付きの文書の翻訳には使えない | https://cloud.google.com/translate/pricing （本文の注の「Credit usage applies collectively to both Cloud Translation - Basic and Cloud Translation - Advanced. The credit is up to $10 ... does not roll over」は取得した本文で確認。料金表の「500,000 characters」「$20 per million characters」は表が取得した本文に含まれなかったため、同じ公式ページの検索結果の要約で確認した） |
| 数え方 | コードポイントで数える。空白を含む。空の要求も 1 文字 | 同上（Charged characters） |
| 課金の有効化 | 「You must enable billing to use Cloud Translation.」 | https://docs.cloud.google.com/translate/docs/setup |
| 認証 | Basic（v2）のすべてのメソッドは API キーを使える。Advanced（v3）は API キーを使えない | https://docs.cloud.google.com/translate/docs/authentication |
| キーの送り方 | ヘッダー `x-goog-api-key` を使う。使えないときだけクエリ `key`（URL に出て漏れやすい） | https://docs.cloud.google.com/docs/authentication/api-keys-use |
| キーの制限 | API の制限（使える API を限る）、アプリケーションの制限（IP アドレス。サーバー用） | https://docs.cloud.google.com/docs/authentication/api-keys |
| 要求の形 | `POST https://translation.googleapis.com/language/translate/v2`、`q`（文字列の配列）、`target`、`source`、`format`（既定は HTML。`text` で平文）、`model`（既定 `nmt`）。応答は `data.translations[].translatedText`。`q` は最大 128 個。応答の順番の明記はない | https://docs.cloud.google.com/translate/docs/reference/rest/v2/translate |
| 1回の要求の上限 | 推奨は1回 5,000 文字まで。Basic は1回 100K バイトまで。超えると `400 INVALID_ARGUMENT` | https://docs.cloud.google.com/translate/quotas |
| 速さの上限 | 1分あたり 6,000,000 文字（プロジェクト・利用者ごと、一般のモデル）。1分あたりの要求 300,000 回（v2） | 同上 |
| 1日の割り当て | 既定は無制限。下げて使用量と費用を抑えられる。1日の割り当ては太平洋時間の 0 時に戻る | 同上、https://docs.cloud.google.com/apis/docs/capping-api-usage |
| 割り当てを超えたとき | 403。1日の割り当ては `Daily Limit Exceeded`、1分の割り当ては `User Rate Limit Exceeded` | https://docs.cloud.google.com/translate/quotas 、https://docs.cloud.google.com/translate/troubleshooting |
| 429・再試行 | Cloud Translation の文書に 429 と再試行の方法の記載はない（**公式に記載なし**）。安全側に、429 は待って再試行、5xx は間をあけて再試行する（05 の 3.7） | — |
| 失敗した要求の課金 | Basic の失敗した要求が課金されるかは**公式に記載なし**（Advanced の一括翻訳は「成功した翻訳だけ課金」）。安全側に、アプリは成功した分だけ数え、上限を無料の範囲の 90%（月）・93%（Google の日 × 31）にして差を吸収する | https://cloud.google.com/translate/pricing |
| 対応言語 | 韓国語 `ko`・日本語 `ja` は NMT の対応言語 | https://docs.cloud.google.com/translate/docs/languages |
| 送った文の扱い | 「Google does not use the content you send to train and improve our Google Translation features.」翻訳のために短い間メモリに置く | https://docs.cloud.google.com/translate/data-usage |
| 規約 | Service Specific Terms の 25. Cloud Translation API: HTML のマークアップの要件と帰属の要件に従う。翻訳結果の保存・表示を禁じる条項は見つからなかった（**保存についての記載はない**） | https://cloud.google.com/terms/service-terms （最終更新 2026-04-02） |
| 帰属の要件 | 翻訳結果を利用者に表示するときは自動翻訳であることを示す。「powered by Google Translate」の画像を翻訳結果の近くに常に表示する。ウェブページの帰属の画像は `http://translate.google.com` へリンクする。翻訳結果をそのまま表示するときは免責の文（「THIS SERVICE MAY CONTAIN TRANSLATIONS POWERED BY GOOGLE. ...」）をサイトかアプリに置く。画像の素材: https://docs.cloud.google.com/static/translate/images/google-translate-attribution.zip 。文字だけの帰属でよいかは**公式に記載なし**（安全側に画像を使う） | https://docs.cloud.google.com/translate/attribution |
| マークアップの要件 | 翻訳結果をそのまま公開し検索できるときは、機械翻訳と示す: 翻訳した文を `lang="{翻訳した言語}-x-mtfrom-{元の言語}"` の要素で囲む（例: `<span lang="fr-x-mtfrom-en">`） | https://docs.cloud.google.com/translate/markup |
| 予算 | 通知だけの予算は使用量・支出を止めない。既定では割引・クレジットを引いた額で数える | https://docs.cloud.google.com/billing/docs/how-to/budgets |
| 支出の上限の予算 | 対象は Gemini API・Agent Platform・Cloud Run・Cloud Run functions だけ（Cloud Translation は対象外） | https://docs.cloud.google.com/billing/docs/how-to/budgets-spend-caps |
| 請求の月 | Google は請求の期間を太平洋時間で数える | https://docs.cloud.google.com/billing/docs/how-to/reports |

検討した選択肢の確認（確認日: 2026-11-19）:

- DeepL（API Free は購入できない、Developer は累計 100 万文字で戻らない）: https://support.deepl.com/hc/en-us/articles/360021200939-DeepL-API-Free （記事の題名「DeepL API plans」、更新 2026-11-18。本文は Zendesk の公開 API で取得）、https://support.deepl.com/hc/en-us/articles/360020685720-Usage-count-and-billing-in-DeepL-API （更新 2026-11-12）
- DeepL の規約（§3.3.2・§3.3.3・§8.3.3・§8.3.11）: https://www.deepl.com/en/pro-license
- Amazon Translate（月 200 万文字を最初の翻訳の要求から 12 か月、以降 100 万文字あたり 15 USD）: https://aws.amazon.com/translate/pricing/
- Papago（文字数で課金。無料の範囲の記載は確認できなかった）: https://guide.ncloud-docs.com/docs/papagotranslation-overview
- LibreTranslate（AGPL v3、Argos Translate）: https://github.com/LibreTranslate/LibreTranslate
- Azure は 0007 の確認（2026-11-05）による。

---

## 한국어

### 0018. 외부 데이터 번역은 Google Cloud Translation(Basic, v2)을 쓴다(0007을 대체)

- 날짜: 2026-11-19
- 상태: 채택(2026-11-19, 사람이 결정. Issue #39)
- 관련 요건: F-02 ⑥, F-11, N-01, N-07 (U-14, U-13, R-58)
- 대체하는 ADR: 0007(Azure AI Translator F0)

### 배경

- 0007은 Azure AI Translator 무료 등급(F0)을 골랐지만, 사람은 Microsoft 계정을 만들지 않기로 했다(2026-11-19).
- 다음으로 DeepL API Free를 골랐지만(2026-11-19), architect의 조사로 DeepL 공식(지원 문서, 갱신 2026-11-18)에 "The DeepL API Free plan can no longer be purchased."라고 되어 있고, 지금의 무료 플랜(Developer)은 **누적 100만 자이며 매달 돌아오지 않는다**는 것을 알았다. 0007의 추정(첫 달 약 92만 자, 둘째 달 이후 약 45만 자/월)으로는 운영 수집에서 약 5주 만에 다 쓴다. 약관에도 무료 API에서 개인 데이터 처리 금지(§8.3.11)가 있다(아래 "검토한 선택지").
- PM이 다시 비교하고(Issue #39 코멘트), 사람이 **Google Cloud Translation**으로 정했다(2026-11-19, 2번째). 이유: 무료 범위(월 50만 자)가 매달 돌아오므로 일본어 표시가 이어진다. 지도(0006)와 같은 Google Cloud 결제 계정을 쓸 수 있어 새 계정이 필요 없다. DeepL 약관의 제한이 없다.
- 0007의 배경(수집할 때 번역해서 저장해야 함, 매일 수집, 월 0원과 T-01)은 바뀌지 않는다.

### 결정

번호는 0007의 결정과 같은 번호로 맞췄다(아래 "0007과의 대응").

1. 번역 API는 **Google Cloud Translation - Basic(v2)**의 `translate`(모델은 기본 NMT)를 쓴다(한국어 `ko` → 일본어 `ja`). Advanced(v3)는 쓰지 않는다(v3는 API 키를 쓸 수 없고, 서비스 계정 인증을 위해 새 라이브러리나 서명을 직접 만들어야 하므로. 아래 "확인한 것").
2. **수집할 때 번역해서 저장한다**(B-01의 S7. 05의 3.7). 화면 표시 때마다 API를 부르지 않는다(0007 결정 2를 이어받음).
3. **번역량을 줄인다**(0007 결정 3을 그대로 이어받음).
   - **같은 원문은 두 번 번역하지 않는다**(원문 해시별로 번역 결과를 저장해 다시 쓴다. 04의 DB-05).
   - 공연 항목이 바뀌면 **바뀐 항목만** 다시 번역한다.
   - 장르·공연 상태처럼 값의 종류가 정해진 것은 API로 번역하지 않고 화면 문구와 같은 방법(다국어 사전)으로 옮긴다.
   - 역명은 서울교통공사의 공식 일본어 표기를 쓰고 API로 번역하지 않는다(0009).
4. **사용량 상한(T-01)**. Google 쪽과 앱 쪽 이중으로 멈춘다.
   - **Google 쪽**: Google Cloud 콘솔의 "할당량(Quotas)"에서 Cloud Translation API의 **하루 글자 수를 15,000자**로 낮춘다(무료 범위 50만 자 ÷ 31일 ≒ 16,129자. 0006 결정 4와 같이 31로 나누고 여유를 두어 적게 한다. 31일 × 15,000 = 46.5만 자로 무료 범위의 93%). 넘으면 403(`Daily Limit Exceeded`)으로 멈추고 그날 분은 과금되지 않는다. 하루는 태평양 시간 0시에 돌아온다(공식).
   - **앱 쪽(1회 실행)**: S7의 1회 실행에서 보내는 글자 수를 **15,000자**까지로 한다(B-01은 하루 1번. 05의 3.7). 보통은 앱이 먼저 멈춰 Google 할당량에 닿지 않는다.
   - **앱 쪽(월)**: 한 달 번역 글자 수를 세어 **월 45만 자**(무료 범위 50만 자의 90%)에 이르면 그달은 번역을 멈춘다. 월은 Asia/Seoul 달력의 월(P-09 그대로. 이유는 "주의점").
   - 멈춘 뒤 번역하지 못한 글은 다음 수집 때 다시 번역한다(그동안은 한국어 원문을 보여준다. 02의 3.4의 P-06. F-11 ①·R-58).
5. **예산 알림(T-01)**: 0006 결정 5의 예산(결제 계정 전체, 1 USD 상당, 1%·50%·100%에서 알림)을 그대로 쓴다. 번역 과금도 이 예산에 포함된다. 예산의 "할인·크레딧 포함"은 기본값(포함)으로 둔다(무료 범위는 월 10 USD 크레딧으로 빠지므로, 포함하지 않으면 무료 범위 안에서도 알림이 온다). 예산은 사용량을 멈추지 않으므로(공식) 멈추는 것은 4.에서 한다.
6. **키와 Google Cloud 프로젝트**
   - 번역 **전용 Google Cloud 프로젝트**를 만들고 지도(0006)와 같은 결제 계정에 연결한다(결제를 켠다. Cloud Translation은 결제 활성화가 필수(공식)). 0006 결정 7(지도 프로젝트는 지도 전용)과 0011(reCAPTCHA는 결제 없는 별도 프로젝트)을 바꾸지 않기 위해, 지도·reCAPTCHA 프로젝트에서는 Cloud Translation API를 켜지 않는다.
   - 인증은 **API 키**(v2는 API 키를 쓸 수 있다(공식)). 키에 다음 제한을 건다: **API 제한**을 Cloud Translation API만으로 한다. **애플리케이션 제한**을 IP 주소로 하고, 운영 서버의 예약된 공인 IP(0010)만 허용한다.
   - 키는 서버 환경변수(`.env`의 `GOOGLE_TRANSLATE_API_KEY`)에만 둔다. 브라우저에서 번역 API를 부르지 않는다. 요청에서는 키를 HTTP 헤더 `x-goog-api-key`로 보낸다(쿼리 `key`는 URL에 드러나 새기 쉽다고 공식이 쓰고 있으므로 쓰지 않는다).
7. 호출은 REST API를 Spring `RestClient`로 한다. Google SDK(클라이언트 라이브러리)는 넣지 않는다(0007 결정 7을 이어받음. 새 라이브러리를 늘리지 않는다).
8. **출처(귀속) 표시와 HTML 마크업(Google Cloud Service Specific Terms의 25.)**: 기계 번역을 표시할 때는 Google의 귀속 요건("powered by Google Translate" 이미지를 번역 가까이에 둔다, `translate.google.com`으로의 링크, 면책 문구를 사이트에 둔다)과 HTML 마크업 요건(기계 번역 글의 요소에 `lang="ja-x-mtfrom-ko"`)을 지킨다. 화면 어디에 무엇을 내는지는 02의 3.4(P-19), 기계 번역인지를 화면이 구별하는 항목은 03의 7.2(`LocalizedText.machineTranslated`), 겉모습은 07에서 정한다.

#### 0007과의 대응(다른 문서가 0007의 번호로 참조하는 곳의 바꿔 읽기)

| 0007 | 0018 | 바뀌었는가 |
|---|---|---|
| 결정 1(Azure F0) | 결정 1(Google Cloud Translation Basic v2) | 대체 |
| 결정 2(수집할 때 번역해 저장) | 결정 2 | 같음 |
| 결정 3·"3."(같은 원문은 한 번만, 바뀐 항목만, 정해진 값은 화면 사전, 역명은 공식 표기) | 결정 3 | 같음(그대로 이어받음) |
| 결정 4(월 180만 자, `403001`) | 결정 4(하루 15,000·1회 15,000·월 45만 자, 403) | 값과 멈추는 방법을 대체 |
| 결정 5(Azure 예산) | 결정 5(0006의 예산을 씀) | 대체 |
| 결정 6(키는 서버 `.env`에만) | 결정 6 | 방침은 같음. 키 제한·프로젝트를 추가 |
| 결정 7(SDK를 넣지 않고 HTTP 클라이언트로 호출) | 결정 7 | 같음 |
| 번역 글자 수 추정 | 아래 "추정과 상한 비교" | 추정 값은 그대로 이어받음 |
| — | 결정 8(귀속 표시·마크업) | 추가 |

- 01·04 등이 "ADR 0007 3." "ADR 0007 결정 3"이라고 쓴 내용(같은 원문은 두 번 번역하지 않음, 정해진 값은 화면 사전)은 이 ADR의 결정 3과 같다. 참조를 고쳐 쓰기 전까지는 0007의 결정 3을 0018의 결정 3으로 바꿔 읽는다.

### 추정과 상한 비교

추정은 0007의 "번역 글자 수 추정"을 그대로 쓴다(가정도 같음. 구현 첫 수집에서 실측해 이 ADR을 다시 본다).

| 시기 | 0007의 추정 | 이 ADR의 상한으로 번역할 수 있는 양 | 결과 |
|---|---|---|---|
| 첫 달 | 약 92만 자(첫 수집 약 47만 + 매일 약 1.5만 × 30) | 1회 15,000 × 하루 1번 → 월 약 45만 자(월 상한 45만 자) | 약 47만 자가 다음 달 이후로 남는다. 그동안 남은 글은 한국어 원문으로 보여준다(사람이 받아들임. Issue #39) |
| 둘째 달 이후 | 약 45만 자/월(매일 약 1.5만) | 월 약 45만 자 | 추정과 거의 같아 여유가 없다. 첫 달에 남은 분은 거의 줄지 않는다 |

- 번역 순서는 공연명 → 공연장명 → 역명 → 그 밖(05의 3.7)이므로, 상한에 닿아도 일본어 키워드 검색(F-02 ⑥)과 원문을 함께 적는 항목(F-11 ②)이 먼저 번역된다.
- 실측이 추정보다 많아 남는 양이 계속 늘면 이 ADR을 다시 본다(예: 번역할 항목을 줄이는 요건 변경, 유료 범위를 허용할지). 사람이 정한다.

#### 상한을 넘을 것 같을 때의 동작

| 상황 | 동작 | 과금 |
|---|---|---|
| S7의 1회에서 15,000자에 이름 | 그 회의 S7을 멈춘다(결과는 성공, 기록에 "1회 상한"). 다음 날 실행에서 이어서 번역한다 | 없음 |
| 이번 달(Asia/Seoul) 합계가 45만 자에 이름 | 그달의 S7을 멈춘다(결과는 성공, 기록에 "월 상한"). 다음 달 첫 실행에서 이어서 번역한다 | 없음 |
| 앱의 계산 착오·수동 재실행으로 Google 하루 할당량(15,000자)을 넘음 | Google이 403을 돌려준다. 그 회의 S7을 멈춘다(기록은 실패). 태평양 시간 0시에 돌아온다 | 없음(할당량을 넘은 요청은 번역되지 않음) |
| 할당량 설정을 잊었거나 해제함 | 앱 쪽 상한으로만 멈춘다. 무료 범위를 넘은 분은 100만 자당 20 USD. 예산 알림(결정 5)으로 알아챈다 | 있음(막기 위해 운영 절차의 확인 항목으로 한다) |

### 검토한 선택지

| 선택지 | 무료 범위 | 상한을 넘었을 때 | 카드 | 판단 |
|---|---|---|---|---|
| **Google Cloud Translation(Basic v2)** | 월 50만 자(월 10 USD 크레딧. Basic과 Advanced 합계. 이월 안 됨) | 넘은 분은 100만 자당 20 USD. 할당량(하루 글자 수)을 낮춰 멈춘다 | 필요(결제 활성화 필수. 지도와 같은 결제 계정) | **채택**(사람의 결정). 매달 돌아온다. 새 계정이 필요 없다 |
| DeepL API Developer(무료) | **누적 100만 자. 돌아오지 않음**(공식. DeepL API Free는 새로 신청할 수 없음) | 다 쓰면 유료 Growth로 옮겨야 함 | 공식에 기재 없음 | 운영 수집에서 약 5주 만에 다 쓴다. 약관: §8.3.11(무료 API로 개인 데이터를 처리하지 않음. 출연자명은 실존 인물의 이름), §8.3.3(표시하는 번역에 DeepL 로고나 이름을 보임), §3.3.2(보낸 글과 번역을 DeepL이 무기한 저장할 수 있음), §3.3.3(무료 API 기능을 예고 없이 바꾸거나 없앨 수 있음) |
| DeepL API Growth | 없음(유료. 월정액과 사용량 과금) | 사용량으로 과금 | 필요 | 월 0원(U-12, N-07)에 맞지 않는다. 요금 금액은 공식으로 확인하지 않았다 |
| Azure AI Translator F0(0007) | 월 200만 자 | `403001`로 멈추고 과금 없음 | 필요(Azure 계정) | Microsoft 계정을 만들지 않는다(사람의 결정) |
| Amazon Translate | 월 200만 자. **첫 번역 요청부터 12개월만** | 100만 자당 15 USD | 필요(AWS 계정) | 1년 뒤부터 유료. 새 클라우드 계정이 늘어난다 |
| Papago(NAVER Cloud) | 공식에서 무료 범위 기재를 확인하지 못했다(PM 비교에서는 무료 범위 없음) | 글자 수로 과금(공식) | 필요 | 월 0원에 맞지 않는다. 새 계정이 늘어난다 |
| LibreTranslate(자체 서버에서 운영) | 제한 없음(OSS, AGPL v3. 번역은 Argos Translate) | — | 불필요 | 한국어·일본어 지원과 필요한 메모리가 공식 README에 기재되어 있지 않다. 번역 품질(한국어→일본어) 확인과 0010 서버 메모리 재검토가 필요하다 |
| 번역하지 않음 | — | — | — | F-11 ①(모두 번역)을 충족하지 못한다 |

### 결과(좋은 점·주의점)

#### 비용(월 0원 범위인가)

| 항목 | 내용 |
|---|---|
| 무료 범위 | 월 50만 자(언어 감지와 NMT 번역. 월 10 USD 크레딧으로 빠지고 이월되지 않는다. Basic과 Advanced 합계) |
| 넘었을 때 요금 | 100만 자당 20 USD |
| 세는 법 | Unicode 코드 포인트 수. 공백도 센다. 빈 요청도 1자로 과금된다(공식). 앱도 같이 코드 포인트로 세고 빈 글은 보내지 않는다 |
| 카드 등록 | 필요(결제 계정. 지도와 같은 것) |
| 멈추는 방법 | Google 하루 할당량 15,000자(결정 4) + 앱의 1회 15,000·월 45만 자. **지출 상한(spend cap) 예산은 Cloud Translation에 쓸 수 없다**(대상은 Gemini API·Agent Platform·Cloud Run·Cloud Run functions뿐. 공식) |
| 예산 알림 | 0006의 예산(결제 계정 전체, 1 USD 상당). 사용량은 멈추지 않는다 |

#### 좋은 점

- 무료 범위가 매달 돌아오므로 상한에서 멈춰도 다음 달에 번역이 이어진다.
- 지도와 같은 결제 계정·예산으로 관리할 수 있다. 새 클라우드 계정이 필요 없다.
- 보낸 글은 Google 번역의 학습·개선에 쓰이지 않는다(공식).
- 할당량으로 하루 양을 강제로 멈출 수 있으므로, 앱에 오류가 있어도 한 달 46.5만 자(무료 범위의 93%)를 넘지 않는다.

#### 주의점

- **여유가 없다**: 둘째 달 이후 추정(약 45만 자/월)이 상한과 거의 같다. 첫 달에 남은 약 47만 자는 거의 줄지 않는다. 공연명부터 번역하므로 남는 것은 주로 출연자·티켓 가격 같은 긴 글이 된다. 실측으로 다시 본다.
- **월 경계의 어긋남(P-09)**: Google 청구의 월은 태평양 시간으로 나뉜다(공식). 앱의 월(Asia/Seoul)과 16~17시간 어긋난다. B-01은 06:00(Asia/Seoul)에 하루 1번 돌고 1회 상한이 15,000자이므로, 어긋남으로 Google의 한 달에 들어가는 것은 최대 1회분(15,000자) 많을 뿐이다(45만 + 1.5만 = 46.5만 자 < 50만 자). Google 하루 할당량도 태평양 시간의 날로 세므로 Google 월 합계는 31일 × 15,000 = 46.5만 자를 넘지 않는다. 그래서 P-09(Asia/Seoul 달력의 월로 셈)는 바꾸지 않는다(이유만 02·05에서 고쳐 썼다).
- **할당량 반영 지연**: 할당량 적용에는 지연이 있다(공식 "Quota enforcement has a latency period"). 그래서 하루 할당량은 16,129가 아니라 15,000으로 했다.
- **귀속 표시**가 필요하다(결정 8). 화면에 "powered by Google Translate" 이미지·링크·면책 문구를 둘 곳이 늘어난다(02·07).
- 무료 범위 크레딧이 결제 계정별인지 프로젝트별인지는 공식에 기재가 없다. 안전을 위해 같은 결제 계정의 다른 프로젝트에서 Cloud Translation을 쓰지 않는다.
- 요청 1회의 보내는 법: v2 응답이 요청과 같은 순서로 나온다는 것은 v2 레퍼런스에 명시되어 있지 않다(v3 문서에는 명시됨). 그래서 **요청 1회에 원문을 하나만 보낸다**(05의 3.7). 하루 15,000자이므로 요청 수(하루 수백 번)는 제한(v2는 분당 300,000번)보다 충분히 적다.
- 기계 번역의 고유명사 품질은 원문 병기로 보완한다(U-13 결정. F-11 ②).
- Google은 Cloud Translation의 기능·요금을 바꿀 수 있다. 바뀌면 이 ADR을 다시 본다.

#### 확인한 것(확인일: 2026-11-19)

| 항목 | 내용 | 공식 출처 |
|---|---|---|
| 무료 범위·요금 | 처음 50만 자/월은 무료(월 10 USD 크레딧으로 적용). 넘은 분은 100만 자당 20 USD. 크레딧은 Basic과 Advanced 합계에 적용, 최대 10 USD, 이월 안 됨, 서식 있는 문서 번역에는 쓸 수 없음 | https://cloud.google.com/translate/pricing (본문 주석 "Credit usage applies collectively to both Cloud Translation - Basic and Cloud Translation - Advanced. The credit is up to $10 ... does not roll over"는 가져온 본문으로 확인. 요금표의 "500,000 characters" "$20 per million characters"는 표가 가져온 본문에 포함되지 않아 같은 공식 페이지의 검색 결과 요약으로 확인했다) |
| 세는 법 | 코드 포인트로 센다. 공백 포함. 빈 요청도 1자 | 위와 같음(Charged characters) |
| 결제 활성화 | "You must enable billing to use Cloud Translation." | https://docs.cloud.google.com/translate/docs/setup |
| 인증 | Basic(v2)의 모든 메서드는 API 키를 쓸 수 있다. Advanced(v3)는 API 키를 쓸 수 없다 | https://docs.cloud.google.com/translate/docs/authentication |
| 키 보내는 법 | 헤더 `x-goog-api-key`를 쓴다. 쓸 수 없을 때만 쿼리 `key`(URL에 드러나 새기 쉬움) | https://docs.cloud.google.com/docs/authentication/api-keys-use |
| 키 제한 | API 제한(쓸 수 있는 API를 한정), 애플리케이션 제한(IP 주소. 서버용) | https://docs.cloud.google.com/docs/authentication/api-keys |
| 요청 형태 | `POST https://translation.googleapis.com/language/translate/v2`, `q`(문자열 배열), `target`, `source`, `format`(기본 HTML. `text`면 평문), `model`(기본 `nmt`). 응답은 `data.translations[].translatedText`. `q`는 최대 128개. 응답 순서는 명시되어 있지 않음 | https://docs.cloud.google.com/translate/docs/reference/rest/v2/translate |
| 요청 1회 상한 | 권장은 1회 5,000자까지. Basic은 1회 100K 바이트까지. 넘으면 `400 INVALID_ARGUMENT` | https://docs.cloud.google.com/translate/quotas |
| 속도 상한 | 분당 6,000,000자(프로젝트·사용자별, 일반 모델). 분당 요청 300,000번(v2) | 위와 같음 |
| 하루 할당량 | 기본은 무제한. 낮춰서 사용량과 비용을 억제할 수 있다. 하루 할당량은 태평양 시간 0시에 돌아온다 | 위와 같음, https://docs.cloud.google.com/apis/docs/capping-api-usage |
| 할당량을 넘었을 때 | 403. 하루 할당량은 `Daily Limit Exceeded`, 분 할당량은 `User Rate Limit Exceeded` | https://docs.cloud.google.com/translate/quotas , https://docs.cloud.google.com/translate/troubleshooting |
| 429·재시도 | Cloud Translation 문서에 429와 재시도 방법의 기재는 없다(**공식에 기재 없음**). 안전 쪽으로 429는 기다려 재시도, 5xx는 간격을 두고 재시도한다(05의 3.7) | — |
| 실패한 요청의 과금 | Basic의 실패한 요청이 과금되는지는 **공식에 기재 없음**(Advanced 일괄 번역은 "성공한 번역만 과금"). 안전 쪽으로 앱은 성공한 분만 세고, 상한을 무료 범위의 90%(월)·93%(Google 하루 × 31)로 해 차이를 흡수한다 | https://cloud.google.com/translate/pricing |
| 지원 언어 | 한국어 `ko`·일본어 `ja`는 NMT 지원 언어 | https://docs.cloud.google.com/translate/docs/languages |
| 보낸 글의 취급 | "Google does not use the content you send to train and improve our Google Translation features." 번역을 위해 잠시 메모리에 둔다 | https://docs.cloud.google.com/translate/data-usage |
| 약관 | Service Specific Terms의 25. Cloud Translation API: HTML 마크업 요건과 귀속 요건을 따른다. 번역 결과의 저장·표시를 금지하는 조항은 찾지 못했다(**저장에 관한 기재는 없음**) | https://cloud.google.com/terms/service-terms (최종 갱신 2026-04-02) |
| 귀속 요건 | 번역 결과를 이용자에게 표시할 때는 자동 번역임을 보인다. "powered by Google Translate" 이미지를 번역 결과 가까이에 항상 표시한다. 웹 페이지의 귀속 이미지는 `http://translate.google.com`으로 링크한다. 번역 결과를 그대로 표시할 때는 면책 문구("THIS SERVICE MAY CONTAIN TRANSLATIONS POWERED BY GOOGLE. ...")를 사이트나 앱에 둔다. 이미지 소재: https://docs.cloud.google.com/static/translate/images/google-translate-attribution.zip . 글자만의 귀속으로 되는지는 **공식에 기재 없음**(안전 쪽으로 이미지를 쓴다) | https://docs.cloud.google.com/translate/attribution |
| 마크업 요건 | 번역 결과를 그대로 공개하고 검색될 수 있을 때는 기계 번역임을 보인다: 번역한 글을 `lang="{번역한 언어}-x-mtfrom-{원래 언어}"` 요소로 감싼다(예: `<span lang="fr-x-mtfrom-en">`) | https://docs.cloud.google.com/translate/markup |
| 예산 | 알림만 하는 예산은 사용량·지출을 멈추지 않는다. 기본으로는 할인·크레딧을 뺀 금액으로 센다 | https://docs.cloud.google.com/billing/docs/how-to/budgets |
| 지출 상한 예산 | 대상은 Gemini API·Agent Platform·Cloud Run·Cloud Run functions뿐(Cloud Translation은 대상 외) | https://docs.cloud.google.com/billing/docs/how-to/budgets-spend-caps |
| 청구의 월 | Google은 청구 기간을 태평양 시간으로 센다 | https://docs.cloud.google.com/billing/docs/how-to/reports |

검토한 선택지의 확인(확인일: 2026-11-19):

- DeepL(API Free는 구매 불가, Developer는 누적 100만 자로 돌아오지 않음): https://support.deepl.com/hc/en-us/articles/360021200939-DeepL-API-Free (문서 제목 "DeepL API plans", 갱신 2026-11-18. 본문은 Zendesk 공개 API로 가져옴), https://support.deepl.com/hc/en-us/articles/360020685720-Usage-count-and-billing-in-DeepL-API (갱신 2026-11-12)
- DeepL 약관(§3.3.2·§3.3.3·§8.3.3·§8.3.11): https://www.deepl.com/en/pro-license
- Amazon Translate(월 200만 자를 첫 번역 요청부터 12개월, 이후 100만 자당 15 USD): https://aws.amazon.com/translate/pricing/
- Papago(글자 수로 과금. 무료 범위 기재는 확인하지 못함): https://guide.ncloud-docs.com/docs/papagotranslation-overview
- LibreTranslate(AGPL v3, Argos Translate): https://github.com/LibreTranslate/LibreTranslate
- Azure는 0007의 확인(2026-11-05)에 따른다.
