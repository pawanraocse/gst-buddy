package com.learning.backendservice.engine.resolvers;

import com.learning.backendservice.domain.gstr3b.Gstr3bInterestInput;
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
 * InputResolver for {@code INTEREST_GSTR3B}.
 * Extracts filing date and (if available) net cash liability for Section 50(1) interest computation.
 */
@Component
public class Gstr3bInterestInputResolver implements InputResolver<Gstr3bInterestInput> {

    @Override
    public String getRuleId() {
        return "INTEREST_GSTR3B";
    }

    @Override
    public Gstr3bInterestInput resolve(AuditContext context) {
        AuditDocument doc = context.getDocument(DocumentType.GSTR_3B)
                .orElseThrow(() -> new IllegalStateException(
                        "INTEREST_GSTR3B requires a GSTR_3B document in context"));

        Map<String, Object> fields = doc.extractedFields();

        LocalDate filingDate = LocalDate.parse(String.valueOf(fields.get("arn_date")));

        YearMonth taxPeriod = parseTaxPeriod(String.valueOf(fields.get("tax_period")));

        String gstin = doc.gstin() != null ? doc.gstin()
                : String.valueOf(fields.getOrDefault("gstin", ""));

        String stateCode = (gstin.length() >= 2) ? gstin.substring(0, 2) : context.stateCode();

        // PDF extraction for Table 6.1 cash paid is not yet implemented in parser
        // We initialize as null so the rule flags manual verification
        BigDecimal cgstCashPaid = parseBigDecimalOrNull(fields.get("cgst_cash_paid"));
        BigDecimal sgstCashPaid = parseBigDecimalOrNull(fields.get("sgst_cash_paid"));
        BigDecimal igstCashPaid = parseBigDecimalOrNull(fields.get("igst_cash_paid"));

        return new Gstr3bInterestInput(
                gstin,
                filingDate,
                taxPeriod,
                context.financialYear(),
                context.userParams().isQrmp(),
                stateCode,
                cgstCashPaid,
                sgstCashPaid,
                igstCashPaid
        );
    }

    private YearMonth parseTaxPeriod(String raw) {
        String[] parts = raw.split("-");
        return YearMonth.of(Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
    }

    private BigDecimal parseBigDecimalOrNull(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
