package com.learning.backendservice.domain.recon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Gstr1Vs3bReconciliationService}.
 *
 * <p>Test data verified against GST law:
 * - Tolerance: ₹1.00 / 0.01% (DEFAULT config)
 * - Severity thresholds: MATCH (<tolerance), MINOR (<5%), MATERIAL (5-20%), CRITICAL (≥20%)
 */
class Gstr1Vs3bReconciliationServiceTest {

    private final Gstr1Vs3bReconciliationService service = new Gstr1Vs3bReconciliationService();

    private static final BigDecimal TOLERANCE_AMT = new BigDecimal("1.00");
    private static final BigDecimal TOLERANCE_PCT = new BigDecimal("0.0001");
    private static final YearMonth  PERIOD        = YearMonth.of(2024, 4); // Apr-2024

    // ── Test 1: Perfect match — all tax heads within tolerance ───────────────

    @Test
    @DisplayName("Perfect match — all tax heads identical → MATCH severity, zero delta")
    void perfectMatch_allZeroDelta() {
        Gstr1Vs3bInput input = buildInput(
                bd("50000"), bd("9000"), bd("0"),   bd("9000"), bd("0"),   // GSTR-1: 18% on 50000 IGST scenario? No — CGST+SGST
                /* g3b: same */ bd("9000"), bd("0"), bd("9000"), bd("0")
        );

        Gstr1Vs3bResult result = service.reconcile(input);

        assertThat(result.overallSeverity()).isEqualTo(ReconSeverity.MATCH);
        assertThat(result.totalDelta()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.deltas()).hasSize(4);
        result.deltas().forEach(d ->
                assertThat(d.severity()).isEqualTo(ReconSeverity.MATCH));
    }

    // ── Test 2: Minor under-reporting — delta < 5% ───────────────────────────

    @Test
    @DisplayName("Minor under-reporting (3%) — delta < 5% → MINOR severity")
    void minorUnderReporting_3Percent() {
        // GSTR-1 CGST: ₹10,000; GSTR-3B CGST: ₹9,700 → delta = ₹300 = 3%
        Gstr1Vs3bInput input = buildInput(
                bd("100000"), bd("0"), bd("10000"), bd("0"), bd("0"),
                bd("0"),      bd("9700"),  bd("0"),  bd("0")
        );

        Gstr1Vs3bResult result = service.reconcile(input);

        ReconDelta cgstDelta = findDelta(result, "CGST");
        assertThat(cgstDelta.severity()).isEqualTo(ReconSeverity.MINOR);
        assertThat(cgstDelta.delta()).isEqualByComparingTo(bd("300.00"));
        assertThat(cgstDelta.deltaPercent()).isEqualByComparingTo(bd("3.00"));
        assertThat(result.overallSeverity()).isEqualTo(ReconSeverity.MINOR);
    }

    // ── Test 3: Material under-reporting — 5% ≤ delta < 20% ─────────────────

    @Test
    @DisplayName("Material under-reporting (10%) — 5%≤delta<20% → MATERIAL severity")
    void materialUnderReporting_10Percent() {
        // GSTR-1 IGST: ₹50,000; GSTR-3B IGST: ₹45,000 → delta = ₹5,000 = 10%
        Gstr1Vs3bInput input = buildInput(
                bd("300000"), bd("50000"), bd("0"), bd("0"), bd("0"),
                bd("45000"),  bd("0"),     bd("0"), bd("0")
        );

        Gstr1Vs3bResult result = service.reconcile(input);

        ReconDelta igstDelta = findDelta(result, "IGST");
        assertThat(igstDelta.severity()).isEqualTo(ReconSeverity.MATERIAL);
        assertThat(igstDelta.delta()).isEqualByComparingTo(bd("5000.00"));
        assertThat(result.overallSeverity()).isEqualTo(ReconSeverity.MATERIAL);
    }

    // ── Test 4: Critical under-reporting — delta ≥ 20% ──────────────────────

