-- V8: Add GSTIN Status Cache
CREATE TABLE IF NOT EXISTS gstin_status_cache (
    gstin VARCHAR(15) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    cancellation_date DATE,
    last_filing_period VARCHAR(10),
    last_checked_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    ttl_expires_at TIMESTAMP WITH TIME ZONE DEFAULT (NOW() + interval '7 days')
);

CREATE INDEX IF NOT EXISTS idx_gstin_cache_ttl ON gstin_status_cache (ttl_expires_at);

-- ROLLBACK:
-- DROP INDEX IF EXISTS idx_gstin_cache_ttl;
-- DROP TABLE IF EXISTS gstin_status_cache;
