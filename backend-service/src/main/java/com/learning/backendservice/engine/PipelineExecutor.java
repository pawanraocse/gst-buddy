package com.learning.backendservice.engine;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes resolved audit rules sequentially against the shared {@link AuditContext}.
 *
 * <p>Each rule is isolated — a failure in one rule does not abort the pipeline.
 * Failed rules produce a {@link RuleExecutionResult} with {@code status="FAILED"} and an
 * {@code errorMessage}; successful rules from the same run still complete normally.
 *
 * <p>Type safety is achieved via the {@link InputResolverRegistry}: the executor never
 * casts rule inputs directly. Instead it calls {@code resolver.resolve(context)} to obtain
 * the typed input, then passes it through the rule's raw-typed {@code execute()} method.
 * The single {@code @SuppressWarnings("unchecked")} cast is safe because the registry
 * guarantees resolver ↔ rule ID alignment.
 */
@Component
@RequiredArgsConstructor
public class PipelineExecutor {

    private static final Logger log = LoggerFactory.getLogger(PipelineExecutor.class);

    private final InputResolverRegistry inputResolverRegistry;

    /**
     * Execute all resolved rules against the given context in order.
     *
     * @param rules   rules to execute, sorted by {@link AuditRule#getExecutionOrder()}
     * @param context immutable audit context with pre-loaded documents and resources
     * @return aggregated pipeline result
     */
    public PipelineResult execute(List<AuditRule<?, ?>> rules, AuditContext context) {
        List<RuleExecutionResult> ruleResults = new ArrayList<>();
        List<AuditFinding> allFindings = new ArrayList<>();
        BigDecimal totalImpact = BigDecimal.ZERO;
        List<String> rulesExecuted = new ArrayList<>();

        for (AuditRule<?, ?> rule : rules) {
            long startMs = System.currentTimeMillis();
            String ruleId = rule.getRuleId();

            try {
                @SuppressWarnings("unchecked")
                InputResolver<Object> resolver = inputResolverRegistry.getResolver(ruleId);
                Object input = resolver.resolve(context);

                @SuppressWarnings("unchecked")
                AuditRuleResult<?> result = ((AuditRule<Object, ?>) rule).execute(input, context);

                int durationMs = (int) (System.currentTimeMillis() - startMs);

                ruleResults.add(new RuleExecutionResult(
                        ruleId, rule.getDisplayName(), rule.getLegalBasis(),
                        "SUCCESS", result.findings(), result.totalImpact(),
                        durationMs, null));

                allFindings.addAll(result.findings());
                totalImpact = totalImpact.add(result.totalImpact());
                rulesExecuted.add(ruleId);

                log.info("Pipeline rule={} status=SUCCESS durationMs={} findings={} impact={}",
                        ruleId, durationMs, result.findings().size(), result.totalImpact());

            } catch (Exception e) {
                int durationMs = (int) (System.currentTimeMillis() - startMs);
                log.error("Pipeline rule={} status=FAILED durationMs={} error={}",
                        ruleId, durationMs, e.getMessage(), e);

                ruleResults.add(RuleExecutionResult.failed(
                        ruleId, rule.getDisplayName(), e.getMessage(), durationMs));
                rulesExecuted.add(ruleId);
                // Continue — failure in one rule does not abort the pipeline
            }
        }

        return new PipelineResult(
                List.copyOf(rulesExecuted),
                List.copyOf(ruleResults),
                List.copyOf(allFindings),
                totalImpact);
    }
}
