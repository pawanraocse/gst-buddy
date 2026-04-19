package com.learning.backendservice.engine;

/**
 * Companion strategy to {@link AuditRule} that extracts typed rule input from {@link AuditContext}.
 *
 * <p>Each {@code AuditRule<I, O>} has exactly one companion {@code InputResolver<I>} registered
 * as a Spring {@code @Component}. The {@link PipelineExecutor} calls
 * {@code resolver.resolve(context)} to obtain the typed input before calling
 * {@code rule.execute(input, context)}, preserving full type safety without raw casts in the core loop.
 *
 * <p>{@code InputResolver} implementations:
 * <ul>
 *   <li>May access {@code context.documents()}, {@code context.userParams()},
 *       {@code context.sharedResources()} — all pre-loaded, no DB calls.
 *   <li>Must NOT access the database or any Spring repository directly.
 *   <li>Must throw {@link IllegalStateException} with a clear message if a required
 *       document is missing (should not happen if {@link RuleResolutionEngine} runs first).
 * </ul>
 *
 * @param <I> the input type matching the companion {@code AuditRule<I, O>}
 */
public interface InputResolver<I> {

    /**
     * The rule ID this resolver serves.
     * Must exactly match the return value of the companion {@link AuditRule#getRuleId()}.
     */
    String getRuleId();

    /**
     * Build the typed rule input from the audit context.
     *
     * @param context current audit context with documents and pre-loaded resources
     * @return typed input ready to pass to {@link AuditRule#execute(Object, AuditContext)}
     * @throws IllegalStateException if a required document or param is absent
     */
    I resolve(AuditContext context);
}
