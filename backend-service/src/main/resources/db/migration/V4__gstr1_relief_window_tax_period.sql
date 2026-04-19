-- ═══════════════════════════════════════════════════════════════════════════
-- V4: Add tax-period range columns to late_fee_relief_windows
--
-- A CBIC notification relief window applies based on TWO conditions:
--   1. The actual filing date (ARN date) falls within the notification window
--      (already captured by start_date / end_date in V2).
--   2. The tax period to which the return relates falls within the relief scope.
--      (tax_period_from / tax_period_to — added here).
--
-- When both columns are NULL, the relief applies to ALL tax periods within
-- the filing date window (backward-compatible sentinel).
--
-- Rollback: ALTER TABLE late_fee_relief_windows
--               DROP COLUMN IF EXISTS tax_period_from,
--               DROP COLUMN IF EXISTS tax_period_to;
-- ═══════════════════════════════════════════════════════════════════════════

ALTER TABLE late_fee_relief_windows
    ADD COLUMN IF NOT EXISTS tax_period_from DATE,
    ADD COLUMN IF NOT EXISTS tax_period_to   DATE;

COMMENT ON COLUMN late_fee_relief_windows.tax_period_from
    IS 'Inclusive start of the tax period range this relief covers. NULL = all periods.';
COMMENT ON COLUMN late_fee_relief_windows.tax_period_to
    IS 'Inclusive end of the tax period range this relief covers. NULL = all periods.';

-- ── Seed real CBIC amnesty data ──────────────────────────────────────────
-- Notification 19/2021-CT: Late fee waiver for GSTR-1 for periods Feb-2020 to Apr-2021
-- filed between 01-Jun-2021 and 31-Aug-2021
INSERT INTO late_fee_relief_windows
    (return_type, notification_no, start_date, end_date,
     fee_cgst_per_day, fee_sgst_per_day, max_cap_cgst, max_cap_sgst,
     applies_to, tax_period_from, tax_period_to, notes)
VALUES
    ('GSTR1', 'Notification No. 19/2021-CT (dt. 01-Jun-2021)',
     '2021-06-01', '2021-08-31',
     0.00, 0.00, 500.00, 500.00,
     'ALL',
     '2020-02-01', '2021-04-30',
     'COVID amnesty: Full late fee waiver for GSTR-1 filed between Jun-Aug 2021 for periods Feb-2020 to Apr-2021')

ON CONFLICT DO NOTHING;

-- Notification 20/2021-CT: Reduced late fee for GSTR-1 for FY 2017-18, 2018-19, 2019-20
-- filed between 01-Jun-2021 and 31-Aug-2021
INSERT INTO late_fee_relief_windows
    (return_type, notification_no, start_date, end_date,
     fee_cgst_per_day, fee_sgst_per_day, max_cap_cgst, max_cap_sgst,
     applies_to, tax_period_from, tax_period_to, notes)
VALUES
    ('GSTR1', 'Notification No. 20/2021-CT (dt. 01-Jun-2021)',
     '2021-06-01', '2021-08-31',
     25.00, 25.00, 500.00, 500.00,
     'NON_NIL',
     '2017-07-01', '2020-03-31',
     'COVID amnesty: Reduced cap of ₹500 (CGST) + ₹500 (SGST) for non-nil GSTR-1 for FY 17-18 to 19-20')

ON CONFLICT DO NOTHING;
