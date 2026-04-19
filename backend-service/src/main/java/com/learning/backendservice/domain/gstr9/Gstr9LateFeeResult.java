package com.learning.backendservice.domain.gstr9;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Gstr9LateFeeResult(
        LocalDate dueDate,
        LocalDate filingDate,
        int delayDays,
        BigDecimal cgstFee,
        BigDecimal sgstFee,
        BigDecimal totalFee,
        boolean isExempt,
        String amnestyApplied,
        boolean capAssumedFromAggregateTurnover
) {
    public static Gstr9LateFeeResult onTime(LocalDate dueDate, LocalDate filingDate) {
        return new Gstr9LateFeeResult(dueDate, filingDate, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, null, false);
    }
    
    public static Gstr9LateFeeResult exempt(LocalDate dueDate, LocalDate filingDate) {
        return new Gstr9LateFeeResult(dueDate, filingDate, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true, null, false);
    }
}
