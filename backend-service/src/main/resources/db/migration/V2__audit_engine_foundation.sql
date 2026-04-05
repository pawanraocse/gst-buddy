/*
  # Phase 2: Generic Audit Engine Foundation
  # Migration: V2__audit_engine_foundation.sql
  #
  # Operations (in order):
  #   1. CREATE audit_runs            — generic audit run tracking (all rules)
  #   2. CREATE audit_findings        — individual compliance findings
  #   3. CREATE late_fee_relief_windows — CBIC waiver period reference data
  #   4. CREATE parser_templates      — document fingerprint registry (D6)
  #   5. CREATE parsed_documents      — WORM raw document metadata (D7)
  #   6. MIGRATE data: rule37_calculation_runs → audit_runs
  #   7. DROP rule37_calculation_runs — legacy table removed
  #
  # Primary Keys: UUID (generated as UUID v7 from Java for new records;
  #               gen_random_uuid() used for migrated legacy records)
  #
  # Rollback: One-way migration. Restore from DB backup if needed.
  #           Ensure backup is taken before applying to production.
*/

-- ═══════════════════════════════════════════════════════════════════════════
-- 1. CORE AUDIT ENGINE TABLES
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE audit_runs (
    id                   UUID          PRIMARY KEY,
    tenant_id            VARCHAR(64)   NOT NULL,
    user_id              VARCHAR(255)  NOT NULL,
    rule_id              VARCHAR(100)  NOT NULL,
    status               VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    s3_raw_key           VARCHAR(500),
    input_metadata       JSONB,
    result_data          JSONB,
    total_impact_amount  DECIMAL(18,2) NOT NULL DEFAULT 0,
    credits_consumed     INT           NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    completed_at         TIMESTAMPTZ,
    expires_at           TIMESTAMPTZ   NOT NULL DEFAULT (NOW() + INTERVAL '7 days'),

    CONSTRAINT chk_audit_run_status
        CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED'))
);

CREATE INDEX idx_audit_runs_tenant  ON audit_runs (tenant_id, created_at DESC);
CREATE INDEX idx_audit_runs_rule    ON audit_runs (rule_id,   created_at DESC);
CREATE INDEX idx_audit_runs_expires ON audit_runs (expires_at);
CREATE INDEX idx_audit_runs_user    ON audit_runs (user_id,   created_at DESC);

COMMENT ON TABLE  audit_runs                     IS 'Generic audit run tracking — all GST rule types';
COMMENT ON COLUMN audit_runs.id                  IS 'UUID v7 (time-sorted) — generated in Java; migrated rows use UUID v4';
COMMENT ON COLUMN audit_runs.rule_id             IS 'AuditRule.getRuleId() — e.g. RULE_37_ITC_REVERSAL, LATE_FEE_GSTR1';
COMMENT ON COLUMN audit_runs.status              IS 'PENDING | RUNNING | SUCCESS | FAILED';
COMMENT ON COLUMN audit_runs.s3_raw_key          IS 'S3 object key for raw uploaded document (WORM audit trail)';
COMMENT ON COLUMN audit_runs.input_metadata      IS 'Rule-specific input params (asOnDate, filename, GSTIN, etc.) as JSONB';
COMMENT ON COLUMN audit_runs.result_data         IS 'Rule-specific output JSONB; deserialised by rule_id in Java';
COMMENT ON COLUMN audit_runs.total_impact_amount IS 'Aggregate financial impact = ITC reversal + interest + penalties';

-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE audit_findings (
    id                 UUID          PRIMARY KEY,
    run_id             UUID          NOT NULL REFERENCES audit_runs (id) ON DELETE CASCADE,
    tenant_id          VARCHAR(64)   NOT NULL,
    rule_id            VARCHAR(100)  NOT NULL,
    severity           VARCHAR(20)   NOT NULL,
    legal_basis        TEXT,
    compliance_period  VARCHAR(50),
    impact_amount      DECIMAL(18,2) NOT NULL DEFAULT 0,
    description        TEXT          NOT NULL,
    recommended_action TEXT,
    auto_fix_available BOOLEAN       NOT NULL DEFAULT false,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_finding_severity
        CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'))
);

CREATE INDEX idx_findings_run             ON audit_findings (run_id);
CREATE INDEX idx_findings_tenant_severity ON audit_findings (tenant_id, severity);

COMMENT ON TABLE  audit_findings                  IS 'Individual compliance findings from audit runs';
COMMENT ON COLUMN audit_findings.severity         IS 'CRITICAL | HIGH | MEDIUM | LOW | INFO';
COMMENT ON COLUMN audit_findings.compliance_period IS 'e.g. FY: 2024-25, Tax Period: Apr-2024';

