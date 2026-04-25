-- Phase C — Reconciliation Tolerance Config
-- Rollback: DROP TABLE IF EXISTS recon_tolerance_config;

CREATE TABLE IF NOT EXISTS recon_tolerance_config (
    id                  UUID            PRIMARY KEY,
    tenant_id           VARCHAR(64)     NOT NULL,
    tolerance_amount    DECIMAL(18, 2)  NOT NULL DEFAULT 1.00,
    tolerance_percent   DECIMAL(7, 6)   NOT NULL DEFAULT 0.000100,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_recon_tolerance_tenant
    ON recon_tolerance_config (tenant_id);

-- Seed: global fallback row — used when a tenant has no custom config.
-- ContextEnricher queries by tenantId first, falls back to 'DEFAULT'.
INSERT INTO recon_tolerance_config (id, tenant_id, tolerance_amount, tolerance_percent)
VALUES (gen_random_uuid(), 'DEFAULT', 1.00, 0.000100)
ON CONFLICT DO NOTHING;
