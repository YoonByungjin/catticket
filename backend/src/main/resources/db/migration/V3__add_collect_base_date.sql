-- S1 の収集の基準日を足す（Issue #38 の人間の決定3、2026-11-25。06 の 11 の U-17。docs/design/04-tables.md の 11.11）
-- S1 が上限・遮断・失敗で途中で止まったとき、次の実行は基準日の 0 時より後に収集した公演を飛ばして続きから取り、
-- 成功したときは基準日を「前回 S1 が成功した日」にする（05 の 3.3 の 12.・3.5）。
-- V1・V2 は適用済みなので編集せず、このファイルで変える。

-- ---------------------------------------------------------------------------
-- batch_step_run.collect_base_date
-- 成功・失敗とも S1 の行に記録する。S1 以外は NULL（CHECK）。
-- 成功した S1 に NOT NULL を強いる CHECK は付けない: V3 より前の成功の行は NULL で、
-- その行を区切る列がない。NULL の行は DATE(started_at) で読む（DB-10）。
-- ---------------------------------------------------------------------------

ALTER TABLE batch_step_run
  ADD COLUMN collect_base_date DATE NULL DEFAULT NULL
    COMMENT 'S1 の収集の基準日（続きの S1 では続きを始めた日。05 の 3.3 の 12.）。S1 以外は NULL'
    AFTER window_end_date,
  ADD CONSTRAINT chk_batch_step_run_collect_base_date CHECK (collect_base_date IS NULL OR step_name = 'S1');
