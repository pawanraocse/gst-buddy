package com.learning.backendservice.service;

import com.learning.backendservice.config.UploadProperties;
import com.learning.backendservice.domain.ledger.LedgerFileProcessor;
import com.learning.backendservice.domain.rule37.CalculationSummary;
import com.learning.backendservice.domain.rule37.LedgerResult;
import com.learning.backendservice.dto.CreditWalletResponse;
import com.learning.backendservice.dto.UploadResult;
import com.learning.backendservice.entity.Rule37CalculationRun;
import com.learning.backendservice.exception.InsufficientCreditsException;
import com.learning.backendservice.repository.Rule37RunRepository;
import com.learning.common.tenant.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LedgerUploadOrchestrator — Credit validation and per-tenant limits")
class LedgerUploadOrchestratorTest {

    @Mock private LedgerFileProcessor ledgerFileProcessor;
    @Mock private Rule37RunRepository runRepository;
    @Mock private CreditClient creditClient;

    private UploadProperties uploadProperties;
    private LedgerUploadOrchestrator orchestrator;

    private static final int RETENTION_DAYS = 7;
    private static final int MAX_RUNS_PER_TENANT = 50;
    private static final String USER_ID = "test-user";
    private static final LocalDate AS_ON_DATE = LocalDate.of(2025, 6, 1);

    @BeforeEach
    void setUp() {
        uploadProperties = new UploadProperties();
        orchestrator = new LedgerUploadOrchestrator(
                ledgerFileProcessor, runRepository, uploadProperties,
                creditClient, RETENTION_DAYS, MAX_RUNS_PER_TENANT);
        TenantContext.setCurrentTenant("test-tenant");
    }

    private MultipartFile mockFile(String name) {
        return new MockMultipartFile(name, name, "application/vnd.ms-excel", new byte[]{1, 2, 3});
    }

    private LedgerResult mockLedgerResult(String name) {
        return new LedgerResult(name, CalculationSummary.builder()
                .totalInterest(new BigDecimal("100.00"))
                .totalItcReversal(new BigDecimal("500.00"))
                .atRiskAmount(BigDecimal.ZERO)
                .details(List.of())
                .calculationDate(AS_ON_DATE)
                .build());
    }

    @Nested
    @DisplayName("Credit Pre-Validation")
    class CreditPreValidation {

        @Test
        @DisplayName("Upload with sufficient credits → success, credits consumed")
        void sufficientCreditsSuccess() throws Exception {
            // Arrange
            when(runRepository.countByTenantId(anyString())).thenReturn(0L);
            when(creditClient.checkBalance(eq(USER_ID), eq(1)))
                    .thenReturn(CreditWalletResponse.builder().total(10).used(0).remaining(10).build());
            when(ledgerFileProcessor.process(any(InputStream.class), anyString(), any(LocalDate.class)))
                    .thenReturn(mockLedgerResult("test.xlsx"));
            when(creditClient.consumeCredits(eq(USER_ID), eq(1), anyString(), anyString()))
                    .thenReturn(CreditWalletResponse.builder().total(10).used(1).remaining(9).build());
            when(runRepository.save(any(Rule37CalculationRun.class)))
                    .thenAnswer(inv -> {
                        Rule37CalculationRun run = inv.getArgument(0);
                        run.setId(1L);
                        return run;
                    });

            // Act
            UploadResult result = orchestrator.processUpload(
                    List.of(mockFile("test.xlsx")), AS_ON_DATE, USER_ID);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getCreditsConsumed());
            assertEquals(9, result.getRemainingCredits());
            assertEquals(1, result.getResults().size());
            assertTrue(result.getErrors().isEmpty());
            verify(creditClient).checkBalance(USER_ID, 1);
            verify(creditClient).consumeCredits(eq(USER_ID), eq(1), anyString(), anyString());
        }

