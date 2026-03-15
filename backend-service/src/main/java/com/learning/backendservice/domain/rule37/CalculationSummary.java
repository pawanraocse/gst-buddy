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

    public static final String DISCLAIMER = "Estimated calculations based on uploaded ledger data. "
            + "ITC reversal estimates assume 18% GST rate on tax-inclusive amounts. "
            + "Interest estimates use 18% p.a. as per Section 50 of the CGST Act with assumed availment dates. "
            + "Actual liability depends on ITC availment and utilization dates. "
            + "Consult a qualified CA/tax professional before filing GSTR-3B.";
}
