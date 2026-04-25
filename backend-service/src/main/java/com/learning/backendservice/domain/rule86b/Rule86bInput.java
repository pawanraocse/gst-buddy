package com.learning.backendservice.domain.rule86b;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Input for Rule 86B evaluation.
 */
public record Rule86bInput(
        String gstin,
        LocalDate period,
        BigDecimal monthlyTaxableOutward,
        BigDecimal totalTaxPayable,
        BigDecimal rcmTaxPayable,
        BigDecimal paidInCash,
        boolean hasGovtPsuFormat,
        boolean hasExportInvoices,
        Rule86bConfigSnapshot config
) {}
