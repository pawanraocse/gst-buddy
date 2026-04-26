package com.learning.backendservice.domain.gstr1;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LateReportingGstr1CalculatorService}.
 *
 * <p>All interest calculations verified manually using:
 *   interest = taxAmount × 18% × delayDays / 365
 *
 * <p>Legal basis: Section 50(1), CGST Act 2017 + Notification 63/2020-CT.
 *
 * <p>Example verification (Test 2):
 *   Invoice date: 01-Mar-2024 → expected period: Mar-2024 → due 11-Apr-2024
 *   Filed in: Apr-2024 → declared due 11-May-2024
 *   Delay = 30 days (11-Apr to 11-May)
 *   Tax = ₹1,800 (CGST ₹900 + SGST ₹900)
 *   Interest = 1800 × 0.18 × 30 / 365 = ₹26.63 (HALF_UP)
 */
class LateReportingGstr1CalculatorServiceTest {

    private final LateReportingGstr1CalculatorService service =
            new LateReportingGstr1CalculatorService();

    // ── Test 1: All invoices on time — clean result ──────────────────────────

    @Test
    @DisplayName("All invoices in correct period → no belated invoices, zero interest")
    void allOnTime_cleanResult() {
        // Invoice from Apr-2024, filed in Apr-2024 → on time
        InvoiceRow inv = invoice("INV-001", LocalDate.of(2024, 4, 15),
                bd("10000"), bd("900"), bd("900"), bd("0"), bd("0"), bd("18"));

        LateReportingGstr1Input input = input(YearMonth.of(2024, 4), false, List.of(inv));
        LateReportingGstr1Result result = service.calculate(input);

        assertThat(result.totalBelated()).isZero();
        assertThat(result.totalInterest()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.belatedInvoices()).isEmpty();
    }

    // ── Test 2: One invoice belated by 1 month (30 days) ────────────────────

    @Test
    @DisplayName("Invoice from Mar-2024 declared in Apr-2024 → 30 days late, ₹26.63 interest")
    void oneInvoice_belatedByOneMonth() {
        // Mar-2024 due: 11-Apr-2024; Apr-2024 due: 11-May-2024 → 30 days delay
        // tax = 1800; interest = 1800 × 0.18 × 30 / 365 = 26.630... → 26.63
        InvoiceRow inv = invoice("INV-002", LocalDate.of(2024, 3, 1),
                bd("10000"), bd("900"), bd("900"), bd("0"), bd("0"), bd("18"));

        LateReportingGstr1Input input = input(YearMonth.of(2024, 4), false, List.of(inv));
        LateReportingGstr1Result result = service.calculate(input);

        assertThat(result.totalBelated()).isEqualTo(1);
        BelatedInvoice bi = result.belatedInvoices().get(0);
        assertThat(bi.delayDays()).isEqualTo(30);
        assertThat(bi.taxAmount()).isEqualByComparingTo(bd("1800.00"));
        assertThat(bi.interestAmount()).isEqualByComparingTo(bd("26.63"));
        assertThat(result.totalInterest()).isEqualByComparingTo(bd("26.63"));
    }

    // ── Test 3: Invoice belated by 2 months (61 days) ────────────────────────

    @Test
    @DisplayName("Invoice from Feb-2024 declared in Apr-2024 → 61 days, ₹54.15 interest")
    void oneInvoice_belatedByTwoMonths() {
        // Feb-2024 due: 11-Mar-2024; Apr-2024 due: 11-May-2024 → 61 days
        // tax = 1800; interest = 1800 × 0.18 × 61 / 365 = 324 × 61 / 365 = 19764/365 = 54.1479... → 54.15 (HALF_UP)
        InvoiceRow inv = invoice("INV-003", LocalDate.of(2024, 2, 10),
                bd("10000"), bd("900"), bd("900"), bd("0"), bd("0"), bd("18"));

        LateReportingGstr1Input input = input(YearMonth.of(2024, 4), false, List.of(inv));
        LateReportingGstr1Result result = service.calculate(input);

        BelatedInvoice bi = result.belatedInvoices().get(0);
        assertThat(bi.delayDays()).isEqualTo(61);
        assertThat(bi.interestAmount()).isEqualByComparingTo(bd("54.15"));
    }

    // ── Test 4: Mixed batch — 2 belated, 1 on time ───────────────────────────

