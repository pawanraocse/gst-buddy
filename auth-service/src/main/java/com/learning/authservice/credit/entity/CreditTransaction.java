package com.learning.authservice.credit.entity;

import com.learning.common.tenant.TenantAware;
import com.learning.common.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Immutable audit record for every credit mutation.
 * Acts as an append-only ledger — never updated or deleted.
 */
@Entity
@Table(name = "credit_transactions", indexes = {
        @Index(name = "idx_txn_user", columnList = "user_id, created_at DESC"),
        @Index(name = "idx_txn_idempotency", columnList = "idempotency_key")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditTransaction implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    @Builder.Default
    private String tenantId = TenantContext.DEFAULT_TENANT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    /** Number of credits in this transaction (always positive). */
    @Column(nullable = false)
    private Integer credits;

    /** Wallet balance after this transaction was applied. */
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 30)
    private ReferenceType referenceType;

    /** External reference (e.g. plan name, analysis run ID, Razorpay order ID). */
    @Column(name = "reference_id", length = 255)
    private String referenceId;

    /** Unique key to prevent duplicate transactions. */
    @Column(name = "idempotency_key", unique = true, length = 255)
    private String idempotencyKey;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
