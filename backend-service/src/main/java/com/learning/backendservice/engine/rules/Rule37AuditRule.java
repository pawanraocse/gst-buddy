package com.learning.backendservice.engine.rules;


import com.learning.backendservice.domain.ledger.LedgerFileProcessor;
import com.learning.backendservice.domain.rule37.InterestRow;
import com.learning.backendservice.domain.rule37.LedgerResult;
import com.learning.backendservice.engine.*;
import com.learning.backendservice.exception.LedgerParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * GST Rule 37 — 180-Day ITC Reversal Audit Rule.
 *
 * <p><b>Legal basis:</b> Section 16(2) proviso of CGST Act, 2017 read with Rule 37
 * of CGST Rules, 2017. ITC availed on an invoice must be reversed (Table 4(B)(2) of
 * GSTR-3B) if payment is not made to the supplier within 180 days of the invoice date.
 * Interest is payable u/s 50(1) on reversed ITC.
 *
 * <p><b>Design note:</b> This class is a pure <em>adapter</em>. All domain calculation
 * logic remains in the unchanged {@link LedgerFileProcessor} and
 * {@code Rule37InterestCalculationService}. This class translates the generic
 * {@link AuditRule} contract into calls to the existing domain services.
 */
@Component
public class Rule37AuditRule implements AuditRule<List<MultipartFile>, List<LedgerResult>> {

    private static final Logger log = LoggerFactory.getLogger(Rule37AuditRule.class);

    static final String RULE_ID      = "RULE_37_ITC_REVERSAL";
    static final String DISPLAY_NAME = "Rule 37 — 180-Day ITC Reversal";
    static final String LEGAL_BASIS  = "Section 16(2) proviso, Rule 37 CGST Rules, 2017";

    private final LedgerFileProcessor ledgerFileProcessor;

    public Rule37AuditRule(LedgerFileProcessor ledgerFileProcessor) {
        this.ledgerFileProcessor = ledgerFileProcessor;
    }

    @Override
    public String getRuleId() { return RULE_ID; }

    @Override
    public String getName() { return "Rule 37"; }

    @Override
    public String getDisplayName() { return DISPLAY_NAME; }

    @Override
    public String getDescription() {
        return "Identify invoices where payment is pending for >180 days to avoid ITC reversal with interest.";
    }

    @Override
    public String getCategory() { return "COMPLIANCE"; }

    @Override
    public String getLegalBasis() { return LEGAL_BASIS; }

    @Override
    public java.util.Set<com.learning.backendservice.engine.AnalysisMode> getApplicableModes() {
        return java.util.Set.of(com.learning.backendservice.engine.AnalysisMode.LEDGER_ANALYSIS);
    }

    @Override
    public java.util.Set<com.learning.backendservice.engine.DocumentType> getRequiredDocumentTypes() {
        return java.util.Set.of(com.learning.backendservice.engine.DocumentType.PURCHASE_LEDGER);
    }

    @Override
    public int getExecutionOrder() { return 100; }

    /**
     * 1 credit per distinct supplier/ledger in the uploaded files.
     * The orchestrator reads the actual count from the result.
     */
    @Override
    public int getCreditsRequired() { return 1; }

    @Override
    public AuditRuleResult<List<LedgerResult>> execute(
            List<MultipartFile> files, AuditContext context) {

        List<LedgerResult> results = new ArrayList<>();
        List<AuditFinding> findings = new ArrayList<>();
        int totalLedgerCount = 0;
        OffsetDateTime now = OffsetDateTime.now();

        for (MultipartFile file : files) {
            log.debug("Rule37AuditRule processing file={}, tenantId={}, userId={}",
                    file.getOriginalFilename(), context.tenantId(), context.userId());
            try {
                var outcome = ledgerFileProcessor.processWithLedgerCount(
                        file.getInputStream(),
                        file.getOriginalFilename(),
                        context.asOnDate());

                LedgerResult result = outcome.result();
                results.add(result);
                totalLedgerCount += outcome.ledgerCount();

                // ── Convert domain results to generic AuditFinding instances ──
                for (InterestRow row : result.getSummary().getDetails()) {
                    if (isSignificant(row)) {
                        findings.add(buildFinding(row, context, now));
                    }
                }

            } catch (LedgerParseException e) {
                log.warn("Rule37AuditRule: parse error in file={}: {}", file.getOriginalFilename(), e.getMessage());
                throw e;  // Propagate; orchestrator wraps in FileUploadError
            } catch (Exception e) {
                log.error("Rule37AuditRule: unexpected error processing file={}", file.getOriginalFilename(), e);
                throw new LedgerParseException("Processing failed for " + file.getOriginalFilename() + ": " + e.getMessage(), e);
            }
        }

        // Add a clean INFO finding if no issues detected
        if (findings.isEmpty() && !results.isEmpty()) {
            findings.add(AuditFinding.info(RULE_ID, LEGAL_BASIS,
                    "All " + totalLedgerCount + " ledger(s) reviewed. No ITC reversal required — all payments made within 180 days."));
        }

        BigDecimal totalImpact = results.stream()
                .map(r -> r.getSummary().getTotalItcReversal()
                          .add(r.getSummary().getTotalInterest()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Rule37AuditRule completed: tenantId={}, ledgers={}, findings={}, totalImpact={}",
                context.tenantId(), totalLedgerCount, findings.size(), totalImpact);

        return new AuditRuleResult<>(findings, results, totalImpact, totalLedgerCount);
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private boolean isSignificant(InterestRow row) {
        return row.getRiskCategory() == InterestRow.RiskCategory.BREACHED
                || row.getRiskCategory() == InterestRow.RiskCategory.AT_RISK
                || row.getInterest().compareTo(BigDecimal.ZERO) > 0;
    }

    private AuditFinding buildFinding(InterestRow row, AuditContext ctx, OffsetDateTime now) {
        AuditFinding.Severity severity =
                AuditFinding.Severity.fromRiskCategory(row.getRiskCategory().name());

        String description = buildDescription(row);
        String recommended = buildRecommendation(row);

        return new AuditFinding(
                RULE_ID,
                severity,
                LEGAL_BASIS,
                row.getGstr3bPeriod(),
                row.getItcAmount().add(row.getInterest()),
                description,
                recommended,
                false  // auto-fix not yet implemented
        );
    }

    private String buildDescription(InterestRow row) {
        return String.format(
                "Supplier: %s | Invoice %s (₹%.2f) | ITC: ₹%.2f | Status: %s | Days overdue: %d",
                row.getSupplier(),
                row.getInvoiceNumber() != null ? row.getInvoiceNumber() : "N/A",
                row.getOriginalInvoiceValue(),
                row.getItcAmount(),
                row.getStatus().name(),
                Math.abs(row.getDaysToDeadline())
        );
    }

    private String buildRecommendation(InterestRow row) {
        return switch (row.getRiskCategory()) {
            case BREACHED -> String.format(
                    "Reverse ITC of ₹%.2f in GSTR-3B Table 4(B)(2) for period %s. "
                    + "Pay interest of ₹%.2f u/s 50(1) CGST Act. "
                    + "Reclaim ITC in Table 4(A)(5) upon payment to supplier.",
                    row.getItcAmount(), row.getGstr3bPeriod(), row.getInterest());
            case AT_RISK -> String.format(
                    "Payment deadline is %s (%d days remaining). "
                    + "Ensure payment of ₹%.2f to supplier before deadline to avoid ITC reversal.",
                    row.getPaymentDeadline(), row.getDaysToDeadline(), row.getOriginalInvoiceValue());
            default -> "No action required.";
        };
    }
}
