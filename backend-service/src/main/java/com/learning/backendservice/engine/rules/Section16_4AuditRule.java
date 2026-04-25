package com.learning.backendservice.engine.rules;

import com.learning.backendservice.domain.itc.Section16_4CalculationService;
import com.learning.backendservice.domain.itc.Section16_4Input;
import com.learning.backendservice.domain.itc.Section16_4Result;
import com.learning.backendservice.engine.AuditContext;
import com.learning.backendservice.engine.AuditFinding;
import com.learning.backendservice.engine.AuditRule;
import com.learning.backendservice.engine.AuditRuleResult;
import com.learning.backendservice.engine.DocumentType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class Section16_4AuditRule implements AuditRule<Section16_4Input, Section16_4Result> {

    public static final String RULE_ID = "SECTION_16_4_GUARD";

    private final Section16_4CalculationService service;

    public Section16_4AuditRule() {
        this(new Section16_4CalculationService());
    }

    public Section16_4AuditRule(Section16_4CalculationService service) {
        this.service = service;
    }

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public String getName() {
        return "Section 16(4) ITC Claim Deadline Guard";
    }

    @Override
    public String getDisplayName() {
        return "Section 16(4) ITC Claim Guard";
    }

    @Override
    public String getDescription() {
        return "Flags invoices where ITC is claimed after the 30th November deadline of the subsequent financial year.";
    }

    @Override
    public String getCategory() {
        return "ITC";
    }

    @Override
    public String getLegalBasis() {
        return "Section 16(4), CGST Act 2017";
    }

    @Override
    public int getExecutionOrder() {
        return 30; // Between GSTR1 reconciliation and Rule 86B
    }

    @Override
    public Set<DocumentType> getRequiredDocumentTypes() {
        return Set.of(DocumentType.GSTR_2B); // Optionally GSTR_3B for filing date
    }

    @Override
    public AuditRuleResult<Section16_4Result> execute(Section16_4Input input, AuditContext context) {
        Section16_4Result result = service.evaluate(input);
        List<AuditFinding> findings = new ArrayList<>();

        String compliancePeriod = "FY: " + context.financialYear() + ", As on: " + context.asOnDate();

        if (result.totalExpiredItc().signum() > 0) {
            findings.add(new AuditFinding(
                    RULE_ID,
                    AuditFinding.Severity.HIGH,
                    getLegalBasis(),
                    compliancePeriod,
                    result.totalExpiredItc(),
                    String.format("%d invoices claiming ITC of ₹%s are past the Section 16(4) deadline.",
                            result.expiredRows().size(), result.totalExpiredItc()),
                    "Reverse the expired ITC in Table 4B(2) of GSTR-3B.",
                    false
            ));
        } else {
            findings.add(AuditFinding.info(
                    RULE_ID,
                    getLegalBasis(),
                    "All ITC claims fall within the Section 16(4) deadline."
            ));
        }

        return new AuditRuleResult<>(
                findings,
                result,
                result.totalExpiredItc(),
                getCreditsRequired()
        );
    }
}
