package com.learning.backendservice.engine.rules;

import com.learning.backendservice.domain.rcm.*;
import com.learning.backendservice.engine.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class RcmReco3bRule implements AuditRule<RcmRecoInput, RcmRecoResult> {

    static final String RULE_ID = "RCM_RECO_3B";
    static final String LEGAL_BASIS = "Section 9(3)/9(4) CGST Act 2017 & Notification 13/2017-CT(Rate)";

    private final RcmReconciliationService service;

    public RcmReco3bRule() {
        this.service = new RcmReconciliationService();
    }

    @Override public String getRuleId() { return RULE_ID; }
    @Override public String getName() { return "RCM Reconciliation"; }
    @Override public String getDisplayName() { return "RCM Reconciliation (GSTR-3B vs Books)"; }
    @Override public String getDescription() { return "Reconciles Reverse Charge Mechanism (RCM) liability declared in GSTR-3B Table 3.1(d) against RCM invoices in the purchase register."; }
    @Override public String getCategory() { return "RECONCILIATION"; }
    @Override public String getLegalBasis() { return LEGAL_BASIS; }
    @Override public int getExecutionOrder() { return 50; }

    @Override
    public Set<AnalysisMode> getApplicableModes() {
        return Set.of(AnalysisMode.GSTR_RULES_ANALYSIS);
    }

    @Override
    public Set<DocumentType> getRequiredDocumentTypes() {
        return Set.of(DocumentType.GSTR_3B, DocumentType.PURCHASE_REGISTER);
    }

    @Override
    public AuditRuleResult<RcmRecoResult> execute(RcmRecoInput input, AuditContext context) {
        RcmRecoResult result = service.reconcile(input);

        List<AuditFinding> findings = new ArrayList<>();
        String compliancePeriod = String.format("FY: %s, Tax Period: %s", input.financialYear(), input.taxPeriod());

        for (RcmMismatch mismatch : result.mismatches()) {
            if (mismatch.type() == RcmMismatchType.MATCHED) continue;

            String description;
            String recommendation;
            AuditFinding.Severity severity;

            if (mismatch.type() == RcmMismatchType.UNDECLARED_RCM) {
                severity = AuditFinding.Severity.HIGH;
                description = String.format(
                        "RCM liability ₹%.2f per Section 9(3)/9(4) not declared in 3B Table 3.1(d) for %s. " +
                        "(Books: ₹%.2f vs 3B: ₹%.2f). Note: Per Section 9(4), RCM on unregistered person supplies applies above ₹5,000/day aggregate threshold.",
                        mismatch.delta().abs(), mismatch.taxHead(), mismatch.booksAmount(), mismatch.gstr3bAmount()
                );
                recommendation = "Declare in next 3B Table 3.1(d) and claim ITC in Table 4A(3) to avoid demand.";
            } else { // OVER_DECLARED_RCM
                severity = AuditFinding.Severity.MEDIUM;
                description = String.format(
                        "RCM in 3B exceeds books by ₹%.2f for %s. (Books: ₹%.2f vs 3B: ₹%.2f). " +
                        "Excess ITC may have been claimed.",
                        mismatch.delta().abs(), mismatch.taxHead(), mismatch.booksAmount(), mismatch.gstr3bAmount()
                );
                recommendation = "Reverse excess ITC in 3B Table 4B(2). Per Section 50(3), 24% p.a. interest applies on wrongfully utilised ITC.";
            }

            findings.add(new AuditFinding(
                    RULE_ID,
                    severity,
                    LEGAL_BASIS,
                    compliancePeriod,
                    mismatch.delta().abs(),
                    description,
                    recommendation,
                    false
            ));
        }

        if (findings.isEmpty()) {
            findings.add(AuditFinding.info(RULE_ID, LEGAL_BASIS, result.narrative()));
        }

        return new AuditRuleResult<>(findings, result, result.totalAbsoluteMismatch(), 1);
    }
}
