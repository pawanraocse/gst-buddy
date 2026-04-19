package com.learning.backendservice.engine;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Spring-managed registry of all {@link InputResolver} implementations.
 *
 * <p>Discovers all {@code @Component} implementations of {@link InputResolver} at startup
 * via constructor injection. The {@link PipelineExecutor} calls
 * {@link #getResolver(String)} before executing each rule.
 *
 * <p>Fails fast at startup if two resolvers claim the same {@code ruleId}.
 */
@Component
public class InputResolverRegistry {

    private final Map<String, InputResolver<?>> resolvers;

    public InputResolverRegistry(List<InputResolver<?>> resolverList) {
        this.resolvers = resolverList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        InputResolver::getRuleId,
                        Function.identity(),
                        (a, b) -> { throw new IllegalStateException(
                                "Duplicate InputResolver for ruleId: " + a.getRuleId()); }
                ));
    }

    /**
     * Returns the typed resolver for the given rule ID.
     *
     * @param ruleId the rule ID to look up
     * @param <I>    the input type inferred by the caller
     * @return the matching resolver
     * @throws IllegalStateException if no resolver is registered for this rule ID
     */
    @SuppressWarnings("unchecked")
    public <I> InputResolver<I> getResolver(String ruleId) {
        InputResolver<?> resolver = resolvers.get(ruleId);
        if (resolver == null) {
            throw new IllegalStateException(
                    "No InputResolver registered for ruleId: " + ruleId
                    + ". Register a @Component implementing InputResolver<I> with getRuleId()=\"" + ruleId + "\"");
        }
        return (InputResolver<I>) resolver;
    }

    /** Returns all registered rule IDs — useful for health-check / diagnostics. */
    public java.util.Set<String> registeredRuleIds() {
        return resolvers.keySet();
    }
}
