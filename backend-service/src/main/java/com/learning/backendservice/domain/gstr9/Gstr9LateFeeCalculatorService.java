package com.learning.backendservice.domain.gstr9;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Core business logic for computing GSTR-9/9C late fees per Section 47(2), CGST Act 2017.
 * Pure domain service without Spring dependencies.
 */
public class Gstr9LateFeeCalculatorService {

    private static final BigDecimal DAILY_CGST = new BigDecimal("100.00");
    private static final BigDecimal DAILY_SGST = new BigDecimal("100.00");
    private static final BigDecimal CAP_MULTIPLIER = new BigDecimal("0.0025"); // 0.25%
    private static final BigDecimal EXEMPT_TURNOVER_THRESHOLD = new BigDecimal("20000000.00"); // 2 Cr
    
    // Amnesty Notification 07/2023-CT bounds
    private static final Set<String> AMNESTY_FYS = Set.of("2017-18", "2018-19", "2019-20", "2020-21", "2021-22");
    private static final BigDecimal AMNESTY_CAP_CGST = new BigDecimal("10000.00");
    private static final BigDecimal AMNESTY_CAP_SGST = new BigDecimal("10000.00");

    public Gstr9LateFeeResult calculate(Gstr9LateFeeInput input) {
        LocalDate dueDate = resolveDueDate(input.financialYear());

        // Check Exemption
        if (input.aggregateTurnover() != null && input.aggregateTurnover().compareTo(EXEMPT_TURNOVER_THRESHOLD) <= 0) {
            return Gstr9LateFeeResult.exempt(dueDate, input.filingDate());
        }

        long delayDays = Math.max(0L, ChronoUnit.DAYS.between(dueDate, input.filingDate()));

        if (delayDays == 0) {
            return Gstr9LateFeeResult.onTime(dueDate, input.filingDate());
        }

        BigDecimal days = new BigDecimal(delayDays);
        BigDecimal cgstFee = DAILY_CGST.multiply(days).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgstFee = DAILY_SGST.multiply(days).setScale(2, RoundingMode.HALF_UP);

        BigDecimal capCgst = null;
        BigDecimal capSgst = null;
        String amnestyApplied = null;
        boolean capAssumedFromAggregateTurnover = false;

        if (AMNESTY_FYS.contains(input.financialYear())) {
            capCgst = AMNESTY_CAP_CGST;
            capSgst = AMNESTY_CAP_SGST;
            amnestyApplied = "Notification 07/2023-CT";
        } else if (input.aggregateTurnover() != null) {
            // Cap is 0.25% of turnover in state. 
            // We approximate using aggregate turnover, and flag the approximation.
            capCgst = input.aggregateTurnover().multiply(CAP_MULTIPLIER).setScale(2, RoundingMode.HALF_UP);
            capSgst = capCgst;
            capAssumedFromAggregateTurnover = true;
        }

        if (capCgst != null) {
            cgstFee = cgstFee.min(capCgst);
            sgstFee = sgstFee.min(capSgst);
        }

        BigDecimal totalFee = cgstFee.add(sgstFee);

        return new Gstr9LateFeeResult(
                dueDate, input.filingDate(), (int) delayDays,
                cgstFee, sgstFee, totalFee,
                false, amnestyApplied, capAssumedFromAggregateTurnover);
    }

    private LocalDate resolveDueDate(String financialYear) {
        // e.g. "2023-24" -> "24" -> "2024"
        String[] parts = financialYear.split("-");
        int endYearStr = Integer.parseInt(parts[0].substring(0, 2) + parts[1]);
        return LocalDate.of(endYearStr, 12, 31);
    }
}
