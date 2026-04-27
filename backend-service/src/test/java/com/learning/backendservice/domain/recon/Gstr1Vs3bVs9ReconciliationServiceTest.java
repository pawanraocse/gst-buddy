package com.learning.backendservice.domain.recon;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class Gstr1Vs3bVs9ReconciliationServiceTest {

    private final Gstr1Vs3bVs9ReconciliationService service = new Gstr1Vs3bVs9ReconciliationService();
    private final BigDecimal TOLERANCE = new BigDecimal("5.00");
    private final String FY = "2023-24";

    @Test
    void testPerfectMatch() {
        TaxPaymentSummary gstr1 = new TaxPaymentSummary(bd("100"), bd("50"), bd("50"), bd("10"));
        TaxPaymentSummary gstr3b = new TaxPaymentSummary(bd("100"), bd("50"), bd("50"), bd("10"));
        TaxPaymentSummary gstr9Table4 = new TaxPaymentSummary(bd("100"), bd("50"), bd("50"), bd("10"));
        TaxPaymentSummary gstr9Table9Paid = new TaxPaymentSummary(bd("100"), bd("50"), bd("50"), bd("10"));

        Gstr1Vs3bVs9Input input = new Gstr1Vs3bVs9Input("GSTIN", FY, gstr1, gstr3b, gstr9Table4, gstr9Table9Paid, TOLERANCE);
        Gstr1Vs3bVs9Result result = service.reconcile(input);

        assertThat(result.requiresAction()).isFalse();
        assertThat(result.deltas()).hasSize(4); // IGST, CGST, SGST, CESS
        
        Optional<ThreeWayReconDelta> igstDelta = result.deltas().stream().filter(d -> d.taxHead().equals("IGST")).findFirst();
        assertThat(igstDelta).isPresent();
        assertThat(igstDelta.get().delta1Vs3b()).isEqualByComparingTo("0");
        assertThat(igstDelta.get().delta3bVs9()).isEqualByComparingTo("0");
    }

    @Test
    void testMismatchOutsideTolerance() {
        TaxPaymentSummary gstr1 = new TaxPaymentSummary(bd("100"), bd("50"), bd("50"), bd("10"));
        TaxPaymentSummary gstr3b = new TaxPaymentSummary(bd("90"), bd("50"), bd("50"), bd("10")); // 10 diff in IGST
        TaxPaymentSummary gstr9Table4 = new TaxPaymentSummary(bd("100"), bd("50"), bd("50"), bd("10"));
        TaxPaymentSummary gstr9Table9Paid = new TaxPaymentSummary(bd("90"), bd("50"), bd("50"), bd("10"));

        Gstr1Vs3bVs9Input input = new Gstr1Vs3bVs9Input("GSTIN", FY, gstr1, gstr3b, gstr9Table4, gstr9Table9Paid, TOLERANCE);
        Gstr1Vs3bVs9Result result = service.reconcile(input);

        assertThat(result.requiresAction()).isTrue();
        
        Optional<ThreeWayReconDelta> igstDelta = result.deltas().stream().filter(d -> d.taxHead().equals("IGST")).findFirst();
        assertThat(igstDelta).isPresent();
        assertThat(igstDelta.get().delta1Vs3b()).isEqualByComparingTo("10"); // 100 - 90
        assertThat(igstDelta.get().delta3bVs9()).isEqualByComparingTo("0");  // 90 - 90
        assertThat(igstDelta.get().delta1Vs9()).isEqualByComparingTo("0");   // 100 - 100
    }

    @Test
    void testMismatchWithinTolerance() {
        TaxPaymentSummary gstr1 = new TaxPaymentSummary(bd("100"), bd("50"), bd("50"), bd("10"));
        TaxPaymentSummary gstr3b = new TaxPaymentSummary(bd("98"), bd("50"), bd("50"), bd("10")); // 2 diff in IGST (within 5)
        TaxPaymentSummary gstr9Table4 = new TaxPaymentSummary(bd("100"), bd("50"), bd("50"), bd("10"));
        TaxPaymentSummary gstr9Table9Paid = new TaxPaymentSummary(bd("98"), bd("50"), bd("50"), bd("10"));

        Gstr1Vs3bVs9Input input = new Gstr1Vs3bVs9Input("GSTIN", FY, gstr1, gstr3b, gstr9Table4, gstr9Table9Paid, TOLERANCE);
        Gstr1Vs3bVs9Result result = service.reconcile(input);

        assertThat(result.requiresAction()).isFalse(); // Tolerated
        
        Optional<ThreeWayReconDelta> igstDelta = result.deltas().stream().filter(d -> d.taxHead().equals("IGST")).findFirst();
        assertThat(igstDelta).isPresent();
        assertThat(igstDelta.get().delta1Vs3b()).isEqualByComparingTo("2");
    }

    private BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
