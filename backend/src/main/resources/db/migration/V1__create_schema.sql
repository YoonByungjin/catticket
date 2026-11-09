-- CatTicket のスキーマ（docs/design/04-tables.md の 11. のとおり。Issue #20）
-- 照合順序はテーブルごとに明示し、サーバーの既定（utf8mb4_unicode_ci）に頼らない（04 の 11.5 の D-03）。
-- 外部キーの参照の順（venue → performance → …）で作る。

-- ---------------------------------------------------------------------------
-- 会場・公演
-- ---------------------------------------------------------------------------

CREATE TABLE venue (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  kopis_venue_id VARCHAR(20) COLLATE utf8mb4_bin NOT NULL COMMENT 'KOPIS の施設 ID（mt10id）',
  name VARCHAR(200) NOT NULL COMMENT '会場名（原文）',
  address VARCHAR(500) NULL DEFAULT NULL COMMENT '住所（原文）',
  district VARCHAR(10) NULL DEFAULT NULL COMMENT '区の名前（原文。例: 종로구）。決められなければ NULL',
  latitude DECIMAL(9,6) NULL DEFAULT NULL COMMENT '緯度',
  longitude DECIMAL(9,6) NULL DEFAULT NULL COMMENT '経度',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  PRIMARY KEY (id),
  UNIQUE KEY uk_venue_kopis_venue_id (kopis_venue_id),
  KEY idx_venue_district (district),
  KEY idx_venue_location (latitude, longitude),
  -- 緯度・経度は両方あるか両方ないか（座標のない会場も保存する。P-11）
  CONSTRAINT chk_venue_location_pair CHECK ((latitude IS NULL) = (longitude IS NULL)),
  CONSTRAINT chk_venue_latitude_range CHECK (latitude BETWEEN -90 AND 90),
  CONSTRAINT chk_venue_longitude_range CHECK (longitude BETWEEN -180 AND 180)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci COMMENT='会場（KOPIS の施設）';

CREATE TABLE performance (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  kopis_performance_id VARCHAR(20) COLLATE utf8mb4_bin NOT NULL COMMENT 'KOPIS の公演 ID（mt20id）',
  venue_id BIGINT NOT NULL COMMENT '会場',
  title VARCHAR(500) NOT NULL COMMENT '公演名（原文）',
  start_date DATE NOT NULL COMMENT '公演開始日',
  end_date DATE NOT NULL COMMENT '公演終了日',
  genre_code VARCHAR(10) NOT NULL COMMENT 'ジャンルの KOPIS のコード',
  cast_text TEXT NULL COMMENT '出演者（原文）',
  ticket_price TEXT NULL COMMENT 'チケット価格（原文）',
  schedule_text TEXT NULL COMMENT '公演時間の案内（原文）',
  runtime VARCHAR(200) NULL DEFAULT NULL COMMENT '上演時間（原文）',
  age_limit VARCHAR(200) NULL DEFAULT NULL COMMENT '観覧年齢（原文）',
  poster_url VARCHAR(2000) NULL DEFAULT NULL COMMENT 'ポスターの URL',
  is_open_run BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'オープンランか',
  total_show_count INT NULL DEFAULT NULL COMMENT '開幕からの累計の上演回数',
  has_award BOOLEAN NOT NULL DEFAULT FALSE COMMENT '受賞歴があるか',
  is_seoul BOOLEAN NOT NULL COMMENT 'ソウルの公演か',
  is_hidden BOOLEAN NOT NULL DEFAULT FALSE COMMENT '管理者が非表示にしたか',
  kopis_updated_at DATETIME NULL DEFAULT NULL COMMENT 'KOPIS の最終更新日時',
  collected_at DATETIME NOT NULL COMMENT '最後に収集した日時',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  PRIMARY KEY (id),
  UNIQUE KEY uk_performance_kopis_performance_id (kopis_performance_id),
  KEY idx_performance_venue_id (venue_id),
  CONSTRAINT fk_performance_venue FOREIGN KEY (venue_id) REFERENCES venue (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_performance_total_show_count CHECK (total_show_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci COMMENT='公演（自動収集の値と非表示）';

CREATE TABLE booking_site (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  performance_id BIGINT NOT NULL COMMENT '公演',
  source ENUM('COLLECTED','ADMIN') NOT NULL COMMENT '自動収集の分か管理者の修正の分か',
  name VARCHAR(200) NULL DEFAULT NULL COMMENT '予約サイトの名前（原文）。自動収集で名前がなければ NULL（管理者の行は必須）',
  url VARCHAR(2000) NOT NULL COMMENT '予約サイトの URL',
  sort_order INT NOT NULL COMMENT '並び順（1 から）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  PRIMARY KEY (id),
  UNIQUE KEY uk_booking_site_performance_source_order (performance_id, source, sort_order),
  CONSTRAINT fk_booking_site_performance FOREIGN KEY (performance_id) REFERENCES performance (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_booking_site_sort_order CHECK (sort_order >= 1),
  -- 名前を NULL にできるのは自動収集の行だけ（03 の 5.1 の H-10）
  CONSTRAINT chk_booking_site_admin_name CHECK (source = 'COLLECTED' OR name IS NOT NULL),
  -- 名前なしは NULL で表す（空文字を入れない。05 の 3.3 の 9.）
  CONSTRAINT chk_booking_site_name_not_empty CHECK (name IS NULL OR CHAR_LENGTH(name) >= 1),
  -- URL のない予約サイトは保存しない（05 の 3.3 の 9.）
  CONSTRAINT chk_booking_site_url_not_empty CHECK (CHAR_LENGTH(url) >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci COMMENT='予約サイト';

CREATE TABLE performance_override (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  performance_id BIGINT NOT NULL COMMENT '公演',
  field_name VARCHAR(30) COLLATE utf8mb4_bin NOT NULL COMMENT '修正した項目（performance の列名。booking_sites は予約サイト）',
  -- 照合順序は performance の文の列と同じ（ビューで1つの式にまとめるため）
  override_value TEXT NULL COMMENT '修正した値（文字列。日付は YYYY-MM-DD、真偽は true/false）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修正した日時',
  PRIMARY KEY (id),
  UNIQUE KEY uk_performance_override_performance_field (performance_id, field_name),
  CONSTRAINT fk_performance_override_performance FOREIGN KEY (performance_id) REFERENCES performance (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_performance_override_field_name CHECK (field_name IN (
    'title','start_date','end_date','genre_code','cast_text','ticket_price',
    'schedule_text','runtime','age_limit','is_open_run','booking_sites')),
  -- 予約サイトの修正の値は booking_site の ADMIN の行に持つので、この行は印だけ（DB-02）
  CONSTRAINT chk_performance_override_value_null CHECK ((field_name = 'booking_sites') = (override_value IS NULL)),
  -- ビューが日付・真偽に変換するので、変換できない形を入れない
  CONSTRAINT chk_performance_override_date_format CHECK (
    field_name NOT IN ('start_date','end_date') OR override_value REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'),
  CONSTRAINT chk_performance_override_bool_format CHECK (
    field_name <> 'is_open_run' OR override_value COLLATE utf8mb4_bin IN ('true','false'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci COMMENT='管理者の修正（項目ごとに1行）';

-- ---------------------------------------------------------------------------
-- 駅・コインロッカー
-- ---------------------------------------------------------------------------

CREATE TABLE station (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  external_station_code VARCHAR(20) COLLATE utf8mb4_bin NOT NULL COMMENT '外部の駅コード（고유역번호(외부역코드)）',
  line TINYINT NOT NULL COMMENT '路線（1〜8）',
  name_ko VARCHAR(50) NOT NULL COMMENT '駅名（韓国語）',
  name_ja VARCHAR(100) NULL DEFAULT NULL COMMENT '駅名（日本語の公式表記）',
  latitude DECIMAL(9,6) NOT NULL COMMENT '緯度',
  longitude DECIMAL(9,6) NOT NULL COMMENT '経度',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  PRIMARY KEY (id),
  UNIQUE KEY uk_station_external_station_code (external_station_code),
  UNIQUE KEY uk_station_line_name_ko (line, name_ko),
  KEY idx_station_location (latitude, longitude),
  CONSTRAINT chk_station_line CHECK (line BETWEEN 1 AND 8),
  CONSTRAINT chk_station_latitude_range CHECK (latitude BETWEEN -90 AND 90),
  CONSTRAINT chk_station_longitude_range CHECK (longitude BETWEEN -180 AND 180)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci COMMENT='駅（ソウル交通公社 1〜8号線のソウルの駅。乗り換え駅は路線ごとに1行）';

CREATE TABLE coin_locker (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  station_id BIGINT NOT NULL COMMENT '属する駅',
  name VARCHAR(200) NOT NULL COMMENT '保管庫の名前（보관함명。原文）',
  location_text VARCHAR(500) NOT NULL COMMENT '位置の文章（상세위치。韓国語）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  PRIMARY KEY (id),
  KEY idx_coin_locker_station_id (station_id),
  -- B-02 が駅を入れ替えるとき、属するコインロッカーも消す（05 の 4.3）
  CONSTRAINT fk_coin_locker_station FOREIGN KEY (station_id) REFERENCES station (id) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci COMMENT='コインロッカー（駅に属する）';

-- ---------------------------------------------------------------------------
-- 翻訳
-- ---------------------------------------------------------------------------

CREATE TABLE translation (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  source_hash CHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '原文の SHA-256（小文字の16進）',
  source_text TEXT NOT NULL COMMENT '原文（韓国語）',
  translated_text TEXT NOT NULL COMMENT '日本語の翻訳',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '翻訳した日時',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  PRIMARY KEY (id),
  UNIQUE KEY uk_translation_source_hash (source_hash),
  -- SHA2(文, 256) と同じ形（小文字の16進 64 文字）だけを入れる
  CONSTRAINT chk_translation_source_hash_format CHECK (source_hash REGEXP '^[0-9a-f]{64}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci COMMENT='原文ごとの日本語の翻訳';

CREATE TABLE translation_usage (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  usage_month DATE NOT NULL COMMENT '月（その月の1日）',
  char_count INT NOT NULL DEFAULT 0 COMMENT 'その月に翻訳した文字数',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  PRIMARY KEY (id),
  UNIQUE KEY uk_translation_usage_usage_month (usage_month),
  CONSTRAINT chk_translation_usage_month_first_day CHECK (DAYOFMONTH(usage_month) = 1),
  CONSTRAINT chk_translation_usage_char_count CHECK (char_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci COMMENT='翻訳の月（Asia/Seoul）の文字数';

-- ---------------------------------------------------------------------------
-- 人気順位・ジャンル別トレンド
-- ---------------------------------------------------------------------------

CREATE TABLE ranking_collection (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  period_type ENUM('DAILY','WEEKLY','MONTHLY') NOT NULL COMMENT '期間の種類',
  genre_code VARCHAR(10) NOT NULL COMMENT 'ジャンルのコード（全体は ALL）',
  period_start DATE NOT NULL COMMENT '期間の初日',
  period_end DATE NOT NULL COMMENT '期間の末日',
  row_count INT NOT NULL COMMENT '保存した行の数',
  completed_at DATETIME NOT NULL COMMENT '収集を終えた日時',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ranking_collection_type_genre_start (period_type, genre_code, period_start),
  CONSTRAINT chk_ranking_collection_row_count CHECK (row_count >= 0),
  -- 期間の形（暦の日・週（月〜日）・月）。DAYOFWEEK の 2 は月曜日。非正規化の period_end が初日とずれないようにする（DB-09）
  CONSTRAINT chk_ranking_collection_period_shape CHECK (
    (period_type = 'DAILY'   AND period_end = period_start) OR
    (period_type = 'WEEKLY'  AND DAYOFWEEK(period_start) = 2 AND period_end = period_start + INTERVAL 6 DAY) OR
    (period_type = 'MONTHLY' AND DAYOFMONTH(period_start) = 1 AND period_end = LAST_DAY(period_start))
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci COMMENT='人気順位の収集の完了（期間の種類・期間・ジャンルの組ごとに1行）';

CREATE TABLE ranking_entry (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  ranking_collection_id BIGINT NOT NULL COMMENT 'どの組の順位か',
  provider_rank INT NOT NULL COMMENT '提供元の順位（rnum）',
  -- performance への外部キーは置かない（S2 の時点で公演がまだないことがある。DB-04・D-01）
  kopis_performance_id VARCHAR(20) COLLATE utf8mb4_bin NOT NULL COMMENT '公演 ID（mt20id）。performance とは外部キーで結ばない',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ranking_entry_collection_performance (ranking_collection_id, kopis_performance_id),
  KEY idx_ranking_entry_collection_rank (ranking_collection_id, provider_rank, kopis_performance_id),
  CONSTRAINT fk_ranking_entry_collection FOREIGN KEY (ranking_collection_id) REFERENCES ranking_collection (id) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT chk_ranking_entry_provider_rank CHECK (provider_rank >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci COMMENT='人気順位の行（提供元が返したすべての行）';

CREATE TABLE genre_trend (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  period_start DATE NOT NULL COMMENT '集計期間の初日',
  period_end DATE NOT NULL COMMENT '集計期間の末日',
  genre_code VARCHAR(10) NOT NULL COMMENT 'ジャンルのコード',
  booking_count INT NOT NULL COMMENT '予約数',
  share DECIMAL(9,6) NOT NULL COMMENT '割合（%）',
  basis ENUM('SEOUL','NATIONAL') NOT NULL COMMENT '集計の基準（ソウル / 全国）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  PRIMARY KEY (id),
  -- period_end を先頭にして、API-22 の最新の期間をこのインデックスで引く
  UNIQUE KEY uk_genre_trend_end_start_genre (period_end, period_start, genre_code),
  CONSTRAINT chk_genre_trend_period CHECK (period_end >= period_start),
  CONSTRAINT chk_genre_trend_booking_count CHECK (booking_count >= 0),
  CONSTRAINT chk_genre_trend_share CHECK (share BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci COMMENT='ジャンル別トレンド（直近30日）';

-- ---------------------------------------------------------------------------
-- ユーザー・トークン・お気に入り・レビュー・メール
-- ---------------------------------------------------------------------------

CREATE TABLE app_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  -- 照合順序 utf8mb4_0900_as_ci（表の既定）: 大文字・小文字を同じアドレスとみなす（D-03）
  email VARCHAR(254) NOT NULL COMMENT 'メールアドレス',
  password_hash VARCHAR(255) COLLATE utf8mb4_bin NOT NULL COMMENT 'パスワードのハッシュ（DelegatingPasswordEncoder の形）',
  -- 照合順序 utf8mb4_0900_as_ci（D-02 の案 A。決定: Issue #20 の確認2）
  nickname VARCHAR(20) COLLATE utf8mb4_0900_as_ci NOT NULL COMMENT 'ニックネーム（NFKC に正規化し前後の空白を除いた値。表示もこの値）',
  role ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER' COMMENT '役割',
  status ENUM('PENDING','ACTIVE') NOT NULL DEFAULT 'PENDING' COMMENT '状態（認証前 / 有効）',
  -- 有効な行の間だけニックネームを一意にする（認証前の行は NULL。DB-06）
  active_nickname VARCHAR(20) COLLATE utf8mb4_0900_as_ci
    GENERATED ALWAYS AS (CASE WHEN status = 'ACTIVE' THEN nickname END) VIRTUAL
    COMMENT '有効な行のニックネーム（一意制約のための生成列）',
  -- 管理者を1行にする（一般ユーザーの行は NULL。DB-07・D-04）
  admin_singleton TINYINT
    GENERATED ALWAYS AS (CASE WHEN role = 'ADMIN' THEN 1 END) VIRTUAL
    COMMENT '管理者の行だけ 1（管理者を1行にする生成列）',
  failed_login_count INT NOT NULL DEFAULT 0 COMMENT '連続のログインの失敗回数',
  locked_at DATETIME NULL DEFAULT NULL COMMENT 'ロックした日時（NULL でなければロック中）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時（申込・置き換えの日時）',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  PRIMARY KEY (id),
  UNIQUE KEY uk_app_user_email (email),
  UNIQUE KEY uk_app_user_active_nickname (active_nickname),
  UNIQUE KEY uk_app_user_admin_singleton (admin_singleton),
  KEY idx_app_user_status (status),
  CONSTRAINT chk_app_user_nickname_length CHECK (CHAR_LENGTH(nickname) >= 1),
  CONSTRAINT chk_app_user_failed_login_count CHECK (failed_login_count >= 0),
  -- 管理者はいつも有効（05 の 5. の 5.）
  CONSTRAINT chk_app_user_admin_active CHECK (role <> 'ADMIN' OR status = 'ACTIVE')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci COMMENT='ユーザー（一般ユーザー・管理者。認証前の申込を含む）';

CREATE TABLE auth_token (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  user_id BIGINT NOT NULL COMMENT '対象のユーザー',
  purpose ENUM('SIGNUP','PASSWORD_RESET','UNLOCK') NOT NULL COMMENT '用途',
  token_hash CHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT 'トークンの SHA-256（小文字の16進）',
  expires_at DATETIME NOT NULL COMMENT '有効期限',
  used_at DATETIME NULL DEFAULT NULL COMMENT '使った日時（NULL でなければ使用済み）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_token_token_hash (token_hash),
  KEY idx_auth_token_user_purpose_expires (user_id, purpose, expires_at),
  -- 退会・認証前の行の削除でトークンも消す
  CONSTRAINT fk_auth_token_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT chk_auth_token_token_hash_format CHECK (token_hash REGEXP '^[0-9a-f]{64}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci COMMENT='メールのリンクのトークン';

CREATE TABLE favorite (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  user_id BIGINT NOT NULL COMMENT 'ユーザー',
  performance_id BIGINT NOT NULL COMMENT '公演',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登録日時',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  PRIMARY KEY (id),
  UNIQUE KEY uk_favorite_user_performance (user_id, performance_id),
  KEY idx_favorite_user_created (user_id, created_at),
  KEY idx_favorite_performance_id (performance_id),
  CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_favorite_performance FOREIGN KEY (performance_id) REFERENCES performance (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci COMMENT='お気に入り';

CREATE TABLE review (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  user_id BIGINT NOT NULL COMMENT '投稿者',
  performance_id BIGINT NOT NULL COMMENT '公演',
  rating TINYINT NOT NULL COMMENT '評価（1〜5）',
  body VARCHAR(1000) NULL DEFAULT NULL COMMENT '本文（なければ NULL）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投稿日時',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  PRIMARY KEY (id),
  UNIQUE KEY uk_review_user_performance (user_id, performance_id),
  KEY idx_review_performance_created (performance_id, created_at),
  CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_review_performance FOREIGN KEY (performance_id) REFERENCES performance (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5),
  -- 本文なしは NULL で表す（空文字を入れない。空白だけかの判定はアプリ）
  CONSTRAINT chk_review_body CHECK (body IS NULL OR CHAR_LENGTH(body) >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci COMMENT='レビュー';

CREATE TABLE mail_send_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  purpose ENUM('SIGNUP','PASSWORD_RESET','UNLOCK') NOT NULL COMMENT '用途',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '送った日時',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  PRIMARY KEY (id),
  KEY idx_mail_send_log_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci COMMENT='メールの送信の記録';

-- ---------------------------------------------------------------------------
-- バッチの実行の記録
-- ---------------------------------------------------------------------------

CREATE TABLE batch_job_run (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  job_name VARCHAR(50) NOT NULL COMMENT '処理の名前（daily-collect など）',
  started_at DATETIME NOT NULL COMMENT '開始日時',
  finished_at DATETIME NULL DEFAULT NULL COMMENT '終了日時',
  result ENUM('SUCCESS','PARTIAL_FAILURE','FAILURE') NULL DEFAULT NULL COMMENT '結果',
  error_message TEXT NULL COMMENT 'エラーの内容',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  PRIMARY KEY (id),
  KEY idx_batch_job_run_name_started (job_name, started_at),
  -- 終わったら結果がある
  CONSTRAINT chk_batch_job_run_finished CHECK ((finished_at IS NULL) = (result IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci COMMENT='バッチの実行の記録';

CREATE TABLE batch_step_run (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  job_run_id BIGINT NOT NULL COMMENT 'どの実行のステップか',
  step_name VARCHAR(50) NOT NULL COMMENT 'ステップ（S1〜S7 など）',
  started_at DATETIME NOT NULL COMMENT '開始日時',
  finished_at DATETIME NULL DEFAULT NULL COMMENT '終了日時',
  result ENUM('SUCCESS','FAILURE') NULL DEFAULT NULL COMMENT '結果',
  processed_count INT NULL DEFAULT NULL COMMENT '処理した件数',
  summary TEXT NULL COMMENT '件数の内訳',
  error_message TEXT NULL COMMENT 'エラーの内容',
  window_end_date DATE NULL DEFAULT NULL COMMENT 'S1 が成功したときの収集の窓の末日',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  PRIMARY KEY (id),
  UNIQUE KEY uk_batch_step_run_job_step (job_run_id, step_name),
  KEY idx_batch_step_run_step_result_started (step_name, result, started_at),
  CONSTRAINT fk_batch_step_run_job_run FOREIGN KEY (job_run_id) REFERENCES batch_job_run (id) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT chk_batch_step_run_processed_count CHECK (processed_count >= 0),
  CONSTRAINT chk_batch_step_run_finished CHECK ((finished_at IS NULL) = (result IS NULL)),
  -- 窓の末日は成功した S1 だけが持つ（DB-10）
  CONSTRAINT chk_batch_step_run_window CHECK (window_end_date IS NULL OR (step_name = 'S1' AND result = 'SUCCESS'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci COMMENT='バッチのステップの実行の記録';

-- ---------------------------------------------------------------------------
-- ビュー performance_effective（管理者の修正を反映した公演の値。04 の 11.3）
-- 一般ユーザー向けの API が読む。MERGE で外側の WHERE・ORDER BY と一緒に最適化される。
-- 集約・DISTINCT・select の中のサブクエリを入れると MERGE にならないので入れない。
-- ビューには COMMENT を付けられないため、説明はこのコメントと 04 に書く。
-- ---------------------------------------------------------------------------

CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW performance_effective AS
SELECT
  p.id,
  p.kopis_performance_id,
  p.venue_id,
  CASE WHEN o_title.id   IS NULL THEN p.title         ELSE o_title.override_value                  END AS title,
  CASE WHEN o_start.id   IS NULL THEN p.start_date    ELSE CAST(o_start.override_value AS DATE)    END AS start_date,
  CASE WHEN o_end.id     IS NULL THEN p.end_date      ELSE CAST(o_end.override_value AS DATE)      END AS end_date,
  CASE WHEN o_genre.id   IS NULL THEN p.genre_code    ELSE o_genre.override_value                  END AS genre_code,
  CASE WHEN o_cast.id    IS NULL THEN p.cast_text     ELSE o_cast.override_value                   END AS cast_text,
  CASE WHEN o_price.id   IS NULL THEN p.ticket_price  ELSE o_price.override_value                  END AS ticket_price,
  CASE WHEN o_sched.id   IS NULL THEN p.schedule_text ELSE o_sched.override_value                  END AS schedule_text,
  CASE WHEN o_run.id     IS NULL THEN p.runtime       ELSE o_run.override_value                    END AS runtime,
  CASE WHEN o_age.id     IS NULL THEN p.age_limit     ELSE o_age.override_value                    END AS age_limit,
  CASE WHEN o_open.id    IS NULL THEN p.is_open_run   ELSE (o_open.override_value = 'true')        END AS is_open_run,
  (o_sites.id IS NOT NULL) AS has_booking_site_override,
  p.poster_url, p.total_show_count, p.has_award, p.is_seoul, p.is_hidden
FROM performance p
LEFT JOIN performance_override o_title ON o_title.performance_id = p.id AND o_title.field_name = 'title'
LEFT JOIN performance_override o_start ON o_start.performance_id = p.id AND o_start.field_name = 'start_date'
LEFT JOIN performance_override o_end   ON o_end.performance_id   = p.id AND o_end.field_name   = 'end_date'
LEFT JOIN performance_override o_genre ON o_genre.performance_id = p.id AND o_genre.field_name = 'genre_code'
LEFT JOIN performance_override o_cast  ON o_cast.performance_id  = p.id AND o_cast.field_name  = 'cast_text'
LEFT JOIN performance_override o_price ON o_price.performance_id = p.id AND o_price.field_name = 'ticket_price'
LEFT JOIN performance_override o_sched ON o_sched.performance_id = p.id AND o_sched.field_name = 'schedule_text'
LEFT JOIN performance_override o_run   ON o_run.performance_id   = p.id AND o_run.field_name   = 'runtime'
LEFT JOIN performance_override o_age   ON o_age.performance_id   = p.id AND o_age.field_name   = 'age_limit'
LEFT JOIN performance_override o_open  ON o_open.performance_id  = p.id AND o_open.field_name  = 'is_open_run'
LEFT JOIN performance_override o_sites ON o_sites.performance_id = p.id AND o_sites.field_name = 'booking_sites';
