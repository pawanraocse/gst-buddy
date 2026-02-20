package com.learning.backendservice.domain.rule37;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Value object representing a Rule 37 interest calculation row.
 * Enhanced for production with risk categorization and deadline tracking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestRow {

    private String supplier;
    private LocalDate purchaseDate;
    private LocalDate paymentDate; // null for UNPAID
    private double principal;
    private int delayDays;
    private double itcAmount;
    private double interest;
    private InterestStatus status;

    // Production enhancements
    private LocalDate paymentDeadline; // purchaseDate + 180 days
    private RiskCategory riskCategory; // SAFE, AT_RISK, BREACHED
    private String gstr3bPeriod; // Return period for reversal (e.g., "Jan 2025")
    private int daysToDeadline; // Days until/since 180-day deadline (negative = breached)

    // GSTR-3B readiness: ITC availment date for accurate interest computation
    // When null, interest is computed from invoice date (current default)
    private LocalDate itcAvailmentDate;

    public enum InterestStatus {
        PAID_LATE,
        UNPAID
    }

    /**
     * Risk categorization for proactive compliance management.
     */
    public enum RiskCategory {
        /** Payment within 150 days - safe zone */
        SAFE,
        /** Payment between 151-180 days - approaching deadline */
        AT_RISK,
        /** Payment beyond 180 days - ITC reversal required */
        BREACHED
    }
}
