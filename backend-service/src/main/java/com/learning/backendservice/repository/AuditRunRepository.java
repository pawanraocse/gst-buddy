package com.learning.backendservice.repository;

import com.learning.backendservice.entity.AuditRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link AuditRun} entities.
 *
 * <p>All queries are scoped to {@code tenantId} for row-level multi-tenant isolation.
 * User-facing API methods are additionally scoped to {@code userId} to prevent
 * cross-user data leakage within the same tenant.
 */
@Repository
public interface AuditRunRepository extends JpaRepository<AuditRun, UUID> {

    /** Tenant-only query — for scheduler/admin use only. Do NOT use for user-facing APIs. */
    Page<AuditRun> findByTenantId(String tenantId, Pageable pageable);

    /** User-scoped list: returns only runs created by the given user within the tenant. */
    Page<AuditRun> findByTenantIdAndUserId(String tenantId, String userId, Pageable pageable);

    /** User-scoped list filtered by ruleId. */
    Page<AuditRun> findByTenantIdAndUserIdAndRuleId(String tenantId, String userId, String ruleId, Pageable pageable);

    /** User-scoped get: returns run only if it belongs to the given user AND tenant. */
    Optional<AuditRun> findByIdAndTenantIdAndUserId(UUID id, String tenantId, String userId);

    /** Tenant-only get — for export/admin only, never call from user-facing list endpoints. */
    Optional<AuditRun> findByIdAndTenantId(UUID id, String tenantId);

    /** User-scoped existence check (used before delete to avoid info-disclosure). */
    boolean existsByIdAndTenantIdAndUserId(UUID id, String tenantId, String userId);

    /** Count active runs per tenant (used to enforce maxRunsPerTenant limit). */
    long countByTenantId(String tenantId);

    /**
     * Purge expired runs across all tenants.
     * Called by {@link com.learning.backendservice.scheduler.RetentionScheduler}.
     */
    @Modifying
    @Query("DELETE FROM AuditRun r WHERE r.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(OffsetDateTime cutoff);
}
