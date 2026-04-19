package com.learning.backendservice.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RuleResolutionEngine")
class RuleResolutionEngineTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    static AuditContext ledgerCtx() {
        return AuditContext.forAnalysis(
                "T1", "U1", LocalDate.of(2025, 3, 31),
                AnalysisMode.LEDGER_ANALYSIS,
                List.of(doc(DocumentType.PURCHASE_LEDGER)),
                AuditUserParams.defaults(),
                SharedResources.empty());
    }

    static AuditContext gstrCtxWith(DocumentType... types) {
        List<AuditDocument> docs = java.util.Arrays.stream(types)
                .map(RuleResolutionEngineTest::doc)
                .toList();
        return AuditContext.forAnalysis(
                "T1", "U1", LocalDate.of(2025, 3, 31),
                AnalysisMode.GSTR_RULES_ANALYSIS,
                docs,
                AuditUserParams.defaults(),
                SharedResources.empty());
    }

    static AuditDocument doc(DocumentType type) {
        return new AuditDocument(type, "test.xlsx", null, java.util.Map.of(), null, null);
    }

    /** Rule that requires GSTR_1 and runs in GSTR_RULES_ANALYSIS at order 10. */
    static class Gstr1Rule extends StubRule<Object, Object> {
        @Override public String getRuleId() { return "GSTR1_RULE"; }
        @Override public int getExecutionOrder() { return 10; }
        @Override public Set<DocumentType> getRequiredDocumentTypes() {
            return Set.of(DocumentType.GSTR_1);
        }
        @Override public Set<AnalysisMode> getApplicableModes() {
            return Set.of(AnalysisMode.GSTR_RULES_ANALYSIS);
        }
    }

    /** Rule that requires GSTR_3B and runs at order 20. */
    static class Gstr3bRule extends StubRule<Object, Object> {
        @Override public String getRuleId() { return "GSTR3B_RULE"; }
        @Override public int getExecutionOrder() { return 20; }
        @Override public Set<DocumentType> getRequiredDocumentTypes() {
            return Set.of(DocumentType.GSTR_3B);
        }
        @Override public Set<AnalysisMode> getApplicableModes() {
            return Set.of(AnalysisMode.GSTR_RULES_ANALYSIS);
        }
    }

    /** Rule that requires both GSTR_1 and GSTR_3B. */
    static class Gstr1And3bRule extends StubRule<Object, Object> {
        @Override public String getRuleId() { return "GSTR1_3B_RULE"; }
        @Override public int getExecutionOrder() { return 30; }
        @Override public Set<DocumentType> getRequiredDocumentTypes() {
            return Set.of(DocumentType.GSTR_1, DocumentType.GSTR_3B);
        }
        @Override public Set<AnalysisMode> getApplicableModes() {
            return Set.of(AnalysisMode.GSTR_RULES_ANALYSIS);
        }
    }

    /** Ledger rule (Rule 37 style). */
    static class LedgerRule extends StubRule<Object, Object> {
        @Override public String getRuleId() { return "LEDGER_RULE"; }
        @Override public int getExecutionOrder() { return 100; }
        @Override public Set<DocumentType> getRequiredDocumentTypes() {
            return Set.of(DocumentType.PURCHASE_LEDGER);
        }
        @Override public Set<AnalysisMode> getApplicableModes() {
            return Set.of(AnalysisMode.LEDGER_ANALYSIS);
        }
    }

    AuditRuleRegistry registryOf(AuditRule<?, ?>... rules) {
        return new AuditRuleRegistry(List.of(rules));
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Resolves only rules matching the analysis mode")
    void resolvesOnlyMatchingMode() {
        var registry = registryOf(new Gstr1Rule(), new LedgerRule());
        var engine = new RuleResolutionEngine(registry);

        var ledgerRules = engine.resolveExecutableRules(ledgerCtx());
        assertEquals(1, ledgerRules.size());
        assertEquals("LEDGER_RULE", ledgerRules.get(0).getRuleId());

        var gstrRules = engine.resolveExecutableRules(gstrCtxWith(DocumentType.GSTR_1));
        assertEquals(1, gstrRules.size());
        assertEquals("GSTR1_RULE", gstrRules.get(0).getRuleId());
    }

    @Test
    @DisplayName("Excludes rules with missing required document types")
    void excludesMissingDocTypes() {
        // Only GSTR_1 uploaded — GSTR3B_RULE should not resolve
        var registry = registryOf(new Gstr1Rule(), new Gstr3bRule());
        var engine = new RuleResolutionEngine(registry);

        var rules = engine.resolveExecutableRules(gstrCtxWith(DocumentType.GSTR_1));
        assertEquals(1, rules.size());
        assertEquals("GSTR1_RULE", rules.get(0).getRuleId());
    }

    @Test
    @DisplayName("Resolves multi-doc rules when all required types present")
    void resolvesMultiDocRule() {
        var registry = registryOf(new Gstr1Rule(), new Gstr1And3bRule());
        var engine = new RuleResolutionEngine(registry);

        var rules = engine.resolveExecutableRules(
                gstrCtxWith(DocumentType.GSTR_1, DocumentType.GSTR_3B));
        assertEquals(2, rules.size());
    }

    @Test
    @DisplayName("Returned rules are sorted by executionOrder ascending")
    void returnsSortedByOrder() {
        var registry = registryOf(new Gstr3bRule(), new Gstr1Rule()); // intentionally reversed
        var engine = new RuleResolutionEngine(registry);

        var rules = engine.resolveExecutableRules(
                gstrCtxWith(DocumentType.GSTR_1, DocumentType.GSTR_3B));
        assertEquals(2, rules.size());
        assertEquals("GSTR1_RULE", rules.get(0).getRuleId());   // order=10
        assertEquals("GSTR3B_RULE", rules.get(1).getRuleId());  // order=20
    }

    @Test
    @DisplayName("Returns empty list when no rules match mode + doc types")
    void returnsEmptyWhenNoMatch() {
        var registry = registryOf(new Gstr1Rule());
        var engine = new RuleResolutionEngine(registry);

        var rules = engine.resolveExecutableRules(ledgerCtx());
        assertTrue(rules.isEmpty());
    }

    @Test
    @DisplayName("previewUnlockableRules returns rules blocked by missing docs")
    void previewsUnlockableRules() {
        var registry = registryOf(new Gstr1Rule(), new Gstr3bRule(), new Gstr1And3bRule());
        var engine = new RuleResolutionEngine(registry);

        // Only GSTR_1 uploaded — GSTR3B_RULE and GSTR1_3B_RULE should show as unlockable
        var unlockable = engine.previewUnlockableRules(gstrCtxWith(DocumentType.GSTR_1));
        assertEquals(2, unlockable.size());

        var allMissing = unlockable.stream()
                .flatMap(u -> u.missingDocuments().stream())
                .toList();
        assertTrue(allMissing.contains(DocumentType.GSTR_3B));
    }

    // ── Shared stub base class ───────────────────────────────────────────────

    abstract static class StubRule<I, O> implements AuditRule<I, O> {
        @Override public String getName() { return getRuleId(); }
        @Override public String getDisplayName() { return getRuleId(); }
        @Override public String getDescription() { return "stub"; }
        @Override public String getCategory() { return "TEST"; }
        @Override public String getLegalBasis() { return "stub"; }
        @Override @SuppressWarnings("unchecked")
        public AuditRuleResult<O> execute(I input, AuditContext ctx) {
            return new AuditRuleResult<>(List.of(), (O) "ok", BigDecimal.ZERO, 1);
        }
    }
}
