package com.learning.backendservice.repository;

import com.learning.backendservice.BaseIntegrationTest;
import com.learning.backendservice.entity.AuditRun;
import com.learning.backendservice.entity.AuditRunFinding;
import com.learning.backendservice.util.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@DisplayName("AuditRunRepository Integration")
class AuditRunRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AuditRunRepository auditRunRepository;

    @Test
    @DisplayName("Should save and retrieve AuditRun with Findings (UUID v7 PKs)")
    void shouldSaveAndRetrieveAuditRunWithFindings() {
        // Create Run
        AuditRun run = new AuditRun();
        run.setId(UuidV7.generate());
        run.setTenantId("tenant-123");
        run.setRulesExecuted(new String[]{"DUMMY_RULE"});
        run.setAnalysisMode("LEDGER_ANALYSIS");
        run.setUserId("user-123");
        run.setCreatedAt(OffsetDateTime.now());
        run.setExpiresAt(OffsetDateTime.now().plusDays(7));
        run.setStatus("SUCCESS");
        run.setTotalImpactAmount(new BigDecimal("150.50"));
        AuditRunFinding finding = new AuditRunFinding();
        finding.setId(UuidV7.generate());
        finding.setAuditRun(run);
        finding.setTenantId(run.getTenantId());
        finding.setRuleId("DUMMY_RULE");
        finding.setSeverity("HIGH");
        finding.setImpactAmount(new BigDecimal("150.50"));
        finding.setDescription("Violation");
        finding.setAutoFixAvailable(false);
        finding.setCreatedAt(OffsetDateTime.now());
        
        run.setFindings(List.of(finding));

        // Save
        AuditRun saved = auditRunRepository.saveAndFlush(run);
        entityManager.clear(); // Ensure we fetch from DB, not L1 cache

        // Retrieve
        Optional<AuditRun> retrievedOpt = auditRunRepository.findByIdAndTenantId(saved.getId(), "tenant-123");
        assertTrue(retrievedOpt.isPresent());
        
        AuditRun retrieved = retrievedOpt.get();
        assertEquals("tenant-123", retrieved.getTenantId());
        assertArrayEquals(new String[]{"DUMMY_RULE"}, retrieved.getRulesExecuted());
        assertEquals(0, new BigDecimal("150.50").compareTo(retrieved.getTotalImpactAmount()));
        assertEquals(1, retrieved.getFindings().size());

        AuditRunFinding retrievedFinding = retrieved.getFindings().get(0);
        assertEquals("HIGH", retrievedFinding.getSeverity());
        assertEquals(0, new BigDecimal("150.50").compareTo(retrievedFinding.getImpactAmount()));
        assertEquals(run.getId(), retrievedFinding.getAuditRun().getId());
    }

    @Test
    @DisplayName("Should not return Run for another tenant")
    void shouldNotReturnRunForAnotherTenant() {
        AuditRun run = new AuditRun();
        run.setId(UuidV7.generate());
        run.setTenantId("tenant-A");
        run.setRulesExecuted(new String[]{"RULE_X"});
        run.setAnalysisMode("LEDGER_ANALYSIS");
        run.setUserId("user-a");
        run.setCreatedAt(OffsetDateTime.now());
        run.setExpiresAt(OffsetDateTime.now().plusDays(7));
        run.setStatus("SUCCESS");
        
        auditRunRepository.saveAndFlush(run);
        
        Optional<AuditRun> retrieved = auditRunRepository.findByIdAndTenantId(run.getId(), "tenant-B");
        assertTrue(retrieved.isEmpty());
    }

    @Test
    @DisplayName("Should find runs by Tenant ID ordered by CreatedAt Descending")
    void shouldFindByTenantIdOrdered() {
        AuditRun run1 = new AuditRun();
        run1.setId(UuidV7.generate());
        run1.setTenantId("tenant-list");
        run1.setStatus("SUCCESS");
        run1.setUserId("list-user");
        run1.setCreatedAt(OffsetDateTime.now());
        run1.setExpiresAt(OffsetDateTime.now().plusDays(7));
        run1.setRulesExecuted(new String[]{"RULE_A"});
        run1.setAnalysisMode("LEDGER_ANALYSIS");
        
        // Ensure UUID generation produces distinct timestamps by sleeping briefly
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        
        AuditRun run2 = new AuditRun();
        run2.setId(UuidV7.generate());
        run2.setTenantId("tenant-list");
        run2.setStatus("FAILED");
        run2.setUserId("list-user");
        run2.setCreatedAt(OffsetDateTime.now());
        run2.setExpiresAt(OffsetDateTime.now().plusDays(7));
        run2.setRulesExecuted(new String[]{"RULE_B"});
        run2.setAnalysisMode("LEDGER_ANALYSIS");

        auditRunRepository.saveAllAndFlush(List.of(run1, run2));

        List<AuditRun> runs = auditRunRepository.findByTenantId("tenant-list", org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))).getContent();
        
        assertEquals(2, runs.size());
        assertEquals(run2.getId(), runs.get(0).getId(), "Newest UUID v7 should be first");
        assertEquals(run1.getId(), runs.get(1).getId());
    }

    @Test
    @DisplayName("Should bulk delete expired runs and cascade findings")
    void shouldDeleteExpiredRuns() {
        // Run 1: Expired
        AuditRun expiredRun = new AuditRun();
        expiredRun.setId(UuidV7.generate());
        expiredRun.setTenantId("tenant-expire");
        expiredRun.setRulesExecuted(new String[]{"RULE_A"});
        expiredRun.setAnalysisMode("LEDGER_ANALYSIS");
        expiredRun.setUserId("expire-user");
        expiredRun.setCreatedAt(OffsetDateTime.now().minusDays(5));
        expiredRun.setStatus("SUCCESS");
        expiredRun.setExpiresAt(OffsetDateTime.now().minusDays(1)); // Expired yesterday
        
        AuditRunFinding finding1 = new AuditRunFinding();
        finding1.setId(UuidV7.generate());
        finding1.setAuditRun(expiredRun);
        finding1.setTenantId(expiredRun.getTenantId());
        finding1.setRuleId("RULE_A");
        finding1.setSeverity("HIGH");
        finding1.setImpactAmount(new BigDecimal("150.50"));
        finding1.setDescription("Violation");
        finding1.setAutoFixAvailable(false);
        finding1.setCreatedAt(OffsetDateTime.now());
        expiredRun.setFindings(List.of(finding1));

        // Run 2: Active
        AuditRun activeRun = new AuditRun();
        activeRun.setId(UuidV7.generate());
        activeRun.setTenantId("tenant-expire");
        activeRun.setRulesExecuted(new String[]{"RULE_A"});
        activeRun.setAnalysisMode("LEDGER_ANALYSIS");
        activeRun.setUserId("expire-user");
        activeRun.setCreatedAt(OffsetDateTime.now());
        activeRun.setStatus("SUCCESS");
        activeRun.setExpiresAt(OffsetDateTime.now().plusDays(1)); // Expires tomorrow
        
        AuditRunFinding finding2 = new AuditRunFinding();
        finding2.setId(UuidV7.generate());
        finding2.setAuditRun(activeRun);
        finding2.setTenantId(activeRun.getTenantId());
        finding2.setRuleId("RULE_A");
        finding2.setSeverity("HIGH");
        finding2.setImpactAmount(new BigDecimal("100.00"));
        finding2.setDescription("Violation");
        finding2.setAutoFixAvailable(false);
        finding2.setCreatedAt(OffsetDateTime.now());
        finding2.setAuditRun(activeRun);
        activeRun.setFindings(List.of(finding2));

        auditRunRepository.saveAllAndFlush(List.of(expiredRun, activeRun));

        // Act
        int deletedCount = auditRunRepository.deleteByExpiresAtBefore(OffsetDateTime.now());

        // Assert
        assertEquals(1, deletedCount);
        
        entityManager.clear();
        
        assertTrue(auditRunRepository.findById(expiredRun.getId()).isEmpty(), "Expired run should be deleted");
        assertTrue(auditRunRepository.findById(activeRun.getId()).isPresent(), "Active run should remain");
        
        // Findings should cascade
        assertNull(entityManager.find(AuditRunFinding.class, finding1.getId()), "Finding 1 should be gone");
        assertNotNull(entityManager.find(AuditRunFinding.class, finding2.getId()), "Finding 2 should remain");
    }
}
