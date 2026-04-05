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
 */
@Repository
public interface AuditRunRepository extends JpaRepository<AuditRun, UUID> {

    /** List runs for tenant (paginated, ordered by created_at DESC via Pageable sort). */
    Page<AuditRun> findByTenantId(String tenantId, Pageable pageable);

    /** List runs for tenant filtered by ruleId. */
    Page<AuditRun> findByTenantIdAndRuleId(String tenantId, String ruleId, Pageable pageable);

    /** Fetch run only if it belongs to the specified tenant. */
    Optional<AuditRun> findByIdAndTenantId(UUID id, String tenantId);

    /** Tenant-scoped existence check (used before delete to return 404 vs 403). */
    boolean existsByIdAndTenantId(UUID id, String tenantId);

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
