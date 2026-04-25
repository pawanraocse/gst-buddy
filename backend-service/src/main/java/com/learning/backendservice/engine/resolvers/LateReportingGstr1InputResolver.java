package com.learning.backendservice.engine.resolvers;

import com.learning.backendservice.domain.gstr1.*;
import com.learning.backendservice.engine.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds {@link LateReportingGstr1Input} from the shared {@link AuditContext}.
 *
 * <p>Extracts the {@code invoices[]} array from the GSTR-1 parsed document and maps
 * each entry to an {@link InvoiceRow} record. The GSTR-1 tax period is taken from
 * {@link AuditDocument#taxPeriod()}.
 *
 * <p>No database calls — all data comes from the parser-populated {@link AuditDocument}.
 */
@Component
public class LateReportingGstr1InputResolver implements InputResolver<LateReportingGstr1Input> {

    @Override
    public String getRuleId() {
        return "LATE_REPORTING_GSTR1";
    }

    @Override
    public LateReportingGstr1Input resolve(AuditContext context) {
        AuditDocument doc = context.getDocument(DocumentType.GSTR_1)
                .orElseThrow(() -> new IllegalStateException(
                        "LateReportingGstr1InputResolver: GSTR_1 document not found in context"));

        if (doc.taxPeriod() == null) {
            throw new IllegalStateException(
                    "LateReportingGstr1InputResolver: taxPeriod missing in GSTR-1 document '"
                    + doc.originalFilename() + "'");
        }

        List<InvoiceRow> invoices = extractInvoices(doc);

        return new LateReportingGstr1Input(
                doc.gstin(),
                doc.taxPeriod(),
                context.financialYear(),
                context.userParams().isQrmp(),
                invoices
        );
    }

    // ── Invoice extraction ───────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<InvoiceRow> extractInvoices(AuditDocument doc) {
        Object invoicesObj = doc.extractedFields().get("invoices");
        if (!(invoicesObj instanceof List<?> rawList)) {
            return List.of();
        }

        List<InvoiceRow> invoices = new ArrayList<>();
        for (Object item : rawList) {
            if (!(item instanceof Map<?, ?> row)) continue;
            InvoiceRow invoice = mapToInvoiceRow(row);
            if (invoice != null) {
                invoices.add(invoice);
            }
        }
        return List.copyOf(invoices);
    }

    private InvoiceRow mapToInvoiceRow(Map<?, ?> row) {
        try {
            String    invoiceNo   = str(row.get("invoice_no"));
            LocalDate invoiceDate = LocalDate.parse(str(row.get("invoice_date")));
            String    pos         = str(row.get("place_of_supply"));
            BigDecimal taxable    = bd(row.get("taxable_value"));
            BigDecimal cgst       = bd(row.get("cgst"));
            BigDecimal sgst       = bd(row.get("sgst"));
            BigDecimal igst       = bd(row.get("igst"));
            BigDecimal cess       = bd(row.get("cess"));
            BigDecimal rate       = bd(row.get("rate"));

            if (invoiceNo == null || invoiceNo.isBlank()) return null;

            return new InvoiceRow(invoiceNo, invoiceDate, pos,
                    taxable, cgst, sgst, igst, cess, rate);
        } catch (Exception e) {
            // Skip malformed rows — logged at WARNING by PipelineExecutor
            return null;
        }
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    private String str(Object v) {
        return v == null ? "" : v.toString().trim();
    }

    private BigDecimal bd(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd.setScale(2, RoundingMode.HALF_UP);
        try {
            return new BigDecimal(v.toString()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
