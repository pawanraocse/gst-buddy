package com.learning.backendservice.repository;

import com.learning.backendservice.entity.AuditRunFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link AuditRunFinding} entities.
 */
@Repository
public interface AuditFindingRepository extends JpaRepository<AuditRunFinding, UUID> {

    /** All findings for a given run, in insertion order. */
    List<AuditRunFinding> findByAuditRunIdOrderByCreatedAtAsc(UUID runId);

    /** Findings for a run filtered by severity. */
    List<AuditRunFinding> findByAuditRunIdAndSeverityOrderByImpactAmountDesc(
            UUID runId, String severity);

    /** Count findings per severity for a tenant (used in dashboard summaries). */
    long countByTenantIdAndSeverity(String tenantId, String severity);
}
