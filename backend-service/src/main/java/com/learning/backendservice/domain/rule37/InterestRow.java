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
    private LocalDate purchaseDate;
    private LocalDate paymentDate;
    private BigDecimal principal;
    private int delayDays;
    private BigDecimal itcAmount;
    private BigDecimal interest;
    private InterestStatus status;

    private LocalDate paymentDeadline;
    private RiskCategory riskCategory;
    private String gstr3bPeriod;
    private int daysToDeadline;

    private LocalDate itcAvailmentDate;

    public enum InterestStatus {
        PAID_LATE,
        UNPAID
    }

    public enum RiskCategory {
        SAFE,
        AT_RISK,
        BREACHED
    }
}
