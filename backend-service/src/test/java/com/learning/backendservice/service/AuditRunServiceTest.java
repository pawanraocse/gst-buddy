package com.learning.backendservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.backendservice.dto.AuditRunResponse;
import com.learning.backendservice.engine.AuditRule;
import com.learning.backendservice.engine.AuditRuleRegistry;
import com.learning.backendservice.entity.AuditRun;
import com.learning.backendservice.repository.AuditRunRepository;
import com.learning.common.infra.exception.NotFoundException;
import com.learning.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditRunService")
class AuditRunServiceTest {

    @Mock
    private AuditRunRepository runRepository;

    @Mock
    private AuditRuleRegistry ruleRegistry;

    @Mock
    private AuditRule<?, ?> dummyRule;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AuditRunService auditRunService;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant("tenant-123");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should list runs for tenant")
    void shouldListRuns() {
        UUID runId = UUID.randomUUID();
        AuditRun run = new AuditRun();
        run.setId(runId);
        run.setTenantId("tenant-123");
        run.setRulesExecuted(new String[]{"RULE_A"});
        run.setAnalysisMode("LEDGER_ANALYSIS");
        run.setTotalImpactAmount(BigDecimal.TEN);
        run.setCreatedAt(OffsetDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        when(runRepository.findByTenantIdAndUserId("tenant-123", "user1", pageable))
                .thenReturn(new PageImpl<>(List.of(run)));

        lenient().when(ruleRegistry.getRule("RULE_A")).thenReturn((AuditRule) dummyRule);
        lenient().when(dummyRule.getDisplayName()).thenReturn("Dummy Rule A");

        Page<AuditRunResponse> responsePage = auditRunService.listRuns("user1", null, pageable);

        assertNotNull(responsePage);
        assertEquals(1, responsePage.getTotalElements());
        AuditRunResponse res = responsePage.getContent().get(0);
        assertEquals(runId.toString(), res.getId());
        assertEquals("RULE_A", res.getRuleId());
        assertEquals("Dummy Rule A", res.getRuleDisplayName());
        assertNull(res.getResultData()); // list shouldn't include result data
    }

    @Test
    @DisplayName("Should get run by id")
    void shouldGetRunById() {
        UUID runId = UUID.randomUUID();
        AuditRun run = new AuditRun();
        run.setId(runId);
        run.setTenantId("tenant-123");
        run.setRulesExecuted(new String[]{"RULE_A"});
        run.setAnalysisMode("LEDGER_ANALYSIS");
        run.setTotalImpactAmount(BigDecimal.TEN);
        run.setResultData("{\"key\":\"value\"}");

        when(runRepository.findByIdAndTenantIdAndUserId(runId, "tenant-123", "user1")).thenReturn(Optional.of(run));
        lenient().when(ruleRegistry.getRule("RULE_A")).thenThrow(new IllegalArgumentException("Unknown"));

        AuditRunResponse res = auditRunService.getRun(runId, "user1");

        assertNotNull(res);
        assertEquals(runId.toString(), res.getId());
        assertEquals("RULE_A", res.getRuleDisplayName()); // Fallback to ruleId
        assertNotNull(res.getResultData()); // getRun includes result data
    }

    @Test
    @DisplayName("Should throw NotFoundException when getting unknown run")
    void shouldThrowNotFoundOnGet() {
        UUID runId = UUID.randomUUID();
        when(runRepository.findByIdAndTenantIdAndUserId(any(), any(), any())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> auditRunService.getRun(runId, "user1"));
    }

    @Test
    @DisplayName("Should delete run")
    void shouldDeleteRun() {
        UUID runId = UUID.randomUUID();
        when(runRepository.existsByIdAndTenantIdAndUserId(runId, "tenant-123", "user1")).thenReturn(true);

        auditRunService.deleteRun(runId, "user1");

        verify(runRepository).deleteById(runId);
    }

    @Test
    @DisplayName("Should throw NotFoundException when deleting unknown run")
    void shouldThrowNotFoundOnDelete() {
        UUID runId = UUID.randomUUID();
        when(runRepository.existsByIdAndTenantIdAndUserId(runId, "tenant-123", "user1")).thenReturn(false);

        assertThrows(NotFoundException.class, () -> auditRunService.deleteRun(runId, "user1"));
        verify(runRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should get raw run entity")
    void shouldGetRawRunEntity() {
        UUID runId = UUID.randomUUID();
        AuditRun run = new AuditRun();
        when(runRepository.findByIdAndTenantId(runId, "tenant-123")).thenReturn(Optional.of(run));

        AuditRun result = auditRunService.getRunEntity(runId);
        assertNotNull(result);
        assertEquals(run, result);
    }
}
