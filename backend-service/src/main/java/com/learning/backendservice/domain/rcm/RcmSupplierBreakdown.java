package com.learning.backendservice.domain.rcm;

import java.math.BigDecimal;

/**
 * Supplier-level breakdown of RCM liability identified in the purchase register.
 */
public record RcmSupplierBreakdown(
        String supplierGstin,
        int invoiceCount,
        BigDecimal totalTaxableValue,
        BigDecimal totalRcmTax
) {}
