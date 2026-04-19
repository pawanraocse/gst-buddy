package com.learning.backendservice.engine.resolvers;

import com.learning.backendservice.domain.gstr9.Gstr9LateFeeInput;
import com.learning.backendservice.engine.AuditContext;
import com.learning.backendservice.engine.AuditDocument;
import com.learning.backendservice.engine.DocumentType;
import com.learning.backendservice.engine.InputResolver;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * InputResolver for {@code LATE_FEE_GSTR9}.
 */
@Component
public class Gstr9LateFeeInputResolver implements InputResolver<Gstr9LateFeeInput> {

    @Override
    public String getRuleId() {
        return "LATE_FEE_GSTR9";
    }

    @Override
    public Gstr9LateFeeInput resolve(AuditContext context) {
        AuditDocument doc = context.getDocument(DocumentType.GSTR_9)
                .orElseThrow(() -> new IllegalStateException(
                        "LATE_FEE_GSTR9 requires a GSTR_9 document in context"));

        Map<String, Object> fields = doc.extractedFields();

        // GSTR-9 filing date (ARN Date)
        LocalDate filingDate = LocalDate.parse(String.valueOf(fields.get("arn_date")));

        String gstin = doc.gstin() != null ? doc.gstin()
                : String.valueOf(fields.getOrDefault("gstin", ""));

        // FY is generally provided at Context level by CA, but could also be parsed from document
        String financialYear = fields.containsKey("financial_year") 
                ? String.valueOf(fields.get("financial_year")) 
                : context.financialYear();

        return new Gstr9LateFeeInput(
                gstin,
                filingDate,
                financialYear,
                context.userParams().aggregateTurnover()
        );
    }
}
