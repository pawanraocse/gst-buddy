package com.learning.backendservice.engine.resolvers;

import com.learning.backendservice.domain.itc.ItcRecoInput;
import com.learning.backendservice.domain.shared.PurchaseRegisterRow;
import com.learning.backendservice.engine.AuditContext;
import com.learning.backendservice.engine.AuditDocument;
import com.learning.backendservice.engine.DocumentType;
import com.learning.backendservice.engine.InputResolver;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ItcReco2bInputResolver implements InputResolver<ItcRecoInput> {

    @Override
    public String getRuleId() {
        return "ITC_RECO_2B";
    }

    @Override
    public ItcRecoInput resolve(AuditContext context) {
        AuditDocument prDoc = context.getDocument(DocumentType.PURCHASE_REGISTER)
                .orElseThrow(() -> new IllegalStateException("ITC_RECO_2B requires PURCHASE_REGISTER document."));
        
        AuditDocument gstr2bDoc = context.getDocument(DocumentType.GSTR_2B)
                .orElseThrow(() -> new IllegalStateException("ITC_RECO_2B requires GSTR_2B document."));

        if (!gstr2bDoc.taxPeriod().equals(prDoc.taxPeriod())) {
            throw new IllegalStateException("Tax period mismatch between Purchase Register and GSTR-2B.");
        }

        List<PurchaseRegisterRow> prRows = extractPurchaseRegisterRows(prDoc);
        List<PurchaseRegisterRow> gstr2bRows = extractGstr2bRows(gstr2bDoc);

        return new ItcRecoInput(
                gstr2bDoc.gstin() != null ? gstr2bDoc.gstin() : prDoc.gstin(),
                gstr2bDoc.taxPeriod(),
                context.financialYear(),
                prRows,
                gstr2bRows,
                context.sharedResources().reconToleranceAmount()
        );
    }

    private List<PurchaseRegisterRow> extractPurchaseRegisterRows(AuditDocument doc) {
        List<PurchaseRegisterRow> rows = new ArrayList<>();
        Map<String, Object> fields = doc.extractedFields();
        Object prObj = fields.get("purchase_register");
        
        if (prObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    rows.add(new PurchaseRegisterRow(
                            (String) map.get("supplier_gstin"),
                            (String) map.get("invoice_no"),
                            parseDate((String) map.get("invoice_date")),
                            toBigDecimal(map.get("taxable_value")),
                            toBigDecimal(map.get("igst")),
                            toBigDecimal(map.get("cgst")),
                            toBigDecimal(map.get("sgst")),
                            toBigDecimal(map.get("cess")),
                            Boolean.TRUE.equals(map.get("rcm_flag"))
                    ));
                }
            }
        }
        return rows;
    }

    private List<PurchaseRegisterRow> extractGstr2bRows(AuditDocument doc) {
        List<PurchaseRegisterRow> rows = new ArrayList<>();
        Map<String, Object> fields = doc.extractedFields();
        Object itcRowsObj = fields.get("itc_rows");
        
        if (itcRowsObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    rows.add(new PurchaseRegisterRow(
                            (String) map.get("supplier_gstin"),
                            (String) map.get("invoice_no"),
                            parseDate((String) map.get("invoice_date")),
                            toBigDecimal(map.get("taxable_value")),
                            toBigDecimal(map.get("igst")),
                            toBigDecimal(map.get("cgst")),
                            toBigDecimal(map.get("sgst")),
                            toBigDecimal(map.get("cess")),
                            false // GSTR-2B ITC extraction doesn't specifically mark RCM in this basic structure right now
                    ));
                }
            }
        }
        return rows;
    }

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

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
