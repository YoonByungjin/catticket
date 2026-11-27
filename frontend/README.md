# CatTicket フロントエンド

Angular 22 + Angular Material（ADR 0014）。仕様は `docs/design/` の設計書（02・03・07）。

## コマンド

```bash
cd frontend
npm ci                       # 依存関係を package-lock.json のとおりに入れる
npm start                    # 開発サーバー（http://localhost:4200）。/api は http://127.0.0.1:8080 へ転送（proxy.conf.json）
npm test -- --watch=false    # 単体テスト（Vitest + jsdom。ヘッドレス。CI と同じ）
npm run build                # 本番ビルド（dist/catticket/。ライセンスの一覧は dist/catticket/3rdpartylicenses.txt）
```

Node.js は LTS（Angular 22 の対応: `^22.22.3 || ^24.15.0 || >=26.0.0`）。

## 構成

| 場所 | 内容 |
|---|---|
| `src/index.html` | 最初の描画の前に表示言語（F-10）とライト・ダーク表示（F-36）を決めるインラインスクリプト |
| `src/styles.scss` | Material のテーマ（`mat.theme`、07 の 2.1・2.2）。部品の CSS は直接上書きしない |
| `src/theme/_theme-colors.scss` | `ng generate @angular/material:theme-color` で作ったパレット（手で変えない） |
| `src/app/core/i18n/` | 画面の文言の辞書（`messages.ko.ts` が正、`messages.ja.ts` は同じキーを型で強制）、`LanguageService`、`translate` パイプ |
| `src/app/core/theme/` | `ThemeService`（ライト・ダーク表示） |
| `src/app/core/notification/` | CM-03 通知（`MatSnackBar`） |
| `src/app/layout/` | CM-01 ヘッダー、CM-02 フッター。出典の2行（`attribution/`）は SC-01 のスマホ表示の出典のダイアログでも使う |
| `src/app/core/api/` | API の共通の型（03 の 7.2・7.3） |
| `src/app/core/code/` | ジャンルのコード（表示の文言は辞書の `genre.{コード}`） |
| `src/app/shared/` | 複数の画面で使う部品: 公演のカード（02 の 3.7）、期間の表示（P-18）、外部データの文の表示（02 の 3.4） |
| `src/app/pages/` | 各画面（`rankings/` は SC-03。API の呼び出しは画面ごとのサービス）、SC-99 エラー、まだ中身のない画面の仮の画面 |
| `src/app/app.routes.ts` | 02 の画面一覧の URL |

画面の文言は辞書に足し、テンプレートに直書きしない: `{{ 'header.login' | translate }}`。

---

## 한국어

# CatTicket 프론트엔드

Angular 22 + Angular Material(ADR 0014). 사양은 `docs/design/`의 설계서(02·03·07).

## 명령어

```bash
cd frontend
npm ci                       # package-lock.json대로 의존성 설치
npm start                    # 개발 서버(http://localhost:4200). /api는 http://127.0.0.1:8080으로 전달(proxy.conf.json)
npm test -- --watch=false    # 단위 테스트(Vitest + jsdom. 헤드리스. CI와 같음)
npm run build                # 운영 빌드(dist/catticket/. 라이선스 목록은 dist/catticket/3rdpartylicenses.txt)
```

Node.js는 LTS(Angular 22 지원: `^22.22.3 || ^24.15.0 || >=26.0.0`).

## 구성

| 위치 | 내용 |
|---|---|
| `src/index.html` | 첫 그리기 전에 표시 언어(F-10)와 라이트·다크 표시(F-36)를 정하는 인라인 스크립트 |
| `src/styles.scss` | Material 테마(`mat.theme`, 07의 2.1·2.2). 부품 CSS는 직접 덮어쓰지 않는다 |
| `src/theme/_theme-colors.scss` | `ng generate @angular/material:theme-color`로 만든 팔레트(손으로 바꾸지 않는다) |
| `src/app/core/i18n/` | 화면 문구 사전(`messages.ko.ts`가 기준, `messages.ja.ts`는 같은 키를 타입으로 강제), `LanguageService`, `translate` 파이프 |
| `src/app/core/theme/` | `ThemeService`(라이트·다크 표시) |
| `src/app/core/notification/` | CM-03 알림(`MatSnackBar`) |
| `src/app/layout/` | CM-01 헤더, CM-02 푸터. 출처 2줄(`attribution/`)은 SC-01 스마트폰 표시의 출처 다이얼로그에서도 쓴다 |
| `src/app/core/api/` | API 공통 타입(03의 7.2·7.3) |
| `src/app/core/code/` | 장르 코드(표시 문구는 사전의 `genre.{코드}`) |
| `src/app/shared/` | 여러 화면에서 쓰는 부품: 공연 카드(02의 3.7), 기간 표시(P-18), 외부 데이터 문장 표시(02의 3.4) |
| `src/app/pages/` | 각 화면(`rankings/`는 SC-03. API 호출은 화면별 서비스), SC-99 오류, 아직 내용이 없는 화면의 임시 화면 |
| `src/app/app.routes.ts` | 02 화면 목록의 URL |

화면 문구는 사전에 추가하고 템플릿에 직접 쓰지 않는다: `{{ 'header.login' | translate }}`.