    @Test
    @DisplayName("Mixed batch: 2 belated + 1 on time → only belated invoices have interest")
    void mixedBatch_onlyBelatedHaveInterest() {
        InvoiceRow onTime   = invoice("INV-100", LocalDate.of(2024, 4, 5),
                bd("5000"), bd("450"), bd("450"), bd("0"), bd("0"), bd("18"));
        InvoiceRow belated1 = invoice("INV-101", LocalDate.of(2024, 3, 10),
                bd("10000"), bd("900"), bd("900"), bd("0"), bd("0"), bd("18")); // 30 days
        InvoiceRow belated2 = invoice("INV-102", LocalDate.of(2024, 2, 20),
                bd("20000"), bd("1800"), bd("1800"), bd("0"), bd("0"), bd("18")); // 61 days

        LateReportingGstr1Input input = input(
                YearMonth.of(2024, 4), false, List.of(onTime, belated1, belated2));
        LateReportingGstr1Result result = service.calculate(input);

        assertThat(result.totalBelated()).isEqualTo(2);
        // INV-101: tax=1800, 1800 × 0.18 × 30 / 365 = 26.63
        // INV-102: tax=3600, 3600 × 0.18 × 61 / 365 = 108.30 (HALF_UP on 19959.12/365=54.6826...×2=108.30)
        // total = 26.63 + 108.30 = 134.93
        assertThat(result.totalInterest()).isEqualByComparingTo(bd("134.93"));
        assertThat(result.totalTaxAtRisk()).isEqualByComparingTo(bd("5400.00"));
    }

    // ── Test 5: Empty invoice list — clean result ─────────────────────────────

    @Test
    @DisplayName("Empty invoice list → clean result")
    void emptyInvoiceList_cleanResult() {
        LateReportingGstr1Input input = input(YearMonth.of(2024, 4), false, List.of());
        LateReportingGstr1Result result = service.calculate(input);

        assertThat(result.totalBelated()).isZero();
        assertThat(result.totalInterest()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── Test 6: QRMP filer — quarterly due dates ──────────────────────────────

    @Test
    @DisplayName("QRMP filer: invoice from Jan-2024 declared in Q4 (Jan-Mar) → same quarter, on time")
    void qrmpFiler_sameQuarterInvoice_onTime() {
        // Q4: Jan-Mar-2024, QRMP due: 13-Apr-2024
        // Invoice from Jan, declared in GSTR-1 for Mar-2024 quarter → on time
        InvoiceRow inv = invoice("INV-200", LocalDate.of(2024, 1, 15),
                bd("10000"), bd("900"), bd("900"), bd("0"), bd("0"), bd("18"));

        LateReportingGstr1Input input = input(YearMonth.of(2024, 3), true, List.of(inv));
        LateReportingGstr1Result result = service.calculate(input);

        assertThat(result.totalBelated()).isZero();
    }

    // ── Test 7: QRMP filer — cross quarter belated ────────────────────────────

    @Test
    @DisplayName("QRMP filer: invoice from Oct-2024 declared in Q4 (Jan-Mar 2025) → cross quarter belated")
    void qrmpFiler_crossQuarterInvoice_belated() {
        // Oct-2024 invoice (Oct-Dec quarter) → due 13-Jan-2025
        // Declared in Mar-2025 (Jan-Mar quarter) → due 13-Apr-2025
        // Delay: 13-Jan-2025 to 13-Apr-2025 = 90 days
        // Tax: ₹1800. Interest: 1800 × 0.18 × 90 / 365 = 79.89
        InvoiceRow inv = invoice("INV-201", LocalDate.of(2024, 10, 15),
                bd("10000"), bd("900"), bd("900"), bd("0"), bd("0"), bd("18"));

        LateReportingGstr1Input input = input(YearMonth.of(2025, 3), true, List.of(inv));
        LateReportingGstr1Result result = service.calculate(input);

        assertThat(result.totalBelated()).isEqualTo(1);
        BelatedInvoice bi = result.belatedInvoices().get(0);
        assertThat(bi.delayDays()).isEqualTo(90);
        assertThat(bi.taxAmount()).isEqualByComparingTo(bd("1800.00"));
        assertThat(bi.interestAmount()).isEqualByComparingTo(bd("79.89"));
        assertThat(result.totalInterest()).isEqualByComparingTo(bd("79.89"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private InvoiceRow invoice(String no, LocalDate date, BigDecimal taxable,
                               BigDecimal cgst, BigDecimal sgst, BigDecimal igst,
                               BigDecimal cess, BigDecimal rate) {
        return new InvoiceRow(no, date, "29-Karnataka", taxable, cgst, sgst, igst, cess, rate);
    }

    private LateReportingGstr1Input input(YearMonth period, boolean isQrmp,
                                          List<InvoiceRow> invoices) {
        return new LateReportingGstr1Input("07ASXPD9282E1Z8", period, "2024-25", isQrmp, invoices);
    }

    private BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