    @Test
    @DisplayName("Critical under-reporting (50%) — delta≥20% → CRITICAL severity")
    void criticalUnderReporting_50Percent() {
        // GSTR-1 CGST: ₹20,000; GSTR-3B CGST: ₹10,000 → delta = ₹10,000 = 50%
        Gstr1Vs3bInput input = buildInput(
                bd("200000"), bd("0"), bd("20000"), bd("0"), bd("0"),
                bd("0"),      bd("10000"),  bd("0"), bd("0")
        );

        Gstr1Vs3bResult result = service.reconcile(input);

        ReconDelta cgstDelta = findDelta(result, "CGST");
        assertThat(cgstDelta.severity()).isEqualTo(ReconSeverity.CRITICAL);
        assertThat(result.overallSeverity()).isEqualTo(ReconSeverity.CRITICAL);
    }

    // ── Test 5: Multi-head mismatch — worst case drives overall ─────────────

    @Test
    @DisplayName("Multi-head mismatch — overall severity = worst individual head")
    void multiHead_worstCaseDrivesOverall() {
        // CGST: 3% (MINOR), SGST: 25% (CRITICAL) → overall = CRITICAL
        Gstr1Vs3bInput input = buildInput(
                bd("100000"), bd("0"), bd("10000"), bd("10000"), bd("0"),
                bd("0"),      bd("9700"),  bd("7500"),  bd("0")   // CGST -3%, SGST -25%
        );

        Gstr1Vs3bResult result = service.reconcile(input);

        assertThat(findDelta(result, "CGST").severity()).isEqualTo(ReconSeverity.MINOR);
        assertThat(findDelta(result, "SGST/UTGST").severity()).isEqualTo(ReconSeverity.CRITICAL);
        assertThat(result.overallSeverity()).isEqualTo(ReconSeverity.CRITICAL);
    }

    // ── Test 6: Both amounts zero — MATCH ────────────────────────────────────

    @Test
    @DisplayName("Both GSTR-1 and GSTR-3B amounts zero → MATCH (nil return)")
    void bothAmountsZero_nilReturn() {
        Gstr1Vs3bInput input = buildInput(
                bd("0"), bd("0"), bd("0"), bd("0"), bd("0"),
                bd("0"), bd("0"), bd("0"), bd("0")
        );

        Gstr1Vs3bResult result = service.reconcile(input);

        assertThat(result.overallSeverity()).isEqualTo(ReconSeverity.MATCH);
        assertThat(result.totalDelta()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── Test 7: Tolerance boundary — ₹0.50 within ₹1 amount tolerance ────────

    @Test
    @DisplayName("Delta ₹0.50 within ₹1 amount tolerance → MATCH despite >0 percent")
    void toleranceBoundary_within1Rupee() {
        // GSTR-1 CGST: ₹100,000; GSTR-3B CGST: ₹99,999.50 → delta = ₹0.50 < ₹1 tolerance
        Gstr1Vs3bInput input = buildInput(
                bd("1000000"), bd("0"), bd("100000"), bd("0"), bd("0"),
                bd("0"),       bd("99999.50"), bd("0"), bd("0")
        );

        Gstr1Vs3bResult result = service.reconcile(input);

        // Delta = 0.50, percent = 0.0005% < 0.01% tolerance → MATCH
        assertThat(findDelta(result, "CGST").severity()).isEqualTo(ReconSeverity.MATCH);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** g1: taxable, igst, cgst, sgstUtgst, cess  |  g3b: igst, cgst, sgstUtgst, cess */
    private Gstr1Vs3bInput buildInput(
            BigDecimal g1Taxable, BigDecimal g1Igst, BigDecimal g1Cgst,
            BigDecimal g1Sgst, BigDecimal g1Cess,
            BigDecimal g3bIgst, BigDecimal g3bCgst, BigDecimal g3bSgst, BigDecimal g3bCess) {

        return new Gstr1Vs3bInput(
                "07ASXPD9282E1Z8",
                PERIOD,
                "2024-25",
                new LiabilitySummary(g1Taxable, g1Igst, g1Cgst, g1Sgst, g1Cess),
                new TaxPaymentSummary(g3bIgst, g3bCgst, g3bSgst, g3bCess),
                TOLERANCE_AMT,
                TOLERANCE_PCT
        );
    }

    private ReconDelta findDelta(Gstr1Vs3bResult result, String taxHead) {
        return result.deltas().stream()
                .filter(d -> d.taxHead().equals(taxHead))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Delta for " + taxHead + " not found"));
    }

    private BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
