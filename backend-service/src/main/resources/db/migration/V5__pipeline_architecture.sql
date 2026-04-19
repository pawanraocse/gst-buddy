-- Rollback:
--   ALTER TABLE audit_runs ADD COLUMN IF NOT EXISTS rule_id VARCHAR(100);
--   ALTER TABLE audit_runs DROP COLUMN IF EXISTS rules_executed;
--   ALTER TABLE audit_runs DROP COLUMN IF EXISTS analysis_mode;
--   DROP TABLE IF EXISTS audit_run_rule_results;

-- ── 1. Add new columns to audit_runs ──────────────────────────────────────
ALTER TABLE audit_runs
    ADD COLUMN IF NOT EXISTS analysis_mode VARCHAR(30) DEFAULT 'LEDGER_ANALYSIS',
    ADD COLUMN IF NOT EXISTS rules_executed TEXT[] DEFAULT ARRAY[]::TEXT[];

-- ── 2. Migrate existing single-rule data into new columns ──────────────────
UPDATE audit_runs
SET rules_executed = ARRAY[rule_id],
    analysis_mode  = CASE
                         WHEN rule_id = 'RULE_37_ITC_REVERSAL' THEN 'LEDGER_ANALYSIS'
                         ELSE 'GSTR_RULES_ANALYSIS'
                     END
WHERE rule_id IS NOT NULL
  AND rules_executed = ARRAY[]::TEXT[];

-- ── 3. Drop the now-superseded rule_id column ──────────────────────────────
ALTER TABLE audit_runs DROP COLUMN IF EXISTS rule_id;

-- ── 4. GIN index on rules_executed for fast array contains queries ─────────
CREATE INDEX IF NOT EXISTS idx_audit_runs_rules_executed
    ON audit_runs USING GIN (rules_executed);

-- ── 5. Per-rule execution tracking table ──────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_run_rule_results (
    id                    UUID          PRIMARY KEY,
    run_id                UUID          NOT NULL REFERENCES audit_runs(id) ON DELETE CASCADE,
    tenant_id             VARCHAR(64)   NOT NULL,
    rule_id               VARCHAR(100)  NOT NULL,
    rule_name             VARCHAR(200),
    legal_basis           TEXT,
    status                VARCHAR(20)   NOT NULL DEFAULT 'SUCCESS',
    impact_amount         DECIMAL(18,2) DEFAULT 0,
    findings_count        INT           DEFAULT 0,
    execution_duration_ms INT,
    error_message         TEXT,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_run_rule_results_run_id
    ON audit_run_rule_results (run_id);

CREATE INDEX IF NOT EXISTS idx_run_rule_results_tenant
    ON audit_run_rule_results (tenant_id);
