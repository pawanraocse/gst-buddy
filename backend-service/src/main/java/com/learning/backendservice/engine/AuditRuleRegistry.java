package com.learning.backendservice.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Auto-discovers and indexes all {@link AuditRule} Spring beans at startup.
 *
 * <p>Any class annotated with {@code @Component} (or {@code @Service}) that
 * implements {@link AuditRule} will be automatically picked up by Spring and
 * injected into this registry via constructor injection.
 *
 * <p><b>To add a new GST audit rule:</b>
 * <ol>
 *   <li>Implement {@link AuditRule}&lt;I, O&gt;
 *   <li>Annotate with {@code @Component}
 *   <li>Ensure {@link AuditRule#getRuleId()} returns a unique constant
 * </ol>
 * That's it — no further registration step required.
 *
 * <p><b>Startup validation:</b> Duplicate {@code ruleId}s throw
 * {@link IllegalStateException} at context refresh time, ensuring invalid
 * configurations are caught before the service accepts traffic.
 */
@Component
public class AuditRuleRegistry {

    private static final Logger log = LoggerFactory.getLogger(AuditRuleRegistry.class);

    /** Insertion-ordered map preserving the natural Spring bean order. */
    private final Map<String, AuditRule<?, ?>> rules;

    public AuditRuleRegistry(List<AuditRule<?, ?>> ruleList) {
        Map<String, AuditRule<?, ?>> map = new LinkedHashMap<>();
        for (AuditRule<?, ?> rule : ruleList) {
            String id = rule.getRuleId();
            if (map.containsKey(id)) {
                throw new IllegalStateException(
                        "Duplicate AuditRule ID detected: '" + id
                        + "'. Each AuditRule must have a globally unique getRuleId().");
            }
            map.put(id, rule);
            log.info("AuditRule registered: id={}, displayName={}, credits={}",
                    id, rule.getDisplayName(), rule.getCreditsRequired());
        }
        this.rules = Collections.unmodifiableMap(map);
        log.info("AuditRuleRegistry initialized with {} rule(s): {}", rules.size(), rules.keySet());
    }

    /**
     * Retrieve a rule by its unique ID.
     *
     * @param ruleId the value returned by {@link AuditRule#getRuleId()}
     * @return the matching rule cast to the expected generic types
     * @throws IllegalArgumentException if no rule is registered with the given ID
     */
    @SuppressWarnings("unchecked")
    public <I, O> AuditRule<I, O> getRule(String ruleId) {
        AuditRule<?, ?> rule = rules.get(ruleId);
        if (rule == null) {
            throw new IllegalArgumentException(
                    "Unknown audit rule: '" + ruleId
                    + "'. Available rules: " + rules.keySet());
        }
        return (AuditRule<I, O>) rule;
    }

    /**
     * All registered rules in registration order.
     * Used by the {@code /api/v1/audit/rules} catalog endpoint.
     */
    public Collection<AuditRule<?, ?>> getAllRules() {
        return rules.values(); // already unmodifiable
    }

    /** True if a rule with the given ID is registered. */
    public boolean hasRule(String ruleId) {
        return rules.containsKey(ruleId);
    }
}
