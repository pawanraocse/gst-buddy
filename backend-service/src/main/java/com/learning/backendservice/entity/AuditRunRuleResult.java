package com.learning.backendservice.entity;

import com.learning.common.infra.tenant.TenantAuditingListener;
import com.learning.common.tenant.TenantAware;
import com.learning.common.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity tracking the execution result of a single {@link com.learning.backendservice.engine.AuditRule}
 * within a pipeline {@link AuditRun}.
 *
 * <p>One {@code AuditRunRuleResult} per rule per run. A pipeline run with 5 applicable rules
 * produces 5 rows, each recording its own status, impact, duration, and error (if any).
 */
@Entity
@Table(name = "audit_run_rule_results")
@EntityListeners(TenantAuditingListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditRunRuleResult implements TenantAware {

    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private AuditRun auditRun;

    @Column(name = "tenant_id", nullable = false, length = 64)
    @Builder.Default
    private String tenantId = TenantContext.DEFAULT_TENANT;

    @Column(name = "rule_id", nullable = false, length = 100)
    private String ruleId;

    @Column(name = "rule_name", length = 200)
    private String ruleName;

    @Column(name = "legal_basis")
    private String legalBasis;

    /** SUCCESS | FAILED */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "SUCCESS";

    @Column(name = "impact_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal impactAmount = BigDecimal.ZERO;

    @Column(name = "findings_count")
    @Builder.Default
    private Integer findingsCount = 0;

    @Column(name = "execution_duration_ms")
    private Integer executionDurationMs;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
