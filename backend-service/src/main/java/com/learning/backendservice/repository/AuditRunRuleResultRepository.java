package com.learning.backendservice.repository;

import com.learning.backendservice.entity.AuditRunRuleResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for per-rule execution results within an audit pipeline run.
 */
@Repository
public interface AuditRunRuleResultRepository extends JpaRepository<AuditRunRuleResult, UUID> {

    /** All rule results for a given run, scoped to tenant. */
    List<AuditRunRuleResult> findByAuditRunIdAndTenantId(UUID runId, String tenantId);
}
