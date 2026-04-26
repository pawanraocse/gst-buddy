package com.learning.backendservice.domain.rule86b;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Collections;

/**
 * Domain service to evaluate Rule 86B (1% Cash Ledger restriction).
 */
public class Rule86bCalculationService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public Rule86bResult evaluate(Rule86bInput input) {
        if (input.period().isBefore(input.config().effectiveFrom())) {
            return notApplicable();
        }

        if (input.monthlyTaxableOutward().compareTo(input.config().turnoverThreshold()) <= 0) {
            return notApplicable();
        }

        BigDecimal outputTaxPayable = input.totalTaxPayable().subtract(input.rcmTaxPayable());
        if (outputTaxPayable.compareTo(BigDecimal.ZERO) <= 0) {
            return notApplicable();
        }

        BigDecimal outputPaidInCash = input.paidInCash().subtract(input.rcmTaxPayable()).max(BigDecimal.ZERO);

        BigDecimal requiredCashPayment = outputTaxPayable.multiply(input.config().cashPercentFloor())
                .setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal actualCashPercent = outputPaidInCash.divide(outputTaxPayable, 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED);

        boolean isBreached = outputPaidInCash.compareTo(requiredCashPayment) < 0;
        BigDecimal cashShortfall = isBreached ? requiredCashPayment.subtract(outputPaidInCash) : BigDecimal.ZERO;

        if (isBreached) {
            if (input.hasGovtPsuFormat()) {
                return exempt(outputTaxPayable, outputPaidInCash, requiredCashPayment, actualCashPercent, "Govt/PSU/Local Authority Entity");
            }
            if (input.hasExportInvoices()) {
                return exempt(outputTaxPayable, outputPaidInCash, requiredCashPayment, actualCashPercent, "Zero-rated supplies / Export with refund claim");
            }
        }

        List<String> advisories = Collections.emptyList();
        if (isBreached) {
            advisories = List.of(
                "Verify whether manager/MD has received IT refund ≥ ₹1 lakh in preceding 2 FYs — if yes, Rule 86B exemption applies.",
                "Verify if cumulative cash payments in the current FY are > 1% of total output tax.",
                "Verify if the taxpayer paid ≥ 1% GST through cash in the preceding FY."
            );
        }

        return new Rule86bResult(
                true,
                isBreached,
                false,
                null,
                outputTaxPayable,
                outputPaidInCash,
                requiredCashPayment,
                cashShortfall,
                actualCashPercent,
                advisories
        );
    }

    private Rule86bResult notApplicable() {
        return new Rule86bResult(
                false, 
                false, 
                false, 
                null, 
                BigDecimal.ZERO, 
                BigDecimal.ZERO, 
                BigDecimal.ZERO, 
                BigDecimal.ZERO, 
                BigDecimal.ZERO, 
                Collections.emptyList()
        );
    }

    private Rule86bResult exempt(BigDecimal taxPayable, BigDecimal paidInCash, BigDecimal required, BigDecimal percent, String reason) {
        return new Rule86bResult(true, false, true, reason, taxPayable, paidInCash, required, BigDecimal.ZERO, percent, Collections.emptyList());
    }
}
