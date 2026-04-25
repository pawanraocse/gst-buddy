package com.learning.backendservice.engine.resolvers;

import com.learning.backendservice.domain.recon.Gstr1Vs3bInput;
import com.learning.backendservice.engine.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Gstr1Vs3bReconciliationInputResolverTest {

    private final Gstr1Vs3bReconciliationInputResolver resolver =
            new Gstr1Vs3bReconciliationInputResolver();

    private static final YearMonth PERIOD = YearMonth.of(2024, 4);

    // ── Test 1: Happy path ────────────────────────────────────────────────────

    @Test
    @DisplayName("Happy path — both docs present, same period → valid input built")
    void happyPath_buildInput() {
        AuditDocument gstr1 = gstr1Doc(PERIOD, Map.of(
                "liability_summary", Map.of(
                        "total_taxable_value", 100000.0,
                        "total_igst", 0.0,
                        "total_cgst", 9000.0,
                        "total_sgst_utgst", 9000.0,
                        "total_cess", 0.0
                )
        ));
        AuditDocument gstr3b = gstr3bDoc(PERIOD, Map.of(
                "table_6_1", Map.of(
                        "tax_payable", Map.of(
                                "igst", 0.0, "cgst", 9000.0, "sgst_utgst", 9000.0, "cess", 0.0)
                )
        ));

        AuditContext context = context(List.of(gstr1, gstr3b));
        Gstr1Vs3bInput input = resolver.resolve(context);

        assertThat(input.gstin()).isEqualTo("07ASXPD9282E1Z8");
        assertThat(input.taxPeriod()).isEqualTo(PERIOD);
        assertThat(input.gstr1Liability().cgst()).isEqualByComparingTo(new BigDecimal("9000.00"));
        assertThat(input.gstr3bTaxPayable().cgst()).isEqualByComparingTo(new BigDecimal("9000.00"));
        assertThat(input.reconToleranceAmount()).isEqualByComparingTo(new BigDecimal("1.00"));
    }

    // ── Test 2: Missing GSTR-3B document ─────────────────────────────────────

    @Test
    @DisplayName("Missing GSTR-3B → IllegalStateException")
    void missingGstr3b_throwsException() {
        AuditDocument gstr1 = gstr1Doc(PERIOD, Map.of("liability_summary", Map.of(
                "total_taxable_value", 0.0, "total_igst", 0.0,
                "total_cgst", 0.0, "total_sgst_utgst", 0.0, "total_cess", 0.0)));

        AuditContext context = context(List.of(gstr1)); // no GSTR-3B

        assertThatThrownBy(() -> resolver.resolve(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GSTR_3B document not found");
    }

    // ── Test 3: Period mismatch between GSTR-1 and GSTR-3B ───────────────────

    @Test
    @DisplayName("Tax period mismatch → IllegalStateException")
    void periodMismatch_throwsException() {
        AuditDocument gstr1  = gstr1Doc(YearMonth.of(2024, 4), Map.of("liability_summary",
                Map.of("total_taxable_value", 0.0, "total_igst", 0.0,
                        "total_cgst", 0.0, "total_sgst_utgst", 0.0, "total_cess", 0.0)));
        AuditDocument gstr3b = gstr3bDoc(YearMonth.of(2024, 3), Map.of("table_6_1", // Mar-2024
                Map.of("tax_payable", Map.of(
                        "igst", 0.0, "cgst", 0.0, "sgst_utgst", 0.0, "cess", 0.0))));

        AuditContext context = context(List.of(gstr1, gstr3b));

        assertThatThrownBy(() -> resolver.resolve(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tax period mismatch");
    }

    // ── Test 4: Missing liability_summary in GSTR-1 ───────────────────────────

    @Test
    @DisplayName("Missing liability_summary in GSTR-1 → IllegalStateException")
    void missingLiabilitySummary_throwsException() {
        AuditDocument gstr1  = gstr1Doc(PERIOD, Map.of(/* no liability_summary */));
        AuditDocument gstr3b = gstr3bDoc(PERIOD, Map.of("table_6_1",
                Map.of("tax_payable", Map.of(
                        "igst", 0.0, "cgst", 0.0, "sgst_utgst", 0.0, "cess", 0.0))));

        AuditContext context = context(List.of(gstr1, gstr3b));

        assertThatThrownBy(() -> resolver.resolve(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("liability_summary");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AuditDocument gstr1Doc(YearMonth period, Map<String, Object> fields) {
        return new AuditDocument(DocumentType.GSTR_1, "gstr1.pdf", null,
                fields, period, "07ASXPD9282E1Z8");
    }

    private AuditDocument gstr3bDoc(YearMonth period, Map<String, Object> fields) {
        return new AuditDocument(DocumentType.GSTR_3B, "gstr3b.pdf", null,
                fields, period, "07ASXPD9282E1Z8");
    }

    private AuditContext context(List<AuditDocument> docs) {
        SharedResources resources = SharedResources.empty();
        return AuditContext.forAnalysis(
                "tenant-001", "user-001", LocalDate.of(2024, 5, 1),
                AnalysisMode.GSTR_RULES_ANALYSIS, docs,
                AuditUserParams.defaults(), resources);
    }
}
