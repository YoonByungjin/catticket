-- 上演回数をやめる（Issue #38 の人間の決定1、2026-11-20。01 の R-69・X-30。docs/design/04-tables.md の 11.10）
-- KOPIS から開幕からの累計の上演回数を得られないため、F-32 ② を廃止し、B-01 の S5 をなくした（05 の 3.6）。
-- V1 は適用済みなので編集せず、このファイルで変える。

-- ---------------------------------------------------------------------------
-- ビュー performance_effective（04 の 11.3）から total_show_count を外す
-- 列を消す前に作り直す（消した列を参照したままのビューが残らないように）。
-- 定義は V1 と同じで、最後の行から p.total_show_count を除いただけ。
-- ---------------------------------------------------------------------------

CREATE OR REPLACE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW performance_effective AS
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
  p.poster_url, p.has_award, p.is_seoul, p.is_hidden
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

-- ---------------------------------------------------------------------------
-- performance.total_show_count と、その CHECK を消す
-- CHECK が参照している列は消せないので、同じ ALTER の中で CHECK を先に消す。
-- ---------------------------------------------------------------------------

ALTER TABLE performance
  DROP CHECK chk_performance_total_show_count,
  DROP COLUMN total_show_count;

-- ---------------------------------------------------------------------------
-- batch_step_run.step_name の COMMENT を直す（S5 は欠番。05 の 3.6）
-- 型・NULL・照合順序（表の既定）は V1 と同じ。COMMENT だけを変える。
-- ---------------------------------------------------------------------------

ALTER TABLE batch_step_run
  MODIFY COLUMN step_name VARCHAR(50) NOT NULL COMMENT 'ステップ（B-01 は S1〜S4・S6・S7。S5 は欠番）';
