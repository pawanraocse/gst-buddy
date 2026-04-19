package com.learning.backendservice.domain.gstr3b;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Gstr3bInterestResult(
        boolean hasDelay,
        LocalDate dueDate,
        long delayDays,
        BigDecimal cgstInterest,
        BigDecimal sgstInterest,
        BigDecimal igstInterest,
        BigDecimal totalInterest,
        boolean requiresManualCashVerification
) {
    public static Gstr3bInterestResult noDelay(LocalDate dueDate) {
        return new Gstr3bInterestResult(
                false, dueDate, 0,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false);
    }
}
