package com.learning.backendservice.domain.rule86b;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of Rule 86B evaluation.
 */
public record Rule86bResult(
        boolean isApplicable,
        boolean isBreached,
        boolean hasExemption,
        String exemptionReason,
        BigDecimal outputTaxPayable,
        BigDecimal outputPaidInCash,
        BigDecimal requiredCashPayment,
        BigDecimal cashShortfall,
        BigDecimal actualCashPercent,
        List<String> manualAdvisories
) {}
