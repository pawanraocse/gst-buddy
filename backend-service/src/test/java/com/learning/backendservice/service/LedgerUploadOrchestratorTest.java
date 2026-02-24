package com.learning.backendservice.service;

import com.learning.backendservice.config.UploadProperties;
import com.learning.backendservice.domain.ledger.LedgerFileProcessor;
import com.learning.backendservice.domain.ledger.LedgerFileProcessor.ProcessingOutcome;
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
@DisplayName("LedgerUploadOrchestrator — Per-ledger credit validation and consumption")
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

    private ProcessingOutcome mockOutcome(String name, int ledgerCount) {
        return new ProcessingOutcome(mockLedgerResult(name), ledgerCount);
    }

    @Nested
    @DisplayName("Single-Supplier File")
    class SingleSupplierFile {

        @Test
        @DisplayName("Upload single file with 1 supplier → consumes 1 credit")
        void singleSupplierConsumesOneCredit() throws Exception {
            when(runRepository.countByTenantId(anyString())).thenReturn(0L);
            when(ledgerFileProcessor.processWithLedgerCount(any(InputStream.class), anyString(), any(LocalDate.class)))
                    .thenReturn(mockOutcome("test.xlsx", 1));
            when(creditClient.checkBalance(eq(USER_ID), eq(1)))
                    .thenReturn(CreditWalletResponse.builder().total(10).used(0).remaining(10).build());
            when(creditClient.consumeCredits(eq(USER_ID), eq(1), anyString(), anyString()))
                    .thenReturn(CreditWalletResponse.builder().total(10).used(1).remaining(9).build());
            when(runRepository.save(any(Rule37CalculationRun.class)))
                    .thenAnswer(inv -> {
                        Rule37CalculationRun run = inv.getArgument(0);
                        run.setId(1L);
                        return run;
                    });

            UploadResult result = orchestrator.processUpload(
                    List.of(mockFile("test.xlsx")), AS_ON_DATE, USER_ID);

            assertEquals(1, result.getCreditsConsumed());
            assertEquals(9, result.getRemainingCredits());
            assertEquals(1, result.getResults().size());
            assertTrue(result.getErrors().isEmpty());
            verify(creditClient).checkBalance(USER_ID, 1);
            verify(creditClient).consumeCredits(eq(USER_ID), eq(1), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Multi-Supplier File")
    class MultiSupplierFile {

        @Test
        @DisplayName("Upload file with 2 suppliers → consumes 2 credits")
        void twoSuppliersConsumesTwoCredits() throws Exception {
            when(runRepository.countByTenantId(anyString())).thenReturn(0L);
            when(ledgerFileProcessor.processWithLedgerCount(any(InputStream.class), anyString(), any(LocalDate.class)))
                    .thenReturn(mockOutcome("KD STEEL.xlsx", 2));
            when(creditClient.checkBalance(eq(USER_ID), eq(2)))
                    .thenReturn(CreditWalletResponse.builder().total(10).used(0).remaining(10).build());
            when(creditClient.consumeCredits(eq(USER_ID), eq(2), anyString(), anyString()))
                    .thenReturn(CreditWalletResponse.builder().total(10).used(2).remaining(8).build());
            when(runRepository.save(any(Rule37CalculationRun.class)))
                    .thenAnswer(inv -> {
                        Rule37CalculationRun run = inv.getArgument(0);
                        run.setId(1L);
                        return run;
                    });

            UploadResult result = orchestrator.processUpload(
                    List.of(mockFile("KD STEEL.xlsx")), AS_ON_DATE, USER_ID);

            assertEquals(2, result.getCreditsConsumed());
            assertEquals(8, result.getRemainingCredits());
            verify(creditClient).checkBalance(USER_ID, 2);
            verify(creditClient).consumeCredits(eq(USER_ID), eq(2), anyString(), anyString());
        }

        @Test
        @DisplayName("Upload file with 3 suppliers but only 2 credits → InsufficientCreditsException")
        void insufficientCreditsForMultiSupplierFile() throws Exception {
            when(runRepository.countByTenantId(anyString())).thenReturn(0L);
            when(ledgerFileProcessor.processWithLedgerCount(any(InputStream.class), anyString(), any(LocalDate.class)))
                    .thenReturn(mockOutcome("big-file.xlsx", 3));
            when(creditClient.checkBalance(eq(USER_ID), eq(3)))
                    .thenThrow(new InsufficientCreditsException("Insufficient credits: need 3 but only 2 available"));

            assertThrows(InsufficientCreditsException.class, () ->
                    orchestrator.processUpload(List.of(mockFile("big-file.xlsx")), AS_ON_DATE, USER_ID));

            verify(creditClient, never()).consumeCredits(anyString(), anyInt(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Multiple Files — Credits Sum Across Files")
    class MultipleFiles {

        @Test
        @DisplayName("Upload 2 files: 2 suppliers + 1 supplier → consumes 3 credits total")
        void creditsAccumulatedAcrossFiles() throws Exception {
            when(runRepository.countByTenantId(anyString())).thenReturn(0L);
            when(ledgerFileProcessor.processWithLedgerCount(any(InputStream.class), anyString(), any(LocalDate.class)))
                    .thenReturn(mockOutcome("file1.xlsx", 2))
                    .thenReturn(mockOutcome("file2.xlsx", 1));
            when(creditClient.checkBalance(eq(USER_ID), eq(3)))
                    .thenReturn(CreditWalletResponse.builder().total(10).used(0).remaining(10).build());
            when(creditClient.consumeCredits(eq(USER_ID), eq(3), anyString(), anyString()))
                    .thenReturn(CreditWalletResponse.builder().total(10).used(3).remaining(7).build());
            when(runRepository.save(any(Rule37CalculationRun.class)))
                    .thenAnswer(inv -> {
                        Rule37CalculationRun run = inv.getArgument(0);
                        run.setId(1L);
                        return run;
                    });

            UploadResult result = orchestrator.processUpload(
                    List.of(mockFile("file1.xlsx"), mockFile("file2.xlsx")), AS_ON_DATE, USER_ID);

            assertEquals(3, result.getCreditsConsumed());
            assertEquals(7, result.getRemainingCredits());
            assertEquals(2, result.getResults().size());
            verify(creditClient).checkBalance(USER_ID, 3);
            verify(creditClient).consumeCredits(eq(USER_ID), eq(3), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Credit Pre-Validation — Fail-Fast")
    class CreditPreValidation {

        @Test
        @DisplayName("Insufficient credits for total ledgers → InsufficientCreditsException before charging")
        void insufficientCreditsBeforeCharging() throws Exception {
            when(runRepository.countByTenantId(anyString())).thenReturn(0L);
            when(ledgerFileProcessor.processWithLedgerCount(any(InputStream.class), anyString(), any(LocalDate.class)))
                    .thenReturn(mockOutcome("test.xlsx", 1));
            when(creditClient.checkBalance(eq(USER_ID), eq(1)))
                    .thenThrow(new InsufficientCreditsException("Insufficient credits: need 1 but only 0 available"));

            assertThrows(InsufficientCreditsException.class, () ->
                    orchestrator.processUpload(List.of(mockFile("test.xlsx")), AS_ON_DATE, USER_ID));

            verify(creditClient, never()).consumeCredits(anyString(), anyInt(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Per-Tenant Limit Enforcement")
    class TenantLimitEnforcement {

        @Test
        @DisplayName("Per-tenant limit reached → IllegalArgumentException")
        void tenantLimitReached() {
            when(runRepository.countByTenantId(anyString())).thenReturn((long) MAX_RUNS_PER_TENANT);

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
        @DisplayName("Single consumption call with unique idempotency key for all ledgers")
        void singleConsumptionCallWithUniqueKey() throws Exception {
            when(runRepository.countByTenantId(anyString())).thenReturn(0L);
            when(ledgerFileProcessor.processWithLedgerCount(any(InputStream.class), anyString(), any(LocalDate.class)))
                    .thenReturn(mockOutcome("f1.xlsx", 1))
                    .thenReturn(mockOutcome("f2.xlsx", 1));
            when(creditClient.checkBalance(eq(USER_ID), eq(2)))
                    .thenReturn(CreditWalletResponse.builder().total(10).used(0).remaining(10).build());
            when(creditClient.consumeCredits(eq(USER_ID), eq(2), anyString(), anyString()))
                    .thenReturn(CreditWalletResponse.builder().total(10).used(2).remaining(8).build());
            when(runRepository.save(any(Rule37CalculationRun.class)))
                    .thenAnswer(inv -> {
                        Rule37CalculationRun run = inv.getArgument(0);
                        run.setId(1L);
                        return run;
                    });

            orchestrator.processUpload(
                    List.of(mockFile("f1.xlsx"), mockFile("f2.xlsx")), AS_ON_DATE, USER_ID);

            // Single consumption call for all ledgers
            verify(creditClient, times(1)).consumeCredits(eq(USER_ID), eq(2), anyString(), anyString());
        }
    }
}
