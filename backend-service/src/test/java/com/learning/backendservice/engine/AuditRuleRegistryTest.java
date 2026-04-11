package com.learning.backendservice.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditRuleRegistry")
class AuditRuleRegistryTest {

    static class DummyRuleA implements AuditRule<String, String> {
        @Override public String getRuleId() { return "RULE_A"; }
        @Override public String getName() { return "Rule A"; }
        @Override public String getDisplayName() { return "Rule A Display"; }
        @Override public String getDescription() { return "Description A"; }
        @Override public String getCategory() { return "CAT_A"; }
        @Override public String getLegalBasis() { return "None"; }
        @Override public AuditRuleResult<String> execute(String input, AuditContext ctx) {
            return new AuditRuleResult<>(List.of(), input, BigDecimal.ZERO, 1);
        }
    }

    static class DummyRuleB implements AuditRule<String, String> {
        @Override public String getRuleId() { return "RULE_B"; }
        @Override public String getName() { return "Rule B"; }
        @Override public String getDisplayName() { return "Rule B Display"; }
        @Override public String getDescription() { return "Description B"; }
        @Override public String getCategory() { return "CAT_B"; }
        @Override public String getLegalBasis() { return "None"; }
        @Override public AuditRuleResult<String> execute(String input, AuditContext ctx) {
            return new AuditRuleResult<>(List.of(), input, BigDecimal.ZERO, 1);
        }
    }

    static class DuplicateRuleA extends DummyRuleA {}

    @Test
    @DisplayName("Should register multiple unique rules successfully")
    void shouldRegisterUniqueRules() {
        AuditRuleRegistry registry = new AuditRuleRegistry(List.of(new DummyRuleA(), new DummyRuleB()));
        
        assertTrue(registry.hasRule("RULE_A"));
        assertTrue(registry.hasRule("RULE_B"));
        assertEquals(2, registry.getAllRules().size());
        
        AuditRule<String, String> ruleA = registry.getRule("RULE_A");
        assertNotNull(ruleA);
        assertEquals("Rule A Display", ruleA.getDisplayName());
    }

    @Test
    @DisplayName("Should throw on duplicate rule IDs")
    void shouldThrowOnDuplicateRules() {
        var ex = assertThrows(IllegalStateException.class, 
                () -> new AuditRuleRegistry(List.of(new DummyRuleA(), new DuplicateRuleA())));
        
        assertTrue(ex.getMessage().contains("Duplicate AuditRule ID detected"));
        assertTrue(ex.getMessage().contains("RULE_A"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when getting unknown rule")
    void shouldThrowOnUnknownRule() {
        AuditRuleRegistry registry = new AuditRuleRegistry(List.of(new DummyRuleA()));
        
        var ex = assertThrows(IllegalArgumentException.class, () -> registry.getRule("UNKNOWN"));
        assertTrue(ex.getMessage().contains("Unknown audit rule"));
    }
}
