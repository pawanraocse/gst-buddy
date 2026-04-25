package com.learning.backendservice.engine.resolvers;

import com.learning.backendservice.domain.itc.Section16_4Input;
import com.learning.backendservice.engine.AuditContext;
import com.learning.backendservice.engine.AuditDocument;
import com.learning.backendservice.engine.DocumentType;
import com.learning.backendservice.engine.InputResolver;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class Section16_4InputResolver implements InputResolver<Section16_4Input> {

    public static final String RULE_ID = "SECTION_16_4_GUARD";

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Section16_4Input resolve(AuditContext context) {
        AuditDocument gstr2bDoc = context.getDocument(DocumentType.GSTR_2B)
                .orElseThrow(() -> new IllegalStateException(
                        "SECTION_16_4_GUARD requires a GSTR_2B document in context"));

        // filing date of 3B is needed to check if claim is after deadline
        LocalDate filingDate = null;
        Optional<AuditDocument> gstr3bDoc = context.getDocument(DocumentType.GSTR_3B);
        if (gstr3bDoc.isPresent()) {
            Map<String, Object> fields = gstr3bDoc.get().extractedFields();
            Object dt = fields.get("arn_date");
            if (dt != null) {
                filingDate = LocalDate.parse(String.valueOf(dt));
            }
        }

        String gstin = gstr2bDoc.gstin() != null ? gstr2bDoc.gstin() : context.stateCode();
        Map<String, Object> fields = gstr2bDoc.extractedFields();
        List<Section16_4Input.ItcRow> itcRows = new ArrayList<>();

        if (fields.get("itc_rows") instanceof List<?> rawRows) {
            for (Object obj : rawRows) {
                if (obj instanceof Map<?, ?> rowMap) {
                    itcRows.add(new Section16_4Input.ItcRow(
                            (String) rowMap.get("supplier_gstin"),
                            (String) rowMap.get("invoice_no"),
                            parseDate(rowMap.get("invoice_date")),
                            parseAmount(rowMap.get("igst")),
                            parseAmount(rowMap.get("cgst")),
                            parseAmount(rowMap.get("sgst")),
                            parseAmount(rowMap.get("cess")),
                            false // simplify debit note check for now
                    ));
                }
            }
        }

        return new Section16_4Input(
                gstin,
                context.asOnDate(),
                filingDate,
                null, // annualReturnDate - could be extracted if GSTR_9 is present
                itcRows
        );
    }

    private LocalDate parseDate(Object dtObj) {
        if (dtObj == null) return null;
        try {
            return LocalDate.parse(dtObj.toString());
        } catch (Exception e) {
            return null;
        }
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
