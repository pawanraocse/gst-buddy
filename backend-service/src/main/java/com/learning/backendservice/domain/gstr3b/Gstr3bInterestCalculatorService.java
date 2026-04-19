package com.learning.backendservice.domain.gstr3b;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Calculator for GSTR-3B delayed filing interest.
 *
 * <p><b>Legal basis:</b> Section 50(1), CGST Act 2017.
 * <p><b>Rate:</b> 18% per annum simple interest.
 * <p><b>Base:</b> Net cash tax liability (Tax paid in cash) — Table 6.1 of GSTR-3B.
 */
@Component
public class Gstr3bInterestCalculatorService {

    private static final BigDecimal INTEREST_RATE_PA = new BigDecimal("0.18");
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    private final Gstr3bLateFeeCalculatorService lateFeeCalculator;

    public Gstr3bInterestCalculatorService(Gstr3bLateFeeCalculatorService lateFeeCalculator) {
        this.lateFeeCalculator = lateFeeCalculator;
    }

    public Gstr3bInterestResult calculate(Gstr3bInterestInput input) {
        // Reuse the due date resolution from late fee service, as Sec 50(1) uses Sec 39 due dates.
        LocalDate dueDate = lateFeeCalculator.resolveDueDate(input.taxPeriod(), input.isQrmp(), input.stateCode());

        long delayDays = Math.max(0L, ChronoUnit.DAYS.between(dueDate, input.filingDate()));

        if (delayDays == 0) {
            return Gstr3bInterestResult.noDelay(dueDate);
        }

        // If parser couldn't extract cash paid, we flag for manual verification and set 0
        if (input.cgstCashPaid() == null && input.sgstCashPaid() == null && input.igstCashPaid() == null) {
            return new Gstr3bInterestResult(
                    true, dueDate, delayDays,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    true
            );
        }

        BigDecimal cgstInterest = calculateInterestForHead(input.cgstCashPaid(), delayDays);
        BigDecimal sgstInterest = calculateInterestForHead(input.sgstCashPaid(), delayDays);
        BigDecimal igstInterest = calculateInterestForHead(input.igstCashPaid(), delayDays);

        BigDecimal totalInterest = cgstInterest.add(sgstInterest).add(igstInterest);

        return new Gstr3bInterestResult(
                true, dueDate, delayDays,
                cgstInterest, sgstInterest, igstInterest, totalInterest,
                false
        );
    }

    private BigDecimal calculateInterestForHead(BigDecimal cashPaid, long delayDays) {
        if (cashPaid == null || cashPaid.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal days = new BigDecimal(delayDays);
        return cashPaid
                .multiply(INTEREST_RATE_PA)
                .multiply(days)
                .divide(DAYS_IN_YEAR, 2, RoundingMode.HALF_UP);
    }
}
