package com.learning.backendservice.engine.resolvers;

import com.learning.backendservice.domain.rule86b.Rule86bInput;
import com.learning.backendservice.engine.AuditContext;
import com.learning.backendservice.engine.AuditDocument;
import com.learning.backendservice.engine.DocumentType;
import com.learning.backendservice.engine.InputResolver;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

/**
 * Extracts GSTR-3B and GSTR-1 data for Rule 86B (Cash Ledger 1%) evaluation.
 */
@Component
public class Rule86bInputResolver implements InputResolver<Rule86bInput> {

    public static final String RULE_ID = "RULE_86B_RESTRICTION";

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public Rule86bInput resolve(AuditContext context) {
        AuditDocument gstr3bDoc = context.getDocument(DocumentType.GSTR_3B)
                .orElseThrow(() -> new IllegalStateException(
                        "RULE_86B_RESTRICTION requires a GSTR_3B document in context"));

        String gstin = gstr3bDoc.gstin() != null ? gstr3bDoc.gstin() : context.stateCode(); // Fallback
        YearMonth taxPeriod = gstr3bDoc.taxPeriod();
        if (taxPeriod == null) {
            throw new IllegalStateException("taxPeriod missing in GSTR-3B document");
        }
        
        // Convert YearMonth to a LocalDate (1st of the month) for effectiveFrom comparison
        LocalDate periodDate = taxPeriod.atDay(1);

        Map<String, Object> fields = gstr3bDoc.extractedFields();
        
        BigDecimal monthlyTaxableOutward = extractTaxableValue(fields, "table_3_1", "outward_taxable");
        
        BigDecimal totalTaxPayable = extractTotalTax(fields, "table_6_1", "tax_payable");
        BigDecimal rcmTaxPayable = extractTotalTax(fields, "table_3_1", "inward_rcm");
        BigDecimal paidInCash = extractTotalTax(fields, "table_6_1", "paid_in_cash");

        boolean hasGovtPsuFormat = checkGovtPsuFormat(gstin);
        boolean hasExportInvoices = checkExportInvoices(context);

        return new Rule86bInput(
                gstin,
                periodDate,
                monthlyTaxableOutward,
                totalTaxPayable,
                rcmTaxPayable,
                paidInCash,
                hasGovtPsuFormat,
                hasExportInvoices,
                context.sharedResources().rule86bConfig()
        );
    }

    @SuppressWarnings("unchecked")
    private BigDecimal extractTaxableValue(Map<String, Object> fields, String tableName, String rowName) {
        Object tableObj = fields.get(tableName);
        if (tableObj instanceof Map<?, ?> table) {
            Object rowObj = table.get(rowName);
            if (rowObj instanceof Map<?, ?> row) {
                return parseAmount(row.get("taxable_value"));
            }
        }
        return BigDecimal.ZERO;
    }

    @SuppressWarnings("unchecked")
    private BigDecimal extractTotalTax(Map<String, Object> fields, String tableName, String rowName) {
        Object tableObj = fields.get(tableName);
        if (tableObj instanceof Map<?, ?> table) {
            Object rowObj = table.get(rowName);
            if (rowObj instanceof Map<?, ?> row) {
                BigDecimal igst = parseAmount(row.get("igst"));
                BigDecimal cgst = parseAmount(row.get("cgst"));
                BigDecimal sgst = parseAmount(row.get("sgst_utgst"));
                BigDecimal cess = parseAmount(row.get("cess"));
                return igst.add(cgst).add(sgst).add(cess);
            }
        }
        return BigDecimal.ZERO;
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

    private boolean checkGovtPsuFormat(String gstin) {
        if (gstin == null || gstin.length() != 15) return false;
        // PAN is from 3rd to 12th char (index 2 to 11).
        // 4th char of PAN represents entity type, which is at index 5.
        // G = Government Agency, L = Local Authority
        char entityType = gstin.charAt(5);
        return entityType == 'G' || entityType == 'L';
    }

    private boolean checkExportInvoices(AuditContext context) {
        return context.getDocument(DocumentType.GSTR_1).map(doc -> {
            Map<String, Object> fields = doc.extractedFields();
            if (fields.get("invoices") instanceof java.util.List<?> rawInvoices) {
                for (Object obj : rawInvoices) {
                    if (obj instanceof Map<?, ?> map) {
                        String pos = (String) map.get("place_of_supply");
                        if ("96-Other Countries".equalsIgnoreCase(pos) || "96".equals(pos)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }).orElse(false);
    }
}
