package com.learning.backendservice.domain.rcm;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Result of RCM Reconciliation.
 */
public record RcmRecoResult(
        YearMonth taxPeriod,
        List<RcmMismatch> mismatches,
        List<RcmSupplierBreakdown> supplierBreakdown,
        BigDecimal totalAbsoluteMismatch,
        int totalRcmInvoicesInBooks,
        BigDecimal totalRcmTaxableValueInBooks,
        String narrative
) {}
