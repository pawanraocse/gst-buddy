package com.learning.backendservice.engine.rules;

import com.learning.backendservice.domain.risk.SupplierRiskInput;
import com.learning.backendservice.domain.risk.SupplierRiskResult;
import com.learning.backendservice.domain.risk.SupplierRiskScoringService;
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
public class SupplierRiskAuditRule implements AuditRule<SupplierRiskInput, SupplierRiskResult> {

    public static final String RULE_ID = "SUPPLIER_RISK_2A";

    private final SupplierRiskScoringService service;

    public SupplierRiskAuditRule() {
        this(new SupplierRiskScoringService());
    }

    public SupplierRiskAuditRule(SupplierRiskScoringService service) {
        this.service = service;
    }

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public String getName() {
        return "Supplier Risk Scoring";
    }

    @Override
    public String getDisplayName() {
        return "Supplier Risk Score";
    }

    @Override
    public String getDescription() {
        return "Scores suppliers based on their GSTIN status (Active/Cancelled) to flag risky ITC exposure.";
    }

    @Override
    public String getCategory() {
        return "RISK";
    }

    @Override
    public String getLegalBasis() {
        return "Section 16(2)(c), CGST Act 2017";
    }

    @Override
    public int getExecutionOrder() {
        return 60;
    }

    @Override
    public Set<DocumentType> getRequiredDocumentTypes() {
        return Set.of(DocumentType.GSTR_2A);
    }

    @Override
    public AuditRuleResult<SupplierRiskResult> execute(SupplierRiskInput input, AuditContext context) {
        SupplierRiskResult result = service.evaluate(input);
        List<AuditFinding> findings = new ArrayList<>();

        String compliancePeriod = "FY: " + context.financialYear() + ", As on: " + context.asOnDate();

        if (result.highRiskCount() > 0) {
            findings.add(new AuditFinding(
                    RULE_ID,
                    AuditFinding.Severity.HIGH,
                    getLegalBasis(),
                    compliancePeriod,
                    result.highRiskExposure(),
                    String.format("%d suppliers are CANCELLED. High risk ITC exposure is ₹%s.",
                            result.highRiskCount(), result.highRiskExposure()),
                    "Withhold payment to these suppliers and verify ITC eligibility.",
                    false
            ));
        } else {
            findings.add(AuditFinding.info(
                    RULE_ID,
                    getLegalBasis(),
                    "No CANCELLED suppliers detected. Risk profile is normal."
            ));
        }

        return new AuditRuleResult<>(
                findings,
                result,
                result.highRiskExposure(),
                getCreditsRequired()
        );
    }
}
