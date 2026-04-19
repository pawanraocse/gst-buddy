package com.learning.backendservice.domain.gstr3b;

import com.learning.backendservice.domain.gstr1.ReliefWindowSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Gstr3bLateFeeCalculatorService")
class Gstr3bLateFeeCalculatorServiceTest {

    private final Gstr3bLateFeeCalculatorService svc = new Gstr3bLateFeeCalculatorService();

    private Gstr3bLateFeeInput input(
            LocalDate filingDate, YearMonth taxPeriod,
            boolean isNilReturn, boolean isQrmp, String stateCode,
            ReliefWindowSnapshot relief) {
        return new Gstr3bLateFeeInput("29XXXXX1234X1ZX", filingDate, taxPeriod,
                "2024-25", isNilReturn, isQrmp, stateCode, relief);
    }

    // ── Due Date Resolution ───────────────────────────────────────────────────

    @Test
    @DisplayName("Monthly filer: due date = 20th of following month")
    void monthlyFilerDueDate() {
        LocalDate due = svc.resolveDueDate(YearMonth.of(2024, 3), false, "29");
        assertEquals(LocalDate.of(2024, 4, 20), due);
    }

    @Test
    @DisplayName("QRMP Category A (29=Karnataka): Q4 Jan-Mar due = 22-Apr")
    void qrmpCategoryADueDate() {
        // Jan-Mar quarter → quarter-end = Mar-2024 → due = 22-Apr-2024
        LocalDate due = svc.resolveDueDate(YearMonth.of(2024, 1), true, "29");
        assertEquals(LocalDate.of(2024, 4, 22), due);
    }

    @Test
    @DisplayName("QRMP Category B (09=UP): Q4 Jan-Mar due = 24-Apr")
    void qrmpCategoryBDueDate() {
        LocalDate due = svc.resolveDueDate(YearMonth.of(2024, 1), true, "09");
        assertEquals(LocalDate.of(2024, 4, 24), due);
    }

    @Test
    @DisplayName("QRMP Q1 (Apr-Jun) → due = 22/24-Jul")
    void qrmpQ1DueDate() {
        LocalDate catA = svc.resolveDueDate(YearMonth.of(2024, 5), true, "27"); // Maharashtra
        assertEquals(LocalDate.of(2024, 7, 22), catA);

        LocalDate catB = svc.resolveDueDate(YearMonth.of(2024, 5), true, "07"); // Delhi
        assertEquals(LocalDate.of(2024, 7, 24), catB);
    }

    // ── Late Fee Computation ──────────────────────────────────────────────────

    @Test
    @DisplayName("Filed on time → zero fee")
    void filedOnTime() {
        // Monthly, Mar-2024, filed on 19-Apr (before due 20-Apr)
        var result = svc.calculate(input(
                LocalDate.of(2024, 4, 19), YearMonth.of(2024, 3),
                false, false, "29", null));
        assertEquals(0, result.delayDays());
        assertEquals(BigDecimal.ZERO, result.totalFee());
    }

    @Test
    @DisplayName("Non-nil, 5 days late → ₹125 CGST + ₹125 SGST = ₹250")
    void nonNilFiveDaysLate() {
        // Monthly, Mar-2024, due 20-Apr, filed 25-Apr = 5 days late
        var result = svc.calculate(input(
                LocalDate.of(2024, 4, 25), YearMonth.of(2024, 3),
                false, false, "29", null));
        assertEquals(5, result.delayDays());
        assertEquals(new BigDecimal("125.00"), result.cgstFee()); // 25 × 5
        assertEquals(new BigDecimal("125.00"), result.sgstFee());
        assertEquals(new BigDecimal("250.00"), result.totalFee());
    }

    @Test
    @DisplayName("Nil return, 10 days late → ₹100 CGST + ₹100 SGST = ₹200")
    void nilReturnTenDaysLate() {
        var result = svc.calculate(input(
                LocalDate.of(2024, 4, 30), YearMonth.of(2024, 3),
                true, false, "29", null));
        assertEquals(10, result.delayDays());
        assertEquals(new BigDecimal("100.00"), result.cgstFee()); // 10 × 10
        assertEquals(new BigDecimal("100.00"), result.sgstFee());
    }

    @Test
    @DisplayName("Non-nil, 300 days late → capped at ₹5,000 CGST + ₹5,000 SGST")
    void nonNilCapApplied() {
        var result = svc.calculate(input(
                LocalDate.of(2025, 3, 16), YearMonth.of(2024, 3),
                false, false, "29", null));
        assertTrue(result.delayDays() >= 200);
        assertEquals(new BigDecimal("5000.00"), result.cgstFee());
        assertEquals(new BigDecimal("5000.00"), result.sgstFee());
        assertEquals(new BigDecimal("10000.00"), result.totalFee());
    }

    @Test
    @DisplayName("Nil return, 300 days late → capped at ₹250 CGST + ₹250 SGST")
    void nilReturnCapApplied() {
        var result = svc.calculate(input(
                LocalDate.of(2025, 3, 16), YearMonth.of(2024, 3),
                true, false, "29", null));
        assertEquals(new BigDecimal("250.00"), result.cgstFee());
        assertEquals(new BigDecimal("250.00"), result.sgstFee());
        assertEquals(new BigDecimal("500.00"), result.totalFee());
    }

    @Test
    @DisplayName("Relief window applied — reduces to zero fee")
    void reliefWindowApplied() {
        ReliefWindowSnapshot relief = ReliefWindowSnapshot.waiver("Notification 08/2023-CT");
        var result = svc.calculate(input(
                LocalDate.of(2024, 4, 30), YearMonth.of(2024, 3),
                false, false, "29", relief));
        assertTrue(result.reliefApplied());
        assertEquals("Notification 08/2023-CT", result.appliedNotification());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.cgstFee()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.sgstFee()));
    }

    @Test
    @DisplayName("QRMP Category A state — uses 22nd due date")
    void qrmpCatALateComputation() {
        // Q4 (Jan-Mar 2024), due 22-Apr-2024, Category A (Karnataka=29)
        // Filed 30-Apr = 8 days late
        var result = svc.calculate(input(
                LocalDate.of(2024, 4, 30), YearMonth.of(2024, 1),
                false, true, "29", null));
        assertEquals(8, result.delayDays()); // 30-Apr minus 22-Apr
        assertEquals(new BigDecimal("200.00"), result.cgstFee()); // 25 × 8
    }

    @Test
    @DisplayName("QRMP Category B state — uses 24th due date")
    void qrmpCatBLateComputation() {
        // Q4 (Jan-Mar 2024), due 24-Apr-2024, Category B (Delhi=07)
        // Filed 30-Apr = 6 days late
        var result = svc.calculate(input(
                LocalDate.of(2024, 4, 30), YearMonth.of(2024, 1),
                false, true, "07", null));
        assertEquals(6, result.delayDays()); // 30-Apr minus 24-Apr
        assertEquals(new BigDecimal("150.00"), result.cgstFee()); // 25 × 6
    }
}
