package com.learning.backendservice.domain.rcm;

import com.learning.backendservice.domain.shared.PurchaseRegisterRow;
import com.learning.backendservice.domain.recon.TaxPaymentSummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RcmReconciliationServiceTest {

    private final RcmReconciliationService service = new RcmReconciliationService();
    private final YearMonth TAX_PERIOD = YearMonth.of(2024, 4);
    private final String FY = "2024-25";
    private final BigDecimal TOLERANCE = new BigDecimal("1.00");

    @Test
    void testMatchedRcm_WithinTolerance() {
        List<PurchaseRegisterRow> rows = List.of(
                row("GSTIN1", true, "100", "9", "9", "0")
        );
        TaxPaymentSummary g3b = new TaxPaymentSummary(
                bd("0"), bd("9.50"), bd("9.50"), bd("0") // within 1.00 tolerance
        );
        RcmRecoInput input = new RcmRecoInput("MYGSTIN", TAX_PERIOD, FY, rows, g3b, TOLERANCE);
        
        RcmRecoResult result = service.reconcile(input);
        
        assertThat(result.totalAbsoluteMismatch()).isEqualByComparingTo("1.00");
        assertThat(result.mismatches()).allMatch(m -> m.type() == RcmMismatchType.MATCHED);
    }

    @Test
    void testUndeclaredRcm_BooksGreater() {
        List<PurchaseRegisterRow> rows = List.of(
                row("GSTIN1", true, "50000", "4500", "4500", "0")
        );
        TaxPaymentSummary g3b = new TaxPaymentSummary(
                bd("0"), bd("0"), bd("0"), bd("0")
        );
        RcmRecoInput input = new RcmRecoInput("MYGSTIN", TAX_PERIOD, FY, rows, g3b, TOLERANCE);
        
        RcmRecoResult result = service.reconcile(input);
        
        assertThat(result.totalAbsoluteMismatch()).isEqualByComparingTo("9000");
        assertMismatch(result, "CGST", RcmMismatchType.UNDECLARED_RCM, "4500");
        assertMismatch(result, "SGST/UTGST", RcmMismatchType.UNDECLARED_RCM, "4500");
    }

    @Test
    void testOverDeclaredRcm_3bGreater() {
        List<PurchaseRegisterRow> rows = List.of();
        TaxPaymentSummary g3b = new TaxPaymentSummary(
                bd("3000"), bd("0"), bd("0"), bd("0")
        );
        RcmRecoInput input = new RcmRecoInput("MYGSTIN", TAX_PERIOD, FY, rows, g3b, TOLERANCE);
        
        RcmRecoResult result = service.reconcile(input);
        
        assertThat(result.totalAbsoluteMismatch()).isEqualByComparingTo("3000");
        assertMismatch(result, "IGST", RcmMismatchType.OVER_DECLARED_RCM, "-3000");
    }

    @Test
    void testMixedForwardAndRcmInvoices() {
        List<PurchaseRegisterRow> rows = List.of(
                row("GSTIN1", true, "1000", "90", "90", "0"),
                row("GSTIN2", false, "5000", "450", "450", "0") // Forward charge
        );
        TaxPaymentSummary g3b = new TaxPaymentSummary(
                bd("0"), bd("90"), bd("90"), bd("0")
        );
        RcmRecoInput input = new RcmRecoInput("MYGSTIN", TAX_PERIOD, FY, rows, g3b, TOLERANCE);
        
        RcmRecoResult result = service.reconcile(input);
        
        // Only RCM rows should be considered, so it matches exactly 90
        assertThat(result.totalAbsoluteMismatch()).isEqualByComparingTo("0");
        assertThat(result.mismatches()).allMatch(m -> m.type() == RcmMismatchType.MATCHED);
        assertThat(result.totalRcmInvoicesInBooks()).isEqualTo(1);
    }

    @Test
    void testSupplierBreakdown_ThreeSuppliers() {
        List<PurchaseRegisterRow> rows = List.of(
                row("SUP1", true, "100", "9", "9", "0"),
                row("SUP2", true, "200", "18", "18", "0"),
                row("SUP3", true, "300", "0", "0", "54"), // IGST
                row("SUP1", true, "50", "4.5", "4.5", "0")
        );
        RcmRecoInput input = new RcmRecoInput("MYGSTIN", TAX_PERIOD, FY, rows, TaxPaymentSummary.zero(), TOLERANCE);
        
        RcmRecoResult result = service.reconcile(input);
        
        assertThat(result.supplierBreakdown()).hasSize(3);
        RcmSupplierBreakdown sup1 = result.supplierBreakdown().stream().filter(s -> s.supplierGstin().equals("SUP1")).findFirst().get();
        assertThat(sup1.invoiceCount()).isEqualTo(2);
        assertThat(sup1.totalTaxableValue()).isEqualByComparingTo("150");
        assertThat(sup1.totalRcmTax()).isEqualByComparingTo("27");
    }

    @Test
    void testAllZero_NoRcmAnywhere() {
        RcmRecoInput input = new RcmRecoInput("MYGSTIN", TAX_PERIOD, FY, List.of(), TaxPaymentSummary.zero(), TOLERANCE);
        RcmRecoResult result = service.reconcile(input);
        
        assertThat(result.mismatches()).allMatch(m -> m.type() == RcmMismatchType.MATCHED);
        assertThat(result.totalAbsoluteMismatch()).isEqualByComparingTo("0");
        assertThat(result.narrative()).contains("Match confirmed");
    }

    @Test
    void testIgstOnlyRcm() {
        List<PurchaseRegisterRow> rows = List.of(
                row("FOREIGN", true, "1000", "0", "0", "180")
        );
        TaxPaymentSummary g3b = new TaxPaymentSummary(
                bd("180"), bd("0"), bd("0"), bd("0")
        );
        RcmRecoInput input = new RcmRecoInput("MYGSTIN", TAX_PERIOD, FY, rows, g3b, TOLERANCE);
        RcmRecoResult result = service.reconcile(input);
        
        assertThat(result.totalAbsoluteMismatch()).isEqualByComparingTo("0");
    }

    @Test
    void testLargeVolume() {
        List<PurchaseRegisterRow> rows = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            rows.add(row("SUP" + (i % 10), true, "100", "9", "9", "0"));
        }
        TaxPaymentSummary g3b = new TaxPaymentSummary(
                bd("0"), bd("4500"), bd("4500"), bd("0") // 500 * 9 = 4500
        );
        RcmRecoInput input = new RcmRecoInput("MYGSTIN", TAX_PERIOD, FY, rows, g3b, TOLERANCE);
        RcmRecoResult result = service.reconcile(input);
        
        assertThat(result.totalAbsoluteMismatch()).isEqualByComparingTo("0");
        assertThat(result.supplierBreakdown()).hasSize(10);
    }

    // Helpers
    private PurchaseRegisterRow row(String gstin, boolean rcm, String taxVal, String cgst, String sgst, String igst) {
        return new PurchaseRegisterRow(
                gstin, "INV123", LocalDate.of(2024, 4, 1),
                bd(taxVal), bd(igst), bd(cgst), bd(sgst), bd("0"), rcm
        );
    }

    private BigDecimal bd(String val) {
        return new BigDecimal(val);
    }

    private void assertMismatch(RcmRecoResult result, String taxHead, RcmMismatchType type, String deltaExpected) {
        RcmMismatch m = result.mismatches().stream().filter(x -> x.taxHead().equals(taxHead)).findFirst().get();
        assertThat(m.type()).isEqualTo(type);
        assertThat(m.delta()).isEqualByComparingTo(deltaExpected);
    }
}
