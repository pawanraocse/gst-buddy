package com.learning.authservice.referral.entity;

import com.learning.common.infra.tenant.TenantAuditingListener;
import com.learning.common.tenant.TenantAware;
import com.learning.common.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Entity representing a referral record.
 *
 * <p>
 * Each user gets one seed row (status=ACTIVE, referee_user_id=null) when they
 * first request their referral code. When a new user signs up with that code,
 * a new CONVERTED row is created linking both users.
 * </p>
 *
 * <p>
 * Tenant isolation via TenantAuditingListener.
 * </p>
 */
@Entity
@Table(name = "referrals", indexes = {
        @Index(name = "idx_referrals_code", columnList = "referral_code"),
        @Index(name = "idx_referrals_referrer", columnList = "referrer_user_id"),
        @Index(name = "idx_referrals_tenant", columnList = "tenant_id")
})
@EntityListeners(TenantAuditingListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Referral implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "referrer_user_id", nullable = false, length = 255)
    private String referrerUserId;

    @Column(name = "referee_user_id", length = 255)
    private String refereeUserId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    @Builder.Default
    private String tenantId = TenantContext.DEFAULT_TENANT;

    @Column(name = "referral_code", nullable = false, length = 20)
    private String referralCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReferralStatus status = ReferralStatus.ACTIVE;

    @Column(name = "converted_at")
    private Instant convertedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