-- ═══════════════════════════════════════════════════════════════════════════
-- 2. LATE FEE RELIEF WINDOWS (Phase 1 prerequisite for GSTR-1/3B late fee rules)
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE late_fee_relief_windows (
    id               SERIAL        PRIMARY KEY,
    return_type      VARCHAR(10)   NOT NULL,
    notification_no  VARCHAR(100)  NOT NULL,
    start_date       DATE          NOT NULL,
    end_date         DATE          NOT NULL,
    fee_cgst_per_day DECIMAL(8,2),
    fee_sgst_per_day DECIMAL(8,2),
    max_cap_cgst     DECIMAL(12,2),
    max_cap_sgst     DECIMAL(12,2),
    applies_to       VARCHAR(20)   NOT NULL DEFAULT 'ALL',
    notes            TEXT,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_return_type  CHECK (return_type IN ('GSTR1', 'GSTR3B', 'GSTR9', 'GSTR9C')),
    CONSTRAINT chk_applies_to   CHECK (applies_to  IN ('NIL', 'NON_NIL', 'ALL'))
);

COMMENT ON TABLE  late_fee_relief_windows            IS 'CBIC notification-driven GST late fee waiver/reduction windows';
COMMENT ON COLUMN late_fee_relief_windows.applies_to IS 'NIL = nil-return filers only; NON_NIL = all others; ALL = both';

-- ═══════════════════════════════════════════════════════════════════════════
-- 3. PARSER INFRASTRUCTURE (D6/D7 — future Python sidecar integration)
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE parser_templates (
    id               SERIAL        PRIMARY KEY,
    template_id      VARCHAR(100)  NOT NULL UNIQUE,
    doc_type         VARCHAR(30)   NOT NULL,
    fingerprint      JSONB         NOT NULL,
    extraction_rules JSONB         NOT NULL,
    version          INT           NOT NULL DEFAULT 1,
    is_active        BOOLEAN       NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_parser_templates_active ON parser_templates (doc_type, is_active);

COMMENT ON TABLE  parser_templates                  IS 'Template registry for document fingerprinting and classification (D6)';
COMMENT ON COLUMN parser_templates.doc_type         IS 'e.g. TALLY_LEDGER, BUSY_LEDGER, GSTR2B_PDF, GSTR1_JSON';
COMMENT ON COLUMN parser_templates.fingerprint      IS 'Column headers / structural markers for classification';
COMMENT ON COLUMN parser_templates.extraction_rules IS 'Extraction configuration; interpreted by Python sidecar';

-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE parsed_documents (
    id                UUID          PRIMARY KEY,
    tenant_id         VARCHAR(64)   NOT NULL,
    original_filename VARCHAR(500)  NOT NULL,
    doc_type          VARCHAR(30)   NOT NULL,
    s3_raw_key        VARCHAR(500)  NOT NULL,
    template_id       VARCHAR(100),
    parsed_json       JSONB,
    parser_version    VARCHAR(20)   NOT NULL,
    parse_status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    parse_duration_ms INT,
    error_message     TEXT,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_parse_status
        CHECK (parse_status IN ('PENDING', 'SUCCESS', 'FAILED', 'UNSUPPORTED'))
);

CREATE INDEX idx_parsed_docs_tenant ON parsed_documents (tenant_id);
CREATE INDEX idx_parsed_docs_status ON parsed_documents (parse_status);

COMMENT ON TABLE  parsed_documents           IS 'Parsed document metadata + S3 raw reference (D7 / WORM pattern)';
COMMENT ON COLUMN parsed_documents.s3_raw_key IS 'Immutable S3 key; cleaned up via lifecycle rule after retention period';

-- ═══════════════════════════════════════════════════════════════════════════
-- 4. DATA MIGRATION: rule37_calculation_runs → audit_runs
--    Note: gen_random_uuid() produces UUID v4 for migrated records.
--    All new records created by Java will use UUID v7 (time-sorted).
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO audit_runs (
    id,
    tenant_id,
    user_id,
    rule_id,
    status,
    input_metadata,
    result_data,
    total_impact_amount,
    credits_consumed,
    created_at,
    expires_at
)
SELECT
    gen_random_uuid(),
    tenant_id,
    COALESCE(created_by, 'migrated-unknown'),
    'RULE_37_ITC_REVERSAL',
    'SUCCESS',
    jsonb_build_object(
        'asOnDate',         as_on_date::text,
        'filename',         filename,
        'totalInterest',    total_interest,
        'totalItcReversal', total_itc_reversal,
        'migratedFromV1',   true
    ),
    calculation_data,
    COALESCE(total_interest, 0) + COALESCE(total_itc_reversal, 0),
    1,
    created_at,
    expires_at
FROM rule37_calculation_runs;

-- ═══════════════════════════════════════════════════════════════════════════
-- 5. DROP LEGACY TABLE
-- ═══════════════════════════════════════════════════════════════════════════

DROP TABLE rule37_calculation_runs;
