package com.learning.backendservice.domain.rule86b;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Rule86bCalculationService")
class Rule86bCalculationServiceTest {

    private final Rule86bCalculationService service = new Rule86bCalculationService();

    private Rule86bInput input(String taxable, String payable, String rcm, String cash, boolean gov, boolean exp, String dateStr) {
        return new Rule86bInput(
                "29AABCU9603R1ZM",
                LocalDate.parse(dateStr),
                new BigDecimal(taxable),
                new BigDecimal(payable),
                new BigDecimal(rcm),
                new BigDecimal(cash),
                gov,
                exp,
                Rule86bConfigSnapshot.defaults()
        );
    }

    @Test
    @DisplayName("should not apply if below threshold")
    void testBelowThreshold() {
        var res = service.evaluate(input("4000000.00", "720000.00", "0", "0", false, false, "2024-04-01"));
        assertThat(res.isApplicable()).isFalse();
    }

    @Test
    @DisplayName("should not apply if before effective date")
    void testBeforeEffectiveDate() {
        var res = service.evaluate(input("6000000.00", "1080000.00", "0", "0", false, false, "2020-12-01"));
        assertThat(res.isApplicable()).isFalse();
    }

    @Test
    @DisplayName("should flag breached if cash is below 1%")
    void testBreach() {
        var res = service.evaluate(input("6000000.00", "1080000.00", "0", "0", false, false, "2024-04-01"));
        assertThat(res.isApplicable()).isTrue();
        assertThat(res.isBreached()).isTrue();
        assertThat(res.cashShortfall()).isEqualByComparingTo("10800.00");
    }

    @Test
    @DisplayName("should pass if cash is at least 1%")
    void testCompliant() {
        var res = service.evaluate(input("6000000.00", "1080000.00", "0", "11000.00", false, false, "2024-04-01"));
        assertThat(res.isApplicable()).isTrue();
        assertThat(res.isBreached()).isFalse();
    }

    @Test
    @DisplayName("should properly deduct RCM liability and cash")
    void testRcmDeduction() {
        // Output tax = 10L, RCM = 2L -> Total payable = 12L
        // Output tax cash = 0, RCM cash = 2L -> Total cash = 2L
        // If we don't deduct RCM, cash % = 2L / 12L = 16.6% -> Pass
        // If we correctly deduct RCM, output cash = 0, output tax = 10L -> 0% -> Breach
        var res = service.evaluate(input("6000000.00", "1200000.00", "200000.00", "200000.00", false, false, "2024-04-01"));
        assertThat(res.isApplicable()).isTrue();
        assertThat(res.isBreached()).isTrue();
        assertThat(res.requiredCashPayment()).isEqualByComparingTo("10000.00");
    }

    @Test
    @DisplayName("should handle case where paidInCash < rcmTaxPayable safely")
    void testRcmDeduction_InsufficientCash() {
        // Total Payable = 12L, RCM = 2L. Paid in cash = 1L (underpaid RCM)
        // outputPaidInCash should be max(0, 1L - 2L) = 0
        var res = service.evaluate(input("6000000.00", "1200000.00", "200000.00", "100000.00", false, false, "2024-04-01"));
        assertThat(res.isApplicable()).isTrue();
        assertThat(res.isBreached()).isTrue();
        assertThat(res.outputPaidInCash()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("should apply exemption for Govt PSU")
    void testGovtPsuExemption() {
        var res = service.evaluate(input("6000000.00", "1080000.00", "0", "0", true, false, "2024-04-01"));
        assertThat(res.isApplicable()).isTrue();
        assertThat(res.isBreached()).isFalse();
        assertThat(res.hasExemption()).isTrue();
        assertThat(res.exemptionReason()).contains("Govt/PSU");
    }

    @Test
    @DisplayName("should apply exemption for Export")
    void testExportExemption() {
        var res = service.evaluate(input("6000000.00", "1080000.00", "0", "0", false, true, "2024-04-01"));
        assertThat(res.isApplicable()).isTrue();
        assertThat(res.isBreached()).isFalse();
        assertThat(res.hasExemption()).isTrue();
        assertThat(res.exemptionReason()).contains("Export");
    }

    @Test
    @DisplayName("should guard against division by zero if outputTaxPayable is zero")
    void testDivisionGuard_ZeroOutputTax() {
        // Total payable is 10L, but RCM is also 10L. Output tax = 0.
        // Should immediately return notApplicable and prevent division by zero.
        var res = service.evaluate(input("6000000.00", "1000000.00", "1000000.00", "1000000.00", false, false, "2024-04-01"));
        assertThat(res.isApplicable()).isFalse();
    }
}
