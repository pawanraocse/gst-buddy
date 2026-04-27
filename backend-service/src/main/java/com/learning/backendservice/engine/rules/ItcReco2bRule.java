package com.learning.backendservice.engine.rules;

import com.learning.backendservice.domain.itc.*;
import com.learning.backendservice.engine.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class ItcReco2bRule implements AuditRule<ItcRecoInput, ItcRecoResult> {

    static final String RULE_ID = "ITC_RECO_2B";
    static final String LEGAL_BASIS = "Section 16(2)(aa) of CGST Act & Rule 36(4)";

    private final ItcReconciliationService service;

    public ItcReco2bRule() {
        this.service = new ItcReconciliationService();
    }

    @Override public String getRuleId() { return RULE_ID; }
    @Override public String getName() { return "ITC Reconciliation 2B"; }
    @Override public String getDisplayName() { return "ITC Reconciliation (Books vs GSTR-2B)"; }
    @Override public String getDescription() { return "Performs invoice-level fuzzy matching of Purchase Register against GSTR-2B to identify eligible ITC."; }
    @Override public String getCategory() { return "RECONCILIATION"; }
    @Override public String getLegalBasis() { return LEGAL_BASIS; }
    @Override public int getExecutionOrder() { return 50; }

    @Override
    public Set<AnalysisMode> getApplicableModes() {
        return Set.of(AnalysisMode.GSTR_RULES_ANALYSIS);
    }

    @Override
    public Set<DocumentType> getRequiredDocumentTypes() {
        return Set.of(DocumentType.GSTR_2B, DocumentType.PURCHASE_REGISTER);
    }

    @Override
    public AuditRuleResult<ItcRecoResult> execute(ItcRecoInput input, AuditContext context) {
        ItcRecoResult result = service.reconcile(input);

        List<AuditFinding> findings = new ArrayList<>();
        String compliancePeriod = String.format("FY: %s, Tax Period: %s", input.financialYear(), input.taxPeriod());

        if (result.totalItcAtRisk().compareTo(BigDecimal.ZERO) > 0) {
            findings.add(new AuditFinding(
                    RULE_ID + "_RISK",
                    AuditFinding.Severity.HIGH,
                    LEGAL_BASIS,
                    compliancePeriod,
                    result.totalItcAtRisk(),
                    String.format("ITC at Risk: ₹%.2f. %d invoices missing in GSTR-2B and %d invoices have amount mismatches.", 
                            result.totalItcAtRisk(), 
                            result.mismatches().stream().filter(m -> m.type() == ItcMismatchType.MISSING_IN_2B).count(),
                            result.mismatches().stream().filter(m -> m.type() == ItcMismatchType.AMOUNT_MISMATCH).count()),
                    "Do not claim ITC for invoices missing in GSTR-2B. Follow up with suppliers to file GSTR-1.",
                    false
            ));
        }

        long missingInBooks = result.mismatches().stream().filter(m -> m.type() == ItcMismatchType.MISSING_IN_BOOKS).count();
        if (missingInBooks > 0) {
            findings.add(new AuditFinding(
                    RULE_ID + "_MISSING_BOOKS",
                    AuditFinding.Severity.MEDIUM,
                    LEGAL_BASIS,
                    compliancePeriod,
                    BigDecimal.ZERO,
                    String.format("%d invoices present in GSTR-2B but missing in Purchase Register.", missingInBooks),
                    "Verify if these are valid purchases not yet recorded, or incorrect GSTIN entered by supplier.",
                    false
            ));
        }

        if (findings.isEmpty()) {
            findings.add(AuditFinding.info(RULE_ID, LEGAL_BASIS, result.narrative()));
        }

        return new AuditRuleResult<>(findings, result, result.totalItcAtRisk(), 1);
    }
}
