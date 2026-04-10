package com.learning.backendservice.repository;

import com.learning.backendservice.entity.Rule37CalculationRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface Rule37RunRepository extends JpaRepository<Rule37CalculationRun, Long> {

    /** Tenant-only query — kept for scheduler/admin use only. Do NOT use for user-facing APIs. */
    Page<Rule37CalculationRun> findByTenantId(String tenantId, Pageable pageable);

    /** User-scoped list: only returns runs created by the given user within the tenant. */
    Page<Rule37CalculationRun> findByTenantIdAndCreatedBy(String tenantId, String createdBy, Pageable pageable);

    /** User-scoped get: returns run only if it belongs to the given user AND tenant. */
    Optional<Rule37CalculationRun> findByIdAndTenantIdAndCreatedBy(Long id, String tenantId, String createdBy);

    /** User-scoped existence check (used before delete). */
    boolean existsByIdAndTenantIdAndCreatedBy(Long id, String tenantId, String createdBy);

    /** Used by {@link com.learning.backendservice.scheduler.RetentionScheduler} to purge expired runs. */
    int deleteByExpiresAtBefore(OffsetDateTime cutoff);

    /** Used to enforce per-tenant saved calculation limits. */
    long countByTenantId(String tenantId);
}
