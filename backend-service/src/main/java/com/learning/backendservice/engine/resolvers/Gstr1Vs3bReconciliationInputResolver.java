package com.learning.backendservice.engine.resolvers;

import com.learning.backendservice.domain.recon.*;
import com.learning.backendservice.engine.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Builds {@link Gstr1Vs3bInput} from the shared {@link AuditContext}.
 *
 * <p>Extracts the GSTR-1 {@code liability_summary} and the GSTR-3B {@code table_6_1.tax_payable}
 * from their respective parsed documents. Both documents must share the same tax period.
 *
 * <p>Recon tolerance is read from {@code context.sharedResources()} — pre-loaded by
 * {@code ContextEnricher} from the {@code recon_tolerance_config} table.
 */
@Component
public class Gstr1Vs3bReconciliationInputResolver implements InputResolver<Gstr1Vs3bInput> {

    @Override
    public String getRuleId() {
        return "RECON_1_VS_3B";
    }

    @Override
    public Gstr1Vs3bInput resolve(AuditContext context) {
        AuditDocument gstr1Doc = context.getDocument(DocumentType.GSTR_1)
                .orElseThrow(() -> new IllegalStateException(
                        "Gstr1Vs3bReconciliationInputResolver: GSTR_1 document not found in context"));

        AuditDocument gstr3bDoc = context.getDocument(DocumentType.GSTR_3B)
                .orElseThrow(() -> new IllegalStateException(
                        "Gstr1Vs3bReconciliationInputResolver: GSTR_3B document not found in context"));

        // Both documents must share the same tax period
        if (gstr1Doc.taxPeriod() == null) {
            throw new IllegalStateException(
                    "Gstr1Vs3bReconciliationInputResolver: taxPeriod missing in GSTR-1 document '"
                    + gstr1Doc.originalFilename() + "'");
        }
        if (gstr3bDoc.taxPeriod() == null) {
            throw new IllegalStateException(
                    "Gstr1Vs3bReconciliationInputResolver: taxPeriod missing in GSTR-3B document '"
                    + gstr3bDoc.originalFilename() + "'");
        }
        if (!gstr1Doc.taxPeriod().equals(gstr3bDoc.taxPeriod())) {
            throw new IllegalStateException(String.format(
                    "Gstr1Vs3bReconciliationInputResolver: tax period mismatch — GSTR-1 is %s but GSTR-3B is %s. "
                    + "Upload documents for the same period.",
                    gstr1Doc.taxPeriod(), gstr3bDoc.taxPeriod()));
        }

        LiabilitySummary  gstr1Liability   = extractGstr1Liability(gstr1Doc);
        TaxPaymentSummary gstr3bTaxPayable = extractGstr3bTaxPayable(gstr3bDoc);

        SharedResources resources = context.sharedResources();

        return new Gstr1Vs3bInput(
                gstr1Doc.gstin(),
                gstr1Doc.taxPeriod(),
                context.financialYear(),
                gstr1Liability,
                gstr3bTaxPayable,
                resources.reconToleranceAmount(),
                resources.reconTolerancePercent()
        );
    }

    // ── GSTR-1 liability_summary extraction ─────────────────────────────────

    @SuppressWarnings("unchecked")
    private LiabilitySummary extractGstr1Liability(AuditDocument doc) {
        Map<String, Object> fields = doc.extractedFields();
        Object summaryObj = fields.get("liability_summary");

        if (summaryObj instanceof Map<?, ?> summary) {
            return new LiabilitySummary(
                    toBigDecimal(summary.get("total_taxable_value")),
                    toBigDecimal(summary.get("total_igst")),
                    toBigDecimal(summary.get("total_cgst")),
                    toBigDecimal(summary.get("total_sgst_utgst")),
                    toBigDecimal(summary.get("total_cess"))
            );
        }

        throw new IllegalStateException(
                "Gstr1Vs3bReconciliationInputResolver: 'liability_summary' missing from GSTR-1 '"
                + doc.originalFilename() + "'. Re-upload the latest GST portal PDF.");
    }

    // ── GSTR-3B table_6_1 extraction ────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private TaxPaymentSummary extractGstr3bTaxPayable(AuditDocument doc) {
        Map<String, Object> fields = doc.extractedFields();
        Object table61Obj = fields.get("table_6_1");

        if (table61Obj instanceof Map<?, ?> table61) {
            // We compare against 'tax_payable' (gross before ITC deduction)
            Object taxPayableObj = table61.get("tax_payable");
            if (taxPayableObj instanceof Map<?, ?> taxPayable) {
                return new TaxPaymentSummary(
                        toBigDecimal(taxPayable.get("igst")),
                        toBigDecimal(taxPayable.get("cgst")),
                        toBigDecimal(taxPayable.get("sgst_utgst")),
                        toBigDecimal(taxPayable.get("cess"))
                );
            }
        }

        throw new IllegalStateException(
                "Gstr1Vs3bReconciliationInputResolver: 'table_6_1.tax_payable' missing from GSTR-3B '"
                + doc.originalFilename() + "'. Re-upload the latest GST portal PDF.");
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return new BigDecimal(n.toString()).setScale(2, java.math.RoundingMode.HALF_UP);
        try {
            return new BigDecimal(value.toString()).setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
