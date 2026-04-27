package com.learning.backendservice.engine.rules;

import com.learning.backendservice.domain.recon.*;
import com.learning.backendservice.engine.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class Gstr1Vs3bVs9ReconciliationRule implements AuditRule<Gstr1Vs3bVs9Input, Gstr1Vs3bVs9Result> {

    static final String RULE_ID = "RECON_1_VS_3B_VS_9";
    static final String LEGAL_BASIS = "Section 9, 37, 39 and 44 of CGST Act";

    private final Gstr1Vs3bVs9ReconciliationService service;

    public Gstr1Vs3bVs9ReconciliationRule() {
        this.service = new Gstr1Vs3bVs9ReconciliationService();
    }

    @Override public String getRuleId() { return RULE_ID; }
    @Override public String getName() { return "3-Way Reconciliation"; }
    @Override public String getDisplayName() { return "3-Way Reconciliation (GSTR-1 vs 3B vs 9)"; }
    @Override public String getDescription() { return "Annual reconciliation comparing outward supplies and tax paid across GSTR-1, GSTR-3B, and GSTR-9."; }
    @Override public String getCategory() { return "RECONCILIATION"; }
    @Override public String getLegalBasis() { return LEGAL_BASIS; }
    @Override public int getExecutionOrder() { return 50; }

    @Override
    public Set<AnalysisMode> getApplicableModes() {
        return Set.of(AnalysisMode.GSTR_RULES_ANALYSIS);
    }

    @Override
    public Set<DocumentType> getRequiredDocumentTypes() {
        return Set.of(DocumentType.GSTR_1, DocumentType.GSTR_3B, DocumentType.GSTR_9);
    }

    @Override
    public AuditRuleResult<Gstr1Vs3bVs9Result> execute(Gstr1Vs3bVs9Input input, AuditContext context) {
        Gstr1Vs3bVs9Result result = service.reconcile(input);
        List<AuditFinding> findings = new ArrayList<>();
        
        String compliancePeriod = "FY " + input.financialYear();

        BigDecimal maxDelta = BigDecimal.ZERO;

        for (ThreeWayReconDelta delta : result.deltas()) {
            boolean mismatch1vs3B = delta.delta1Vs3b().abs().compareTo(input.reconToleranceAmount()) > 0;
            boolean mismatch3Bvs9 = delta.delta3bVs9().abs().compareTo(input.reconToleranceAmount()) > 0;
            
            if (mismatch1vs3B) {
                findings.add(new AuditFinding(
                        RULE_ID + "_1_VS_3B_" + delta.taxHead(),
                        AuditFinding.Severity.HIGH,
                        LEGAL_BASIS,
                        compliancePeriod,
                        delta.delta1Vs3b().abs(),
                        String.format("Mismatch in %s between GSTR-1 (₹%.2f) and GSTR-3B (₹%.2f). Delta: ₹%.2f",
                                delta.taxHead(), delta.gstr1Amount(), delta.gstr3bAmount(), delta.delta1Vs3b()),
                        "Ensure tax liability in GSTR-1 is fully paid in GSTR-3B.",
                        false
                ));
            }

            if (mismatch3Bvs9) {
                findings.add(new AuditFinding(
                        RULE_ID + "_3B_VS_9_" + delta.taxHead(),
                        AuditFinding.Severity.MEDIUM,
                        LEGAL_BASIS,
                        compliancePeriod,
                        delta.delta3bVs9().abs(),
                        String.format("Mismatch in %s between GSTR-3B (₹%.2f) and GSTR-9 Paid (₹%.2f). Delta: ₹%.2f",
                                delta.taxHead(), delta.gstr3bAmount(), delta.gstr9PaidAmount(), delta.delta3bVs9()),
                        "Review tax paid in annual return against monthly filings.",
                        false
                ));
            }

            BigDecimal currentMax = delta.delta1Vs3b().abs().max(delta.delta3bVs9().abs());
            if (currentMax.compareTo(maxDelta) > 0) {
                maxDelta = currentMax;
            }
        }

        if (findings.isEmpty()) {
            findings.add(AuditFinding.info(RULE_ID, LEGAL_BASIS, result.narrative()));
        }

        return new AuditRuleResult<>(findings, result, maxDelta, 1);
    }
}
