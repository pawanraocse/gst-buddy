package com.learning.backendservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.backendservice.config.MemoryGuard;
import com.learning.backendservice.config.UploadProperties;
import com.learning.backendservice.dto.CreditWalletResponse;
import com.learning.backendservice.dto.UploadResult;
import com.learning.backendservice.engine.AuditContext;
import com.learning.backendservice.engine.AuditRule;
import com.learning.backendservice.engine.AuditRuleRegistry;
import com.learning.backendservice.engine.DocumentTypeResolver;
import com.learning.backendservice.engine.AuditRuleResult;
import com.learning.backendservice.entity.AuditRun;
import com.learning.backendservice.repository.AuditFindingRepository;
import com.learning.backendservice.repository.AuditRunRepository;
import com.learning.backendservice.repository.LateFeeReliefWindowRepository;
import com.learning.backendservice.service.ingestion.ParserOrchestrator;
import com.learning.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditRunOrchestrator")
class AuditRunOrchestratorTest {

    @Mock private AuditRuleRegistry ruleRegistry;
    @Mock private AuditRunRepository runRepository;
    @Mock private AuditFindingRepository findingRepository;
    @Mock private UploadProperties uploadProperties;
    @Mock private CreditClient creditClient;
    @Mock private MemoryGuard memoryGuard;
    @Mock private ParserOrchestrator parserOrchestrator;
    @Mock private LateFeeReliefWindowRepository reliefWindowRepository;
    @Mock private com.learning.backendservice.engine.RuleResolutionEngine ruleResolutionEngine;
    @Mock private com.learning.backendservice.engine.PipelineExecutor pipelineExecutor;
    @Mock private ContextEnricher contextEnricher;
    @Mock private com.learning.backendservice.repository.AuditRunRuleResultRepository ruleResultRepository;
    @Mock private DocumentTypeResolver documentTypeResolver;

    @Mock private AuditRule<List<MultipartFile>, Object> dummyRule;

    private AuditRunOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        when(uploadProperties.getMaxFiles()).thenReturn(5);
        when(uploadProperties.getMaxConcurrentUploads()).thenReturn(2);
        
        // Use lenient to avoid UnnecessaryStubbingException in some error tests where this is checked late or not at all
        lenient().when(uploadProperties.getMaxFileSize()).thenReturn(DataSize.ofMegabytes(10));

        orchestrator = new AuditRunOrchestrator(
                ruleRegistry, runRepository, findingRepository,
                uploadProperties, creditClient, memoryGuard, new ObjectMapper(),
                parserOrchestrator, reliefWindowRepository,
                ruleResolutionEngine, pipelineExecutor, contextEnricher, ruleResultRepository,
                documentTypeResolver, 7, 50);

                
        TenantContext.setCurrentTenant("tenant123");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should orchestrate upload, execute rule, consume credits, and save entity")
    void shouldOrchestrateUpload() {
        MultipartFile file = new MockMultipartFile("file", "test.xlsx", "text/plain", "data".getBytes());
        LocalDate asOnDate = LocalDate.parse("2024-03-31");
        String ruleId = "DUMMY_RULE";
        String userId = "user1";

        when(ruleRegistry.hasRule(ruleId)).thenReturn(true);
        when(ruleRegistry.getRule(ruleId)).thenReturn((AuditRule) dummyRule);
        when(runRepository.countByTenantId("tenant123")).thenReturn(10L);

        AuditRuleResult<Object> ruleResult = new AuditRuleResult<>(List.of(), List.of("data"), new BigDecimal("100"), 1);
        when(dummyRule.execute(anyList(), any(AuditContext.class))).thenReturn(ruleResult);

        when(runRepository.save(any(AuditRun.class))).thenAnswer(i -> {
            AuditRun r = i.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });

        CreditWalletResponse walletResponse = new CreditWalletResponse(100, 1, 99);
        when(creditClient.consumeCredits(eq(userId), eq(1), anyString(), anyString())).thenReturn(walletResponse);

        UploadResult uploadResult = orchestrator.processUpload(List.of(file), asOnDate, ruleId, userId);

        assertNotNull(uploadResult);
        assertEquals(99, uploadResult.getRemainingCredits());
        assertEquals(1, uploadResult.getCreditsConsumed());

        verify(memoryGuard).checkMemoryBudget(anyList());
        verify(creditClient).checkBalance(userId, 1);
        verify(findingRepository).saveAll(anyList());
        
        ArgumentCaptor<AuditRun> runCaptor = ArgumentCaptor.forClass(AuditRun.class);
        verify(runRepository).save(runCaptor.capture());
        assertArrayEquals(new String[]{ruleId}, runCaptor.getValue().getRulesExecuted());
        assertEquals("tenant123", runCaptor.getValue().getTenantId());
    }

    @Test
    @DisplayName("Should throw if run limit exceeded")
    void shouldThrowIfRunLimitExceeded() {
        MultipartFile file = new MockMultipartFile("file", "test.xlsx", "text/plain", "data".getBytes());
        String ruleId = "DUMMY_RULE";

        when(ruleRegistry.hasRule(ruleId)).thenReturn(true);
        when(runRepository.countByTenantId("tenant123")).thenReturn(50L); // Max is 50

        Exception ex = assertThrows(IllegalArgumentException.class, 
                () -> orchestrator.processUpload(List.of(file), LocalDate.now(), ruleId, "user1"));
        
        assertTrue(ex.getMessage().contains("Maximum saved audit runs"));
    }
}
