package com.learning.backendservice.domain.gstr1;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Gstr1LateFeeCalculatorService}.
 *
 * <p>All test scenarios are derived from the 10-row matrix in the implementation plan.
 * Every monetary assertion uses {@code compareTo()} — never {@code equals()} —
 * to avoid BigDecimal scale sensitivity per the java-backend skill.
 *
 * <p><b>Legal basis under test:</b> Section 47(1), CGST Act 2017.
 */
@DisplayName("Gstr1LateFeeCalculatorService")
class Gstr1LateFeeCalculatorServiceTest {

    private final Gstr1LateFeeCalculatorService sut = new Gstr1LateFeeCalculatorService();

    // ── Due Date Resolution ───────────────────────────────────────────────────

    @Test
    @DisplayName("Monthly filer: due date is 11th of the following month")
    void monthlyDueDate() {
        LocalDate due = sut.resolveDueDate(YearMonth.of(2024, 3), false);
        assertThat(due).isEqualTo(LocalDate.of(2024, 4, 11));
    }

    @Test
    @DisplayName("QRMP filer: due date is 13th of month after quarter-end (Q4 = Jan-Mar)")
    void qrmpDueDateQ4() {
        LocalDate due = sut.resolveDueDate(YearMonth.of(2024, 3), true);
        assertThat(due).isEqualTo(LocalDate.of(2024, 4, 13));
    }

    @Test
    @DisplayName("QRMP filer: due date is 13th of month after quarter-end (Q2 = Jul-Sep)")
    void qrmpDueDateQ2() {
        LocalDate due = sut.resolveDueDate(YearMonth.of(2024, 9), true);
        assertThat(due).isEqualTo(LocalDate.of(2024, 10, 13));
    }

    // ── Main Calculation Matrix ───────────────────────────────────────────────

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("Late fee calculation matrix — Section 47(1) CGST Act 2017")
    @CsvSource(delimiter = '|', textBlock = """
        # scenario               | period   | qrmp  | nil   | arnDate    | expDelay | expCgst | expSgst
        Filed on exact due date  | 2024-03  | false | false | 2024-04-11 | 0        | 0.00    | 0.00
        Filed 1 day late         | 2024-03  | false | false | 2024-04-12 | 1        | 25.00   | 25.00
        Filed 4 days late        | 2024-03  | false | false | 2024-04-15 | 4        | 100.00  | 100.00
        QRMP: filed on 13th      | 2024-03  | true  | false | 2024-04-13 | 0        | 0.00    | 0.00
        QRMP: filed 2 days late  | 2024-03  | true  | false | 2024-04-15 | 2        | 50.00   | 50.00
        Nil: cap hit (₹250 each) | 2024-03  | false | true  | 2024-07-20 | 100      | 250.00  | 250.00
        Normal: cap hit (₹5000)  | 2024-03  | false | false | 2025-04-11 | 365      | 5000.00 | 5000.00
        Filed 1 day early        | 2024-03  | false | false | 2024-04-10 | 0        | 0.00    | 0.00
        Nil: below cap           | 2024-03  | false | true  | 2024-04-16 | 5        | 50.00   | 50.00
        FY rollover: filed late  | 2024-03  | false | false | 2025-04-16 | 370      | 5000.00 | 5000.00
        """)
    void lateFeeMatrix(
            String scenario,
            String periodStr,
            boolean isQrmp,
            boolean isNilReturn,
            String arnDateStr,
            int expectedDelay,
            String expectedCgst,
            String expectedSgst) {

        YearMonth taxPeriod = YearMonth.parse(periodStr);
        LocalDate arnDate   = LocalDate.parse(arnDateStr);

        Gstr1LateFeeInput input = new Gstr1LateFeeInput(
                "29XXXXX1234X1ZX", arnDate, taxPeriod,
                "2024-25", isNilReturn, isQrmp, null
        );

        Gstr1LateFeeResult result = sut.calculate(input);

        assertThat(result.delayDays()).as("delayDays — " + scenario).isEqualTo(expectedDelay);
        assertThat(result.cgstFee().compareTo(new BigDecimal(expectedCgst)))
                .as("cgstFee — " + scenario).isZero();
        assertThat(result.sgstFee().compareTo(new BigDecimal(expectedSgst)))
                .as("sgstFee — " + scenario).isZero();
    }

    // ── Relief Window Scenarios ───────────────────────────────────────────────

    @Test
    @DisplayName("Relief window: full waiver overrides normal rates")
    void reliefWindowFullWaiver() {
        ReliefWindowSnapshot relief = ReliefWindowSnapshot.waiver("Notification No. 19/2021-CT");
        Gstr1LateFeeInput input = new Gstr1LateFeeInput(
                "29XXXXX1234X1ZX",
                LocalDate.of(2021, 7, 15),
                YearMonth.of(2021, 3),
                "2020-21", false, false, relief
        );

        Gstr1LateFeeResult result = sut.calculate(input);

        assertThat(result.delayDays()).isGreaterThan(0);
        assertThat(result.cgstFee().compareTo(BigDecimal.ZERO)).isZero();
        assertThat(result.sgstFee().compareTo(BigDecimal.ZERO)).isZero();
        assertThat(result.reliefApplied()).isTrue();
        assertThat(result.appliedNotification()).contains("19/2021");
    }

    @Test
    @DisplayName("Relief window: reduced daily rate with cap")
    void reliefWindowReducedRate() {
        ReliefWindowSnapshot relief = new ReliefWindowSnapshot(
                "Notification No. 20/2021-CT",
                new BigDecimal("25.00"), new BigDecimal("25.00"),
                new BigDecimal("500.00"), new BigDecimal("500.00")
        );
        Gstr1LateFeeInput input = new Gstr1LateFeeInput(
                "29XXXXX1234X1ZX",
                LocalDate.of(2021, 8, 31),   // 142 days late from 11-Apr-2020
                YearMonth.of(2020, 3),
                "2019-20", false, false, relief
        );

        Gstr1LateFeeResult result = sut.calculate(input);

        // 25 × 142 = 3,550 → capped at 500
        assertThat(result.cgstFee().compareTo(new BigDecimal("500.00"))).isZero();
        assertThat(result.sgstFee().compareTo(new BigDecimal("500.00"))).isZero();
        assertThat(result.totalFee().compareTo(new BigDecimal("1000.00"))).isZero();
        assertThat(result.reliefApplied()).isTrue();
    }

    // ── totalFee Consistency ──────────────────────────────────────────────────

    @Test
    @DisplayName("totalFee always equals cgstFee + sgstFee")
    void totalFeeConsistency() {
        Gstr1LateFeeInput input = new Gstr1LateFeeInput(
                "29XXXXX1234X1ZX",
                LocalDate.of(2024, 4, 21),
                YearMonth.of(2024, 3),
                "2024-25", false, false, null
        );
        Gstr1LateFeeResult result = sut.calculate(input);

        assertThat(result.totalFee().compareTo(result.cgstFee().add(result.sgstFee()))).isZero();
    }
}
