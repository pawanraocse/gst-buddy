package com.learning.backendservice.engine.resolvers;

import com.learning.backendservice.domain.gstr1.Gstr1LateFeeInput;
import com.learning.backendservice.domain.gstr1.ReliefWindowSnapshot;
import com.learning.backendservice.engine.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Builds {@link Gstr1LateFeeInput} from the shared {@link AuditContext}.
 *
 * <p>Extracts GSTIN, ARN date, and tax period from the parsed GSTR-1 {@link AuditDocument},
 * then resolves the applicable relief window from the pre-loaded {@link SharedResources}.
 * No database calls — all resources are pre-loaded by {@code ContextEnricher}.
 */
@Component
public class Gstr1LateFeeInputResolver implements InputResolver<Gstr1LateFeeInput> {

    @Override
    public String getRuleId() {
        return "LATE_FEE_GSTR1";
    }

    @Override
    public Gstr1LateFeeInput resolve(AuditContext context) {
        AuditDocument doc = context.getDocument(DocumentType.GSTR_1)
                .orElseThrow(() -> new IllegalStateException(
                        "Gstr1LateFeeInputResolver: GSTR_1 document not found in context"));

        Map<String, Object> fields = doc.extractedFields();

        LocalDate arnDate = LocalDate.parse(
                String.valueOf(fields.get("arn_date")));

        YearMonth taxPeriod = doc.taxPeriod();
        if (taxPeriod == null) {
            throw new IllegalStateException(
                    "Gstr1LateFeeInputResolver: taxPeriod is null in GSTR_1 document '"
                    + doc.originalFilename() + "'");
        }

        // Resolve relief window from pre-loaded shared resources
        boolean isNilReturn = context.userParams().isNilReturn();
        String bucket = "GSTR_1_" + (isNilReturn ? "NIL" : "NON_NIL");
        List<ReliefWindowSnapshot> windows = context.sharedResources()
                .reliefWindowsByReturnType()
                .getOrDefault(bucket, List.of());

        // ContextEnricher pre-filters by returnType+appliesTo — first window is applicable.
        ReliefWindowSnapshot relief = windows.isEmpty() ? null : windows.get(0);

        return new Gstr1LateFeeInput(
                doc.gstin(),
                arnDate,
                taxPeriod,
                context.financialYear(),
                isNilReturn,
                context.userParams().isQrmp(),
                relief
        );
    }
}
