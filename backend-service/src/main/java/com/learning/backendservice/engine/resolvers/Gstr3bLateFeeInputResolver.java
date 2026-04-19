package com.learning.backendservice.engine.resolvers;

import com.learning.backendservice.domain.gstr1.ReliefWindowSnapshot;
import com.learning.backendservice.domain.gstr3b.Gstr3bLateFeeInput;
import com.learning.backendservice.engine.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * InputResolver for {@code LATE_FEE_GSTR3B} — extracts typed {@link Gstr3bLateFeeInput}
 * from the shared {@link AuditContext} without touching the database.
 *
 * <p>Requires a {@link DocumentType#GSTR_3B} document in the context.
 * Relief windows must be pre-loaded into {@link SharedResources} by {@code ContextEnricher}.
 */
@Component
public class Gstr3bLateFeeInputResolver implements InputResolver<Gstr3bLateFeeInput> {

    @Override
    public String getRuleId() {
        return "LATE_FEE_GSTR3B";
    }

    @Override
    public Gstr3bLateFeeInput resolve(AuditContext context) {
        AuditDocument doc = context.getDocument(DocumentType.GSTR_3B)
                .orElseThrow(() -> new IllegalStateException(
                        "LATE_FEE_GSTR3B requires a GSTR_3B document in context"));

        Map<String, Object> fields = doc.extractedFields();

        // Filing date — when GSTR-3B was actually submitted
        LocalDate filingDate = LocalDate.parse(String.valueOf(fields.get("filing_date")));

        // Tax period — "MM-YYYY" format from parser → YearMonth
        YearMonth taxPeriod = parseTaxPeriod(String.valueOf(fields.get("tax_period")));

        // GSTIN from document (already Modulo-11 validated at ingestion)
        String gstin = doc.gstin() != null ? doc.gstin()
                : String.valueOf(fields.getOrDefault("gstin", ""));

        // State code from GSTIN positions 0-1; falls back to context stateCode
        String stateCode = (gstin.length() >= 2) ? gstin.substring(0, 2) : context.stateCode();

        // Relief window from pre-loaded shared resources
        String appliesTo = context.userParams().isNilReturn() ? "NIL" : "NON_NIL";
        String key = "GSTR_3B_" + appliesTo;
        List<ReliefWindowSnapshot> windows = context.sharedResources()
                .reliefWindowsByReturnType()
                .getOrDefault(key, List.of());

        // Relief window — ContextEnricher pre-filters by returnType+appliesTo.
        // First window in the list is already the applicable one.
        ReliefWindowSnapshot relief = windows.isEmpty() ? null : windows.get(0);

        return new Gstr3bLateFeeInput(
                gstin,
                filingDate,
                taxPeriod,
                context.financialYear(),
                context.userParams().isNilReturn(),
                context.userParams().isQrmp(),
                stateCode,
                relief);
    }

    private YearMonth parseTaxPeriod(String raw) {
        // Expected format: "MM-YYYY" e.g. "03-2024"
        String[] parts = raw.split("-");
        return YearMonth.of(Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
    }
}
