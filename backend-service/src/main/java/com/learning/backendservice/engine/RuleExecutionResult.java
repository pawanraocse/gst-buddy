package com.learning.backendservice.engine;

import java.math.BigDecimal;
import java.util.List;

/**
 * Per-rule execution summary within a {@link PipelineResult}.
 *
 * @param ruleId        the unique rule identifier
 * @param ruleName      human-readable rule name
 * @param legalBasis    legal citation (e.g. "Section 47(1), CGST Act 2017")
 * @param status        "SUCCESS" or "FAILED"
 * @param findings      individual compliance findings from this rule (empty if FAILED)
 * @param impact        aggregate financial impact from this rule in INR (zero if FAILED)
 * @param durationMs    wall-clock execution time in milliseconds
 * @param errorMessage  error description if status is "FAILED"; {@code null} if SUCCESS
 */
public record RuleExecutionResult(
        String ruleId,
        String ruleName,
        String legalBasis,
        String status,
        List<AuditFinding> findings,
        BigDecimal impact,
        int durationMs,
        String errorMessage
) {
    /** Convenience factory for a failed rule result. */
    public static RuleExecutionResult failed(
            String ruleId, String name, String error, int durationMs) {
        return new RuleExecutionResult(
                ruleId, name, null, "FAILED",
                List.of(), BigDecimal.ZERO, durationMs, error);
    }
}
