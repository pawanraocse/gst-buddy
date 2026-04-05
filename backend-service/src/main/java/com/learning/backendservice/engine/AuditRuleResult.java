package com.learning.backendservice.engine;

import java.math.BigDecimal;
import java.util.List;

/**
 * Standardized result returned by every {@link AuditRule#execute} invocation.
 *
 * <p>The orchestrator uses this to:
 * <ul>
 *   <li>Persist {@link AuditFinding} instances to {@code audit_findings} table
 *   <li>Store {@code ruleSpecificOutput} as JSONB in {@code audit_runs.result_data}
 *   <li>Record the {@code totalImpact} in {@code audit_runs.total_impact_amount}
 *   <li>Consume {@code creditsConsumed} credits via {@code CreditClient}
 * </ul>
 *
 * @param <O>                rule-specific output type (serialized to JSON for storage)
 * @param findings           individual compliance findings (persisted to audit_findings)
 * @param ruleSpecificOutput rule-specific result object (e.g., List&lt;LedgerResult&gt; for Rule 37)
 * @param totalImpact        aggregate financial impact in INR (ITC reversal + interest + penalties)
 * @param creditsConsumed    actual credits consumed (may differ from AuditRule.getCreditsRequired())
 */
public record AuditRuleResult<O>(
        List<AuditFinding> findings,
        O ruleSpecificOutput,
        BigDecimal totalImpact,
        int creditsConsumed
) {
    /**
     * Convenience factory for a successful zero-impact result
     * (e.g., all invoices paid on time — no ITC reversal required).
     */
    public static <O> AuditRuleResult<O> clean(O output, int credits) {
        return new AuditRuleResult<>(List.of(), output, BigDecimal.ZERO, credits);
    }
}
