package com.learning.backendservice.engine.resolvers;

import com.learning.backendservice.domain.gstr1.LateReportingGstr1Input;
import com.learning.backendservice.engine.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LateReportingGstr1InputResolverTest {

    private final LateReportingGstr1InputResolver resolver =
            new LateReportingGstr1InputResolver();

    private static final YearMonth PERIOD = YearMonth.of(2024, 4);

    // ── Test 1: Happy path — 2 invoices extracted ─────────────────────────────

    @Test
    @DisplayName("Happy path — 2 invoices in extractedFields → input built correctly")
    void happyPath_twoInvoices() {
        List<Map<String, Object>> invoiceRows = List.of(
                Map.of("invoice_no", "INV-001", "invoice_date", "2024-04-10",
                        "place_of_supply", "29-Karnataka",
                        "taxable_value", 10000.0, "cgst", 900.0, "sgst", 900.0,
                        "igst", 0.0, "cess", 0.0, "rate", 18.0),
                Map.of("invoice_no", "INV-002", "invoice_date", "2024-03-15",
                        "place_of_supply", "29-Karnataka",
                        "taxable_value", 5000.0, "cgst", 450.0, "sgst", 450.0,
                        "igst", 0.0, "cess", 0.0, "rate", 18.0)
        );

        AuditDocument doc = gstr1Doc(PERIOD, invoiceRows);
        AuditContext  ctx = context(List.of(doc), false);

        LateReportingGstr1Input input = resolver.resolve(ctx);

        assertThat(input.gstin()).isEqualTo("07ASXPD9282E1Z8");
        assertThat(input.gstr1TaxPeriod()).isEqualTo(PERIOD);
        assertThat(input.invoices()).hasSize(2);
        assertThat(input.invoices().get(0).invoiceNo()).isEqualTo("INV-001");
        assertThat(input.invoices().get(1).invoiceDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(input.isQrmp()).isFalse();
    }

    // ── Test 2: No invoices in document ──────────────────────────────────────

    @Test
    @DisplayName("No invoices in extractedFields → empty invoice list (not an error)")
    void noInvoices_emptyList() {
        AuditDocument doc = gstr1Doc(PERIOD, List.of());
        AuditContext  ctx = context(List.of(doc), false);

        LateReportingGstr1Input input = resolver.resolve(ctx);

        assertThat(input.invoices()).isEmpty();
    }

    // ── Test 3: Missing GSTR-1 document ──────────────────────────────────────

    @Test
    @DisplayName("Missing GSTR-1 document → IllegalStateException")
    void missingGstr1_throwsException() {
        AuditContext ctx = context(List.of(), false); // no documents at all

        assertThatThrownBy(() -> resolver.resolve(ctx))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GSTR_1 document not found");
    }

    // ── Test 4: QRMP flag is passed through ───────────────────────────────────

    @Test
    @DisplayName("QRMP=true passed from userParams → input.isQrmp() is true")
    void qrmpFlag_passedThrough() {
        AuditDocument doc = gstr1Doc(PERIOD, List.of());
        AuditContext  ctx = context(List.of(doc), true); // QRMP = true

        LateReportingGstr1Input input = resolver.resolve(ctx);

        assertThat(input.isQrmp()).isTrue();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AuditDocument gstr1Doc(YearMonth period, List<Map<String, Object>> invoices) {
        return new AuditDocument(
                DocumentType.GSTR_1, "gstr1.pdf", null,
                Map.of("invoices", invoices),
                period, "07ASXPD9282E1Z8");
    }

    private AuditContext context(List<AuditDocument> docs, boolean isQrmp) {
        AuditUserParams params = new AuditUserParams(isQrmp, false, null, null);
        return AuditContext.forAnalysis(
                "tenant-001", "user-001", LocalDate.of(2024, 5, 1),
                AnalysisMode.GSTR_RULES_ANALYSIS, docs,
                params, SharedResources.empty());
    }
}
