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

    Page<Rule37CalculationRun> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    Optional<Rule37CalculationRun> findByIdAndTenantId(Long id, String tenantId);

    boolean existsByIdAndTenantId(Long id, String tenantId);

    /** Used by {@link com.learning.backendservice.scheduler.RetentionScheduler} to purge expired runs. */
    int deleteByExpiresAtBefore(OffsetDateTime cutoff);

    /** Used to enforce per-tenant saved calculation limits. */
    long countByTenantId(String tenantId);
}
