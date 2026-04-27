package com.learning.backendservice.domain.recon;

import java.math.BigDecimal;

/**
 * Captures the differences across GSTR-1, GSTR-3B, and GSTR-9 for a specific tax head.
 */
public record ThreeWayReconDelta(
        String taxHead,
        BigDecimal gstr1Amount,
        BigDecimal gstr3bAmount,
        BigDecimal gstr9DeclaredAmount, // Table 4 + 5 or just Table 4 (taxable)
        BigDecimal gstr9PaidAmount,     // Table 9 paid
        BigDecimal delta1Vs3b,          // GSTR-1 - GSTR-3B
        BigDecimal delta3bVs9,          // GSTR-3B - GSTR-9 Paid
        BigDecimal delta1Vs9            // GSTR-1 - GSTR-9 Declared
) {}
