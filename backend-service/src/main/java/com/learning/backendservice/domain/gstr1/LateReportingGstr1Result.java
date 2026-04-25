package com.learning.backendservice.domain.gstr1;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable result of the GSTR-1 Late Reporting Interest calculation.
 *
 * <p>All amounts use {@code BigDecimal} with scale=2, RoundingMode.HALF_UP
 * per the GST Expert skill requirement.
 *
 * @param belatedInvoices   list of invoices detected as belated (may be empty for a clean return)
 * @param totalBelated      count of belated invoices
 * @param totalInterest     sum of Section 50(1) interest across all belated invoices (₹)
 * @param totalTaxAtRisk    sum of tax amounts on all belated invoices — total exposure (₹)
 */
public record LateReportingGstr1Result(
        List<BelatedInvoice> belatedInvoices,
        int                  totalBelated,
        BigDecimal           totalInterest,
        BigDecimal           totalTaxAtRisk
) {
    /** Convenience factory — clean return with no belated invoices. */
    public static LateReportingGstr1Result clean() {
        return new LateReportingGstr1Result(List.of(), 0, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
