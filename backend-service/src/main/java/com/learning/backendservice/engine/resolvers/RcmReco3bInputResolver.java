package com.learning.backendservice.engine.resolvers;

import com.learning.backendservice.domain.rcm.RcmRecoInput;
import com.learning.backendservice.domain.shared.PurchaseRegisterRow;
import com.learning.backendservice.domain.recon.TaxPaymentSummary;
import com.learning.backendservice.engine.AuditContext;
import com.learning.backendservice.engine.AuditDocument;
import com.learning.backendservice.engine.DocumentType;
import com.learning.backendservice.engine.InputResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RcmReco3bInputResolver implements InputResolver<RcmRecoInput> {

    private static final Logger log = LoggerFactory.getLogger(RcmReco3bInputResolver.class);

    @Override
    public String getRuleId() {
        return "RCM_RECO_3B";
    }

    @Override
    public RcmRecoInput resolve(AuditContext context) {
        AuditDocument prDoc = context.getDocument(DocumentType.PURCHASE_REGISTER)
                .orElseThrow(() -> new IllegalStateException("RCM_RECO_3B requires PURCHASE_REGISTER document."));
        
        AuditDocument gstr3bDoc = context.getDocument(DocumentType.GSTR_3B)
                .orElseThrow(() -> new IllegalStateException("RCM_RECO_3B requires GSTR_3B document."));

        if (!gstr3bDoc.taxPeriod().equals(prDoc.taxPeriod())) {
            throw new IllegalStateException("Tax period mismatch between Purchase Register and GSTR-3B.");
        }

        List<PurchaseRegisterRow> prRows = extractPurchaseRegisterRows(prDoc);
        TaxPaymentSummary gstr3bRcm = extractGstr3bRcm(gstr3bDoc);

        return new RcmRecoInput(
                prDoc.gstin() != null ? prDoc.gstin() : gstr3bDoc.gstin(),
                gstr3bDoc.taxPeriod(),
                context.financialYear(),
                prRows,
                gstr3bRcm,
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

    private TaxPaymentSummary extractGstr3bRcm(AuditDocument doc) {
        Map<String, Object> fields = doc.extractedFields();
        Object table31Obj = fields.get("table_3_1");

        if (table31Obj instanceof Map<?, ?> table31) {
            Object inwardRcmObj = table31.get("inward_rcm");
            if (inwardRcmObj instanceof Map<?, ?> rcmMap) {
                return new TaxPaymentSummary(
                        toBigDecimal(rcmMap.get("igst")),
                        toBigDecimal(rcmMap.get("cgst")),
                        toBigDecimal(rcmMap.get("sgst_utgst")),
                        toBigDecimal(rcmMap.get("cess"))
                );
            }
        }
        // If not present, assume zero
        return new TaxPaymentSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
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
