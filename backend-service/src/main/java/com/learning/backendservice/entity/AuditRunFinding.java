package com.learning.backendservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity for an individual compliance finding within an audit run.
 *
 * <p>A single {@link AuditRun} can produce 0..N findings.
 * Cascade-deleted when the parent AuditRun is deleted.
 */
@Entity
@Table(name = "audit_findings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditRunFinding {

    /** UUID v7 — generated in Java service layer */
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false, updatable = false)
    private AuditRun auditRun;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    /** Mirrors the parent run's ruleId for direct querying without join */
    @Column(name = "rule_id", nullable = false, length = 100)
    private String ruleId;

    /** CRITICAL | HIGH | MEDIUM | LOW | INFO */
    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    /** e.g. "Section 16(2) proviso, Rule 37 CGST Rules, 2017" */
    @Column(name = "legal_basis", columnDefinition = "text")
    private String legalBasis;

    /** e.g. "FY: 2024-25, Tax Period: Apr-2024" */
    @Column(name = "compliance_period", length = 50)
    private String compliancePeriod;

    /** Financial impact of this specific finding (₹) */
    @Column(name = "impact_amount", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal impactAmount = BigDecimal.ZERO;

    /** Human-readable description of the compliance issue */
    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    /** Actionable advice: what the taxpayer should do to remediate */
    @Column(name = "recommended_action", columnDefinition = "text")
    private String recommendedAction;

    /** True if the system can automatically generate the corrective filing entry */
    @Column(name = "auto_fix_available", nullable = false)
    @Builder.Default
    private Boolean autoFixAvailable = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
