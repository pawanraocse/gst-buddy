package com.learning.backendservice.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InputResolverRegistry")
class InputResolverRegistryTest {

    static class ResolverA implements InputResolver<String> {
        @Override public String getRuleId() { return "RULE_A"; }
        @Override public String resolve(AuditContext ctx) { return "input-a"; }
    }

    static class ResolverB implements InputResolver<Integer> {
        @Override public String getRuleId() { return "RULE_B"; }
        @Override public Integer resolve(AuditContext ctx) { return 42; }
    }

    static class DuplicateResolverA implements InputResolver<String> {
        @Override public String getRuleId() { return "RULE_A"; }
        @Override public String resolve(AuditContext ctx) { return "dupe"; }
    }

    @Test
    @DisplayName("Resolves correct typed resolver by ruleId")
    void resolvesTypedResolver() {
        var registry = new InputResolverRegistry(List.of(new ResolverA(), new ResolverB()));

        InputResolver<String> resolverA = registry.getResolver("RULE_A");
        assertNotNull(resolverA);
        assertEquals("RULE_A", resolverA.getRuleId());

        InputResolver<Integer> resolverB = registry.getResolver("RULE_B");
        assertEquals("RULE_B", resolverB.getRuleId());
    }

    @Test
    @DisplayName("Throws on missing ruleId")
    void throwsOnMissingRuleId() {
        var registry = new InputResolverRegistry(List.of(new ResolverA()));

        var ex = assertThrows(IllegalStateException.class,
                () -> registry.getResolver("UNKNOWN_RULE"));
        assertTrue(ex.getMessage().contains("No InputResolver registered"));
        assertTrue(ex.getMessage().contains("UNKNOWN_RULE"));
    }

    @Test
    @DisplayName("Throws on duplicate resolver for same ruleId")
    void throwsOnDuplicateResolver() {
        assertThrows(IllegalStateException.class,
                () -> new InputResolverRegistry(
                        List.of(new ResolverA(), new DuplicateResolverA())));
    }

    @Test
    @DisplayName("registeredRuleIds returns all registered IDs")
    void registeredRuleIdsContainsAll() {
        var registry = new InputResolverRegistry(List.of(new ResolverA(), new ResolverB()));
        var ids = registry.registeredRuleIds();
        assertTrue(ids.contains("RULE_A"));
        assertTrue(ids.contains("RULE_B"));
        assertEquals(2, ids.size());
    }

    @Test
    @DisplayName("Empty registry has no registered IDs")
    void emptyRegistryHasNoIds() {
        var registry = new InputResolverRegistry(List.of());
        assertTrue(registry.registeredRuleIds().isEmpty());
    }
}
