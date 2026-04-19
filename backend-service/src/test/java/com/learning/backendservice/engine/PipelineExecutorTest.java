package com.learning.backendservice.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PipelineExecutor")
class PipelineExecutorTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    static AuditContext ctx() {
        return AuditContext.forAnalysis(
                "T1", "U1", LocalDate.of(2025, 3, 31),
                AnalysisMode.GSTR_RULES_ANALYSIS,
                List.of(new AuditDocument(DocumentType.GSTR_1, "f.json", null, Map.of(), null, null)),
                AuditUserParams.defaults(),
                SharedResources.empty());
    }

    /** Rule that succeeds and returns a single finding. */
    static class SuccessfulRule implements AuditRule<String, String> {
        private final String ruleId;
        private final int order;
        private final BigDecimal impact;

        SuccessfulRule(String ruleId, int order, BigDecimal impact) {
            this.ruleId = ruleId; this.order = order; this.impact = impact;
        }

        @Override public String getRuleId() { return ruleId; }
        @Override public String getName() { return ruleId; }
        @Override public String getDisplayName() { return ruleId; }
        @Override public String getDescription() { return "ok"; }
        @Override public String getCategory() { return "TEST"; }
        @Override public String getLegalBasis() { return "Section X"; }
        @Override public int getExecutionOrder() { return order; }
        @Override public Set<AnalysisMode> getApplicableModes() {
            return Set.of(AnalysisMode.GSTR_RULES_ANALYSIS);
        }
        @Override
        public AuditRuleResult<String> execute(String input, AuditContext context) {
            var finding = new AuditFinding(ruleId, AuditFinding.Severity.HIGH,
                    "Section X", "FY 2024-25", impact, "desc", "rec", false);
            return new AuditRuleResult<>(List.of(finding), "result", impact, 1);
        }
    }

    /** Rule that always throws. */
    static class FailingRule implements AuditRule<String, String> {
        @Override public String getRuleId() { return "FAILING_RULE"; }
        @Override public String getName() { return "fail"; }
        @Override public String getDisplayName() { return "Failing Rule"; }
        @Override public String getDescription() { return "always fails"; }
        @Override public String getCategory() { return "TEST"; }
        @Override public String getLegalBasis() { return "N/A"; }
        @Override
        public AuditRuleResult<String> execute(String input, AuditContext context) {
            throw new RuntimeException("Rule execution failed intentionally");
        }
    }

    /** InputResolver that always returns a fixed string. */
    static class FixedStringResolver implements InputResolver<String> {
        private final String ruleId;
        FixedStringResolver(String ruleId) { this.ruleId = ruleId; }
        @Override public String getRuleId() { return ruleId; }
        @Override public String resolve(AuditContext context) { return "resolved-input"; }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Executes a single rule successfully")
    void executesRuleSuccessfully() {
        var rule = new SuccessfulRule("RULE_A", 10, new BigDecimal("5000.00"));
        var registry = new InputResolverRegistry(List.of(new FixedStringResolver("RULE_A")));
        var executor = new PipelineExecutor(registry);

        PipelineResult result = executor.execute(List.of(rule), ctx());

        assertEquals(1, result.rulesExecuted().size());
        assertEquals("RULE_A", result.rulesExecuted().get(0));
        assertEquals(1, result.allFindings().size());
        assertEquals(0, new BigDecimal("5000.00").compareTo(result.totalImpact()));

        RuleExecutionResult rr = result.ruleResults().get(0);
        assertEquals("SUCCESS", rr.status());
        assertNull(rr.errorMessage());
        assertTrue(rr.durationMs() >= 0);
    }

    @Test
    @DisplayName("Failed rule does not abort pipeline — other rules still execute")
    void failedRuleDoesNotAbortPipeline() {
        var ruleA = new SuccessfulRule("RULE_A", 10, new BigDecimal("1000.00"));
        var failingRule = new FailingRule();
        var ruleC = new SuccessfulRule("RULE_C", 30, new BigDecimal("2000.00"));

        var registry = new InputResolverRegistry(List.of(
                new FixedStringResolver("RULE_A"),
                new FixedStringResolver("FAILING_RULE"),
                new FixedStringResolver("RULE_C")));
        var executor = new PipelineExecutor(registry);

        PipelineResult result = executor.execute(List.of(ruleA, failingRule, ruleC), ctx());

        // All 3 attempted
        assertEquals(3, result.rulesExecuted().size());

        // Only 2 findings (from RULE_A and RULE_C)
        assertEquals(2, result.allFindings().size());

        // Total impact = 1000 + 2000 (FAILING_RULE contributes zero)
        assertEquals(0, new BigDecimal("3000.00").compareTo(result.totalImpact()));

        // Check per-rule statuses
        var statuses = result.ruleResults().stream()
                .collect(java.util.stream.Collectors.toMap(
                        RuleExecutionResult::ruleId, RuleExecutionResult::status));
        assertEquals("SUCCESS", statuses.get("RULE_A"));
        assertEquals("FAILED", statuses.get("FAILING_RULE"));
        assertEquals("SUCCESS", statuses.get("RULE_C"));
    }

    @Test
    @DisplayName("Failed rule records error message")
    void failedRuleRecordsErrorMessage() {
        var failingRule = new FailingRule();
        var registry = new InputResolverRegistry(List.of(
                new FixedStringResolver("FAILING_RULE")));
        var executor = new PipelineExecutor(registry);

        PipelineResult result = executor.execute(List.of(failingRule), ctx());

        assertEquals(1, result.ruleResults().size());
        RuleExecutionResult rr = result.ruleResults().get(0);
        assertEquals("FAILED", rr.status());
        assertNotNull(rr.errorMessage());
        assertTrue(rr.errorMessage().contains("intentionally"));
        assertEquals(0, BigDecimal.ZERO.compareTo(rr.impact()));
    }

    @Test
    @DisplayName("Empty rule list returns empty pipeline result")
    void emptyRuleListReturnsEmptyResult() {
        var executor = new PipelineExecutor(new InputResolverRegistry(List.of()));
        PipelineResult result = executor.execute(List.of(), ctx());

        assertTrue(result.rulesExecuted().isEmpty());
        assertTrue(result.allFindings().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.totalImpact()));
    }

    @Test
    @DisplayName("Total impact aggregates across all successful rules")
    void totalImpactAggregates() {
        var rule1 = new SuccessfulRule("R1", 10, new BigDecimal("1234.50"));
        var rule2 = new SuccessfulRule("R2", 20, new BigDecimal("5678.25"));
        var registry = new InputResolverRegistry(List.of(
                new FixedStringResolver("R1"), new FixedStringResolver("R2")));
        var executor = new PipelineExecutor(registry);

        PipelineResult result = executor.execute(List.of(rule1, rule2), ctx());

        BigDecimal expected = new BigDecimal("6912.75");
        assertEquals(0, expected.compareTo(result.totalImpact()));
    }
}
