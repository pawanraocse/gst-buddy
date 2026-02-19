package com.learning.authservice.credit.entity;

import com.learning.common.tenant.TenantAware;
import com.learning.common.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Per-user credit wallet. Uses optimistic locking ({@code @Version})
 * to prevent concurrent over-spend of credits.
 */
@Entity
@Table(name = "user_credit_wallets", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "tenant_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreditWallet implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    @Builder.Default
    private String tenantId = TenantContext.DEFAULT_TENANT;

    @Column(name = "total_credits", nullable = false)
    @Builder.Default
    private Integer totalCredits = 0;

    @Column(name = "consumed_credits", nullable = false)
    @Builder.Default
    private Integer consumedCredits = 0;

    @Column(name = "has_used_trial", nullable = false)
    @Builder.Default
    private Boolean hasUsedTrial = false;

    /** Optimistic locking version — prevents concurrent double-spend. */
    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ---- Derived ----

    /**
     * Remaining credits = total - consumed.
     */
    public int getRemainingCredits() {
        return totalCredits - consumedCredits;
    }

    /**
     * Grant credits to this wallet.
     */
    public void addCredits(int credits) {
        this.totalCredits += credits;
    }

    /**
     * Consume credits from this wallet.
     * 
     * @throws IllegalStateException if insufficient balance
     */
    public void deductCredits(int credits) {
        if (getRemainingCredits() < credits) {
            throw new IllegalStateException(
                    "Insufficient credits: required=" + credits + ", remaining=" + getRemainingCredits());
        }
        this.consumedCredits += credits;
    }
}
