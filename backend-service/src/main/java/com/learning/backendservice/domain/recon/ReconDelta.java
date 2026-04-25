package com.learning.backendservice.domain.recon;

import java.math.BigDecimal;

/**
 * Per-tax-head delta from a GSTR-1 vs GSTR-3B reconciliation.
 *
 * <p>A positive {@code delta} means the taxpayer declared more liability in GSTR-1
 * than was reported as tax-payable in GSTR-3B — i.e. under-payment risk.
 * A negative delta means the GSTR-3B declared more — i.e. over-payment.
 *
 * @param taxHead       "IGST" | "CGST" | "SGST/UTGST" | "CESS"
 * @param gstr1Amount   amount declared in GSTR-1 for this tax head (₹)
 * @param gstr3bAmount  amount declared in GSTR-3B Table 6.1 for this tax head (₹)
 * @param delta         gstr1Amount − gstr3bAmount (₹); positive = under-reported in 3B
 * @param deltaPercent  |delta| / gstr1Amount × 100 (0.00 when gstr1Amount = 0)
 * @param severity      classification of this delta
 */
public record ReconDelta(
        String        taxHead,
        BigDecimal    gstr1Amount,
        BigDecimal    gstr3bAmount,
        BigDecimal    delta,
        BigDecimal    deltaPercent,
        ReconSeverity severity
) {}
