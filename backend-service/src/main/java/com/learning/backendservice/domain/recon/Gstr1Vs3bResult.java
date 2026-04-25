package com.learning.backendservice.domain.recon;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Immutable result of a GSTR-1 vs GSTR-3B reconciliation calculation.
 *
 * @param taxPeriod       the GST tax period that was reconciled
 * @param deltas          per-tax-head deltas (IGST, CGST, SGST/UTGST, CESS)
 * @param totalDelta      sum of |delta| across all tax heads (₹)
 * @param overallSeverity worst-case severity across all tax heads
 * @param narrative       human-readable reconciliation summary citing Section 37 + 39
 */
public record Gstr1Vs3bResult(
        YearMonth        taxPeriod,
        List<ReconDelta> deltas,
        BigDecimal       totalDelta,
        ReconSeverity    overallSeverity,
        String           narrative
) {}
