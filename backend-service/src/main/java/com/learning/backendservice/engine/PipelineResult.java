package com.learning.backendservice.engine;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated output from a full pipeline execution across all resolved rules.
 *
 * @param rulesExecuted list of rule IDs that were attempted (SUCCESS or FAILED)
 * @param ruleResults   per-rule execution details (status, findings, duration, errors)
 * @param allFindings   flat list of all {@link AuditFinding} instances from all successful rules
 * @param totalImpact   sum of {@link RuleExecutionResult#impact()} across successful rules (INR)
 */
public record PipelineResult(
        List<String> rulesExecuted,
        List<RuleExecutionResult> ruleResults,
        List<AuditFinding> allFindings,
        BigDecimal totalImpact
) {}