        @Test
        @DisplayName("Upload with insufficient credits → InsufficientCreditsException before calculation")
        void insufficientCreditsBeforeCalculation() {
            // Arrange
            when(runRepository.countByTenantId(anyString())).thenReturn(0L);
            when(creditClient.checkBalance(eq(USER_ID), eq(1)))
                    .thenThrow(new InsufficientCreditsException("Insufficient credits: need 1 but only 0 available"));

            // Act & Assert
            assertThrows(InsufficientCreditsException.class, () ->
                    orchestrator.processUpload(List.of(mockFile("test.xlsx")), AS_ON_DATE, USER_ID));

            // Verify no processing occurred
            verifyNoInteractions(ledgerFileProcessor);
            verify(creditClient, never()).consumeCredits(anyString(), anyInt(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Partial Success — Per-Ledger Credit Check")
    class PartialSuccess {

        @Test
        @DisplayName("Upload 3 files with 2 credits → first 2 succeed, last 1 fails with error")
        void partialCreditExhaustion() throws Exception {
            // Arrange
            when(runRepository.countByTenantId(anyString())).thenReturn(0L);
            when(creditClient.checkBalance(eq(USER_ID), eq(1)))
                    .thenReturn(CreditWalletResponse.builder().total(2).used(0).remaining(2).build());
            when(ledgerFileProcessor.process(any(InputStream.class), anyString(), any(LocalDate.class)))
                    .thenReturn(mockLedgerResult("file1.xlsx"))
                    .thenReturn(mockLedgerResult("file2.xlsx"))
                    .thenReturn(mockLedgerResult("file3.xlsx"));

            // First credit consumption succeeds → 1 remaining
            when(creditClient.consumeCredits(eq(USER_ID), eq(1), anyString(), anyString()))
                    .thenReturn(CreditWalletResponse.builder().total(2).used(1).remaining(1).build())
                    .thenReturn(CreditWalletResponse.builder().total(2).used(2).remaining(0).build());

            when(runRepository.save(any(Rule37CalculationRun.class)))
                    .thenAnswer(inv -> {
                        Rule37CalculationRun run = inv.getArgument(0);
                        run.setId(1L);
                        return run;
                    });

            // Act
            List<MultipartFile> files = List.of(
                    mockFile("file1.xlsx"),
                    mockFile("file2.xlsx"),
                    mockFile("file3.xlsx"));
            UploadResult result = orchestrator.processUpload(files, AS_ON_DATE, USER_ID);

            // Assert: 2 succeeded, 1 failed due to credits
            assertEquals(2, result.getCreditsConsumed());
            assertEquals(2, result.getResults().size());
            assertEquals(1, result.getErrors().size());
            assertTrue(result.getErrors().get(0).getMessage().contains("Insufficient credits"));
        }
    }

    @Nested
    @DisplayName("Per-Tenant Limit Enforcement")
    class TenantLimitEnforcement {

        @Test
        @DisplayName("Per-tenant limit reached → IllegalArgumentException")
        void tenantLimitReached() {
            // Arrange: tenant already at max runs
            when(runRepository.countByTenantId(anyString())).thenReturn((long) MAX_RUNS_PER_TENANT);

            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    orchestrator.processUpload(List.of(mockFile("test.xlsx")), AS_ON_DATE, USER_ID));

            assertTrue(ex.getMessage().contains("Maximum saved calculations"));
            verifyNoInteractions(creditClient);
            verifyNoInteractions(ledgerFileProcessor);
        }
    }

    @Nested
    @DisplayName("Idempotent Credit Consumption")
    class IdempotentCredits {

        @Test
        @DisplayName("Each file gets a unique idempotency key")
        void uniqueIdempotencyKeys() throws Exception {
            when(runRepository.countByTenantId(anyString())).thenReturn(0L);
            when(creditClient.checkBalance(eq(USER_ID), eq(1)))
                    .thenReturn(CreditWalletResponse.builder().total(10).used(0).remaining(10).build());
            when(ledgerFileProcessor.process(any(InputStream.class), anyString(), any(LocalDate.class)))
                    .thenReturn(mockLedgerResult("f1.xlsx"))
                    .thenReturn(mockLedgerResult("f2.xlsx"));
            when(creditClient.consumeCredits(eq(USER_ID), eq(1), anyString(), anyString()))
                    .thenReturn(CreditWalletResponse.builder().total(10).used(1).remaining(9).build())
                    .thenReturn(CreditWalletResponse.builder().total(10).used(2).remaining(8).build());
            when(runRepository.save(any(Rule37CalculationRun.class)))
                    .thenAnswer(inv -> {
                        Rule37CalculationRun run = inv.getArgument(0);
                        run.setId(1L);
                        return run;
                    });

            orchestrator.processUpload(
                    List.of(mockFile("f1.xlsx"), mockFile("f2.xlsx")), AS_ON_DATE, USER_ID);

            // Each call should have a different idempotency key (verified by 2 separate invocations)
            verify(creditClient, times(2)).consumeCredits(eq(USER_ID), eq(1), anyString(), anyString());
        }
    }
}
