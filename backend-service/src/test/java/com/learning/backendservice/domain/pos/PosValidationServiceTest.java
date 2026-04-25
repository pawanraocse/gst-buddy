package com.learning.backendservice.domain.pos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PosValidationService")
class PosValidationServiceTest {

    private final PosValidationService service = new PosValidationService();
    private static final String SUPPLIER_STATE = "29-Karnataka";

    private PosValidationInput.InvoiceData invoice(String no, String pos, String igst, String cgst, String sgst, String sec) {
        return new PosValidationInput.InvoiceData(
                no, pos,
                igst == null ? null : new BigDecimal(igst),
                cgst == null ? null : new BigDecimal(cgst),
                sgst == null ? null : new BigDecimal(sgst),
                sec
        );
    }

    @Test
    @DisplayName("should pass correct intra-state invoice")
    void testCorrectIntraState() {
        var inv = invoice("I1", "29-Karnataka", "0", "900", "900", "4A");
        var res = service.validate(new PosValidationInput(SUPPLIER_STATE, List.of(inv)));

        assertThat(res.totalInvoicesChecked()).isEqualTo(1);
        assertThat(res.totalMismatches()).isZero();
        assertThat(res.mismatches()).isEmpty();
    }

    @Test
    @DisplayName("should fail intra-state with IGST")
    void testWrongIntraState() {
        var inv = invoice("I2", "29-Karnataka", "1800", "0", "0", "4A");
        var res = service.validate(new PosValidationInput(SUPPLIER_STATE, List.of(inv)));

        assertThat(res.totalMismatches()).isEqualTo(1);
        assertThat(res.mismatches().get(0).mismatchType()).isEqualTo(PosMismatch.MismatchType.INTRASTATE_SUPPLY_WITH_IGST);
    }

    @Test
    @DisplayName("should pass correct inter-state invoice")
    void testCorrectInterState() {
        var inv = invoice("I3", "27-Maharashtra", "1800", "0", "0", "4A");
        var res = service.validate(new PosValidationInput(SUPPLIER_STATE, List.of(inv)));

        assertThat(res.totalInvoicesChecked()).isEqualTo(1);
        assertThat(res.totalMismatches()).isZero();
    }

    @Test
    @DisplayName("should fail inter-state with CGST/SGST")
    void testWrongInterState() {
        var inv = invoice("I4", "27-Maharashtra", "0", "900", "900", "4A");
        var res = service.validate(new PosValidationInput(SUPPLIER_STATE, List.of(inv)));

        assertThat(res.totalMismatches()).isEqualTo(1);
        assertThat(res.mismatches().get(0).mismatchType()).isEqualTo(PosMismatch.MismatchType.INTERSTATE_SUPPLY_WITH_CGST_SGST);
    }

    @Test
    @DisplayName("should handle SEZ supply as inter-state")
    void testSezSupply() {
        // Even if POS is same state, SEZ (4B) must have IGST
        var inv = invoice("I5", "29-Karnataka", "1800", "0", "0", "4B");
        var res = service.validate(new PosValidationInput(SUPPLIER_STATE, List.of(inv)));

        assertThat(res.totalMismatches()).isZero();

        // If SEZ has CGST/SGST, it's a mismatch
        var wrongInv = invoice("I6", "29-Karnataka", "0", "900", "900", "4B");
        var wrongRes = service.validate(new PosValidationInput(SUPPLIER_STATE, List.of(wrongInv)));

        assertThat(wrongRes.totalMismatches()).isEqualTo(1);
        assertThat(wrongRes.mismatches().get(0).mismatchType()).isEqualTo(PosMismatch.MismatchType.INTERSTATE_SUPPLY_WITH_CGST_SGST);
    }

    @Test
    @DisplayName("should handle export and other territory")
    void testExportAndOtherTerritory() {
        var inv1 = invoice("E1", "96-Other Countries", "1800", "0", "0", "6A");
        var inv2 = invoice("E2", "97-Other Territory", "1800", "0", "0", "4A");
        
        var res = service.validate(new PosValidationInput(SUPPLIER_STATE, List.of(inv1, inv2)));
        assertThat(res.totalMismatches()).isZero();
    }

    @Test
    @DisplayName("should skip zero-rated invoices")
    void testZeroRated() {
        var inv = invoice("Z1", "27-Maharashtra", "0", "0", "0", "4A");
        var res = service.validate(new PosValidationInput(SUPPLIER_STATE, List.of(inv)));

        assertThat(res.totalInvoicesChecked()).isZero();
        assertThat(res.totalMismatches()).isZero();
    }
}
