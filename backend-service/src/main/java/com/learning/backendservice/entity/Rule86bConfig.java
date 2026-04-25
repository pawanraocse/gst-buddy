package com.learning.backendservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Per-tenant configuration for Rule 86B (1% Cash Ledger restriction).
 */
@Entity
@Table(
    name = "rule_86b_config",
    uniqueConstraints = @UniqueConstraint(name = "uq_rule_86b_tenant", columnNames = "tenant_id")
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rule86bConfig {

    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "turnover_threshold", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal turnoverThreshold = new BigDecimal("5000000.00");

    @Column(name = "cash_percent_floor", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal cashPercentFloor = new BigDecimal("0.0100");

    @Column(name = "effective_from", nullable = false)
    @Builder.Default
    private LocalDate effectiveFrom = LocalDate.of(2021, 1, 1);

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
