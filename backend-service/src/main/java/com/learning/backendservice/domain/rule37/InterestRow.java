package com.learning.backendservice.domain.rule37;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Value object representing a Rule 37 interest calculation row.
 * Uses BigDecimal for all financial fields to prevent floating-point rounding errors.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestRow {

    private String supplier;
    private String invoiceNumber;
    private LocalDate purchaseDate;
    private LocalDate paymentDate;
    @Builder.Default
    private BigDecimal originalInvoiceValue = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal principal = BigDecimal.ZERO;
    private int delayDays;
    @Builder.Default
    private BigDecimal itcAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal interest = BigDecimal.ZERO;
    private InterestStatus status;

    private LocalDate paymentDeadline;
    private RiskCategory riskCategory;
    private String gstr3bPeriod;
    private int daysToDeadline;

    private LocalDate itcAvailmentDate;

    public enum InterestStatus {
        PAID_LATE,
        PAID_ON_TIME,
        UNPAID
    }

    public enum RiskCategory {
        SAFE,
        AT_RISK,
        BREACHED
    }
}
