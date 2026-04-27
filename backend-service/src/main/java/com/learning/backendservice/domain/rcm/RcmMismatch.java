package com.learning.backendservice.domain.rcm;

import java.math.BigDecimal;

/**
 * Per-tax-head mismatch result for RCM reconciliation.
 */
public record RcmMismatch(
        String taxHead,
        BigDecimal booksAmount,
        BigDecimal gstr3bAmount,
        BigDecimal delta,
        RcmMismatchType type
) {}
