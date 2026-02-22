package com.learning.backendservice.domain.rule37;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Value object representing Rule 37 calculation result for a single ledger.
 * Uses BigDecimal for all financial aggregates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculationSummary {

    private BigDecimal totalInterest;
    private BigDecimal totalItcReversal;
    private List<InterestRow> details;

    private int atRiskCount;
    private BigDecimal atRiskAmount;
    private int breachedCount;
    private LocalDate calculationDate;

    public static final String DISCLAIMER = "Interest calculated from invoice date. Per Section 50 + Rule 88B, actual interest "
            + "depends on ITC availment and utilization dates. Consult CA for precise liability.";
}
