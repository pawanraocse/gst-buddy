package com.learning.backendservice.engine.resolvers;

import com.learning.backendservice.domain.pos.PosValidationInput;
import com.learning.backendservice.engine.AuditContext;
import com.learning.backendservice.engine.AuditDocument;
import com.learning.backendservice.engine.DocumentType;
import com.learning.backendservice.engine.InputResolver;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Extracts GSTR-1 invoices for Place of Supply validation.
 */
@Component
public class PosValidationGstr1InputResolver implements InputResolver<PosValidationInput> {

    public static final String RULE_ID = "POS_VALIDATION_GSTR1";

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public PosValidationInput resolve(AuditContext context) {
        String supplierStateCode = context.stateCode();
        List<PosValidationInput.InvoiceData> invoices = new ArrayList<>();

        List<AuditDocument> gstr1Docs = context.getDocuments(DocumentType.GSTR_1);
        if (gstr1Docs.isEmpty()) {
            throw new IllegalStateException("GSTR-1 document missing for POS Validation Rule");
        }

        for (AuditDocument doc : gstr1Docs) {
            var data = doc.extractedFields();
            if (data == null || !data.containsKey("invoices")) {
                continue;
            }

            List<?> rawInvoices = (List<?>) data.get("invoices");
            for (Object obj : rawInvoices) {
                if (!(obj instanceof Map<?, ?> map)) continue;

                String invoiceNo = (String) map.get("invoice_no");
                String pos = (String) map.get("place_of_supply");
                String sec = (String) map.get("table_section");

                BigDecimal igst = parseAmount(map.get("igst"));
                BigDecimal cgst = parseAmount(map.get("cgst"));
                BigDecimal sgst = parseAmount(map.get("sgst"));

                // Fallback to GSTIN state if pos is missing
                if (pos == null || pos.isBlank()) {
                    String gstin = (String) map.get("supplier_gstin");
                    if (gstin == null) gstin = (String) map.get("gstin");
                    if (gstin != null && gstin.length() >= 2) {
                        pos = gstin.substring(0, 2);
                    }
                }

                invoices.add(new PosValidationInput.InvoiceData(
                        invoiceNo, pos, igst, cgst, sgst, sec
                ));
            }
        }

        return new PosValidationInput(supplierStateCode, invoices);
    }

    private BigDecimal parseAmount(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof Number n) return new BigDecimal(n.toString());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
