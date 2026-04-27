package com.learning.backendservice.domain.itc;

import com.learning.backendservice.domain.shared.PurchaseRegisterRow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ItcReconciliationServiceTest {

    private final ItcReconciliationService service = new ItcReconciliationService();
    private final YearMonth TAX_PERIOD = YearMonth.of(2024, 4);
    private final String FY = "2024-25";
    private final BigDecimal TOLERANCE = new BigDecimal("1.00");

    @Test
    void testExactMatch() {
        List<PurchaseRegisterRow> books = List.of(row("GSTIN1", "INV-001", "1000", "90", "90", "0"));
        List<PurchaseRegisterRow> gstr2b = List.of(row("GSTIN1", "INV-001", "1000", "90", "90", "0"));

        ItcRecoInput input = new ItcRecoInput("MYGSTIN", TAX_PERIOD, FY, books, gstr2b, TOLERANCE);
        ItcRecoResult result = service.reconcile(input);

        assertThat(result.totalMatchedInvoices()).isEqualTo(1);
        assertThat(result.mismatches()).isEmpty();
        assertThat(result.totalItcAtRisk()).isEqualByComparingTo("0");
    }

    @Test
    void testMissingIn2B() {
        List<PurchaseRegisterRow> books = List.of(row("GSTIN1", "INV-001", "1000", "90", "90", "0"));
        List<PurchaseRegisterRow> gstr2b = List.of();

        ItcRecoInput input = new ItcRecoInput("MYGSTIN", TAX_PERIOD, FY, books, gstr2b, TOLERANCE);
        ItcRecoResult result = service.reconcile(input);

        assertThat(result.totalMatchedInvoices()).isEqualTo(0);
        assertThat(result.mismatches()).hasSize(1);
        ItcMismatch m = result.mismatches().get(0);
        assertThat(m.type()).isEqualTo(ItcMismatchType.MISSING_IN_2B);
        assertThat(m.deltaAmount()).isEqualByComparingTo("180"); // 90+90
        assertThat(result.totalItcAtRisk()).isEqualByComparingTo("180");
    }

    @Test
    void testMissingInBooks() {
        List<PurchaseRegisterRow> books = List.of();
        List<PurchaseRegisterRow> gstr2b = List.of(row("GSTIN1", "INV-001", "1000", "90", "90", "0"));

        ItcRecoInput input = new ItcRecoInput("MYGSTIN", TAX_PERIOD, FY, books, gstr2b, TOLERANCE);
        ItcRecoResult result = service.reconcile(input);

        assertThat(result.totalMatchedInvoices()).isEqualTo(0);
        assertThat(result.mismatches()).hasSize(1);
        ItcMismatch m = result.mismatches().get(0);
        assertThat(m.type()).isEqualTo(ItcMismatchType.MISSING_IN_BOOKS);
        // deltaAmount for MISSING_IN_BOOKS is represented as negative missing tax
        assertThat(m.deltaAmount()).isEqualByComparingTo("-180");
        assertThat(result.totalItcAtRisk()).isEqualByComparingTo("0"); // Missing in books doesn't add to ITC at risk
    }

    @Test
    void testAmountMismatchOutsideTolerance() {
        List<PurchaseRegisterRow> books = List.of(row("GSTIN1", "INV-001", "1000", "100", "100", "0")); // 200 total tax
        List<PurchaseRegisterRow> gstr2b = List.of(row("GSTIN1", "INV-001", "1000", "90", "90", "0"));   // 180 total tax

        ItcRecoInput input = new ItcRecoInput("MYGSTIN", TAX_PERIOD, FY, books, gstr2b, TOLERANCE);
        ItcRecoResult result = service.reconcile(input);

        assertThat(result.totalMatchedInvoices()).isEqualTo(0);
        assertThat(result.mismatches()).hasSize(1);
        ItcMismatch m = result.mismatches().get(0);
        assertThat(m.type()).isEqualTo(ItcMismatchType.AMOUNT_MISMATCH);
        assertThat(m.deltaAmount()).isEqualByComparingTo("20"); // 200 - 180
        assertThat(result.totalItcAtRisk()).isEqualByComparingTo("20");
    }

    @Test
    void testAmountMismatchWithinTolerance() {
        List<PurchaseRegisterRow> books = List.of(row("GSTIN1", "INV-001", "1000", "90.50", "90.50", "0")); // 181
        List<PurchaseRegisterRow> gstr2b = List.of(row("GSTIN1", "INV-001", "1000", "90.00", "90.00", "0")); // 180

        ItcRecoInput input = new ItcRecoInput("MYGSTIN", TAX_PERIOD, FY, books, gstr2b, TOLERANCE); // Tolerance 1.00
        ItcRecoResult result = service.reconcile(input);

        assertThat(result.totalMatchedInvoices()).isEqualTo(1); // Should match since 181 - 180 = 1 <= 1.00
        assertThat(result.mismatches()).isEmpty();
        assertThat(result.totalItcAtRisk()).isEqualByComparingTo("0");
    }

    @Test
    void testGstinTypoFuzzyMatch() {
        List<PurchaseRegisterRow> books = List.of(row("GSTIN_TYPO", "INV-123/A", "1000", "90", "90", "0"));
        // 2B has correct GSTIN and differently formatted invoice number but same alphanumeric
        List<PurchaseRegisterRow> gstr2b = List.of(row("GSTIN_CORRECT", "INV123A", "1000", "90", "90", "0"));

        ItcRecoInput input = new ItcRecoInput("MYGSTIN", TAX_PERIOD, FY, books, gstr2b, TOLERANCE);
        ItcRecoResult result = service.reconcile(input);

        assertThat(result.totalMatchedInvoices()).isEqualTo(0);
        assertThat(result.mismatches()).hasSize(1);
        ItcMismatch m = result.mismatches().get(0);
        assertThat(m.type()).isEqualTo(ItcMismatchType.GSTIN_MISMATCH);
        assertThat(m.deltaAmount()).isEqualByComparingTo("0");
        assertThat(result.totalItcAtRisk()).isEqualByComparingTo("0"); // Matched functionally, just warning
    }

    @Test
    void testMultipleMismatches() {
        List<PurchaseRegisterRow> books = List.of(
                row("G1", "I1", "100", "9", "9", "0"), // exact
                row("G1", "I2", "100", "10", "10", "0"), // amount mismatch outside tolerance (+2)
                row("G1", "I3", "100", "9", "9", "0")  // missing in 2B (+18)
        );
        List<PurchaseRegisterRow> gstr2b = List.of(
                row("G1", "I1", "100", "9", "9", "0"),
                row("G1", "I2", "100", "9", "9", "0"),
                row("G1", "I4", "100", "9", "9", "0")  // missing in books (doesn't add to risk)
        );

        ItcRecoInput input = new ItcRecoInput("MYGSTIN", TAX_PERIOD, FY, books, gstr2b, TOLERANCE);
        ItcRecoResult result = service.reconcile(input);

        assertThat(result.totalMatchedInvoices()).isEqualTo(1); // I1
        assertThat(result.mismatches()).hasSize(3); // I2, I3, I4
        
        long amtMistmatch = result.mismatches().stream().filter(m -> m.type() == ItcMismatchType.AMOUNT_MISMATCH).count();
        long miss2b = result.mismatches().stream().filter(m -> m.type() == ItcMismatchType.MISSING_IN_2B).count();
        long missBooks = result.mismatches().stream().filter(m -> m.type() == ItcMismatchType.MISSING_IN_BOOKS).count();

        assertThat(amtMistmatch).isEqualTo(1);
        assertThat(miss2b).isEqualTo(1);
        assertThat(missBooks).isEqualTo(1);

        assertThat(result.totalItcAtRisk()).isEqualByComparingTo("20"); // 2 from amount mismatch + 18 from missing in 2b
    }

    private PurchaseRegisterRow row(String gstin, String invNo, String taxVal, String cgst, String sgst, String igst) {
        return new PurchaseRegisterRow(
                gstin, invNo, LocalDate.of(2024, 4, 1),
                bd(taxVal), bd(igst), bd(cgst), bd(sgst), bd("0"), false
        );
    }

    private BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
