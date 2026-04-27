package com.learning.backendservice.domain.recon;

import java.math.BigDecimal;

public record Gstr1Vs3bVs9Input(
        String gstin,
        String financialYear,
        TaxPaymentSummary gstr1Aggregated,
        TaxPaymentSummary gstr3bAggregated,
        TaxPaymentSummary gstr9Table4,
        TaxPaymentSummary gstr9Table9Paid,
        BigDecimal reconToleranceAmount
) {}
