-- Rule 86B Configuration
CREATE TABLE IF NOT EXISTS rule_86b_config (
    id                    UUID PRIMARY KEY,
    tenant_id             VARCHAR(64) NOT NULL,
    turnover_threshold    DECIMAL(18,2) NOT NULL DEFAULT 5000000.00, -- ₹50 lakh
    cash_percent_floor    DECIMAL(5,4) NOT NULL DEFAULT 0.0100,      -- 1%
    effective_from        DATE NOT NULL DEFAULT '2021-01-01',
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Seed default for all tenants
INSERT INTO rule_86b_config (id, tenant_id, turnover_threshold, cash_percent_floor, effective_from)
VALUES ('018e95c1-e77a-7bd8-a78b-0e543666b6c0', 'DEFAULT', 5000000.00, 0.0100, '2021-01-01')
ON CONFLICT DO NOTHING;
