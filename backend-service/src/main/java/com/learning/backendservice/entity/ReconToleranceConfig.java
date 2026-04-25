package com.learning.backendservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Per-tenant reconciliation tolerance configuration.
 *
 * <p>Loaded by {@code ContextEnricher} once per pipeline run.
 * Rules use the values via {@link com.learning.backendservice.engine.SharedResources}.
 *
 * <p>The tenant-id {@code "DEFAULT"} acts as the system-wide fallback row seeded
 * by Flyway migration {@code V6__recon_config.sql}.
 */
@Entity
@Table(
    name = "recon_tolerance_config",
    uniqueConstraints = @UniqueConstraint(name = "uq_recon_tolerance_tenant", columnNames = "tenant_id")
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconToleranceConfig {

    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    /**
     * Delta within this absolute amount (₹) is classified as MATCH regardless of %.
     * Default: ₹1.00 (handles minor rounding differences on the GST portal).
     */
    @Column(name = "tolerance_amount", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal toleranceAmount = new BigDecimal("1.00");

    /**
     * Delta within this fraction of the GSTR-1 amount is classified as MATCH.
     * Default: 0.0001 = 0.01%.
     */
    @Column(name = "tolerance_percent", nullable = false, precision = 7, scale = 6)
    @Builder.Default
    private BigDecimal tolerancePercent = new BigDecimal("0.000100");

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
