# KOPIS のテスト用の応答

2026-11-20 に実行 Claude が実際の KOPIS を呼んで保存した応答（Issue #38。API キーは含まない）。06 の 7.1 の「テスト」。

| ファイル | 元 |
|---|---|
| `performance-list-page-100.xml` | `pblprfr`（ソウル 2026-11-20〜12-20）の 1 ページ目（100 件） |
| `performance-list-last-page.xml` | 同じ照会の最後のページ（73 件） |
| `performance-list-empty.xml` | 最後のページを超えたページ（`<dbs/>`） |
| `performance-detail-relates.xml` | `pblprfr/PF303043`（`relates` が 3 つ） |
| `performance-detail-blank-items.xml` | `pblprfr/PF304568`（`prfcast`・`entrpsnm` が空白1文字、`updatedate` に小数の秒） |
| `performance-detail-no-relates.xml` | 上のファイルから `relates` を除いたもの（**加工**。`relates` のない実物がなかったため） |
| `performance-detail-04.xml` | 存在しない ID の公演詳細（結果コード 04） |
| `venue-detail.xml` / `venue-detail-04.xml` | `prfplc/FC004949` / 存在しない ID |
| `boxoffice-50.xml` | `boxoffice` 日次 2026-11-19・ソウル・全体（50 行） |
| `boxoffice-1.xml` | 上のファイルの先頭の 1 行だけ（**加工**。件数を減らした） |
| `boxoffice-empty.xml` | 当日の `boxoffice`（`<boxofs/>`） |
| `awards.xml` | `prfawad`（5 件） |
| `genre-stats.xml` | `boxStatsCate` 2026-10-21〜11-19（9 ジャンル + `합계`。대중무용 の `totnmrssm` が負） |
| `error-01.xml` / `error-05.xml` / `error-06.xml` | 実際の結果コード 01・05・06 |
| `error-02.xml` / `error-03.xml` / `error-99.xml` | 01 の形の `returncode`・`errmsg` だけを変えたもの（**加工**。02 は確認済みの文言、03 は起こせなかった、99 は知らないコードの例） |
