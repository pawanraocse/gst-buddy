package com.learning.backendservice.engine.rules;

import com.learning.backendservice.domain.ledger.LedgerFileProcessor;
import com.learning.backendservice.domain.ledger.LedgerFileProcessor.ProcessingOutcome;
import com.learning.backendservice.domain.rule37.InterestRow;
import com.learning.backendservice.domain.rule37.LedgerResult;
import com.learning.backendservice.domain.rule37.CalculationSummary;
import com.learning.backendservice.engine.AuditContext;
import com.learning.backendservice.engine.AuditFinding;
import com.learning.backendservice.engine.AuditRuleResult;
import com.learning.backendservice.exception.LedgerParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Rule37AuditRule")
class Rule37AuditRuleTest {

    @Mock
    private LedgerFileProcessor ledgerFileProcessor;

    @InjectMocks
    private Rule37AuditRule rule37AuditRule;

    private AuditContext context;

    @BeforeEach
    void setUp() {
        context = AuditContext.of(
                "tenant123",
                "user1",
                LocalDate.of(2024, 3, 31)
        );
    }

    @Test
    @DisplayName("Should successfully execute rule and map domain results to findings")
    void shouldExecuteSuccessfully() {
        MultipartFile file1 = new MockMultipartFile("file1", "test1.xlsx", "application/vnd.ms-excel", "test1".getBytes());
        List<MultipartFile> files = List.of(file1);

        LedgerResult result = new LedgerResult();
        CalculationSummary summary = new CalculationSummary();
        summary.setTotalItcReversal(new BigDecimal("100.00"));
        summary.setTotalInterest(new BigDecimal("50.00"));
        
        InterestRow row = new InterestRow();
        row.setSupplier("Test Supplier");
        row.setOriginalInvoiceValue(new BigDecimal("500.00"));
        row.setItcAmount(new BigDecimal("100.00"));
        row.setInterest(new BigDecimal("50.00"));
        row.setRiskCategory(InterestRow.RiskCategory.BREACHED);
        row.setStatus(InterestRow.InterestStatus.UNPAID);
        row.setDaysToDeadline(-10);
        summary.setDetails(List.of(row));
        result.setSummary(summary);

        ProcessingOutcome outcome = new ProcessingOutcome(result, 1);
        when(ledgerFileProcessor.processWithLedgerCount(any(InputStream.class), anyString(), any(LocalDate.class)))
                .thenReturn(outcome);

        AuditRuleResult<List<LedgerResult>> ruleResult = rule37AuditRule.execute(files, context);

        assertNotNull(ruleResult);
        assertEquals(1, ruleResult.findings().size());
        assertEquals(1, ruleResult.ruleSpecificOutput().size());
        assertEquals(0, new BigDecimal("150.00").compareTo(ruleResult.totalImpact()));

        AuditFinding finding = ruleResult.findings().get(0);
        assertEquals(Rule37AuditRule.RULE_ID, finding.ruleId());
        assertEquals(AuditFinding.Severity.CRITICAL, finding.severity());
        assertTrue(finding.description().contains("Test Supplier"));
    }

    @Test
    @DisplayName("Should return INFO finding when no significant issues detected")
    void shouldReturnInfoFindingWhenClean() {
        MultipartFile file1 = new MockMultipartFile("file1", "test1.xlsx", "application/vnd.ms-excel", "test1".getBytes());
        List<MultipartFile> files = List.of(file1);

        LedgerResult result = new LedgerResult();
        CalculationSummary summary = new CalculationSummary();
        summary.setTotalItcReversal(BigDecimal.ZERO);
        summary.setTotalInterest(BigDecimal.ZERO);
        
        InterestRow row = new InterestRow();
        row.setSupplier("Test Supplier");
        row.setOriginalInvoiceValue(new BigDecimal("500.00"));
        row.setItcAmount(new BigDecimal("100.00"));
        row.setInterest(BigDecimal.ZERO);
        row.setRiskCategory(InterestRow.RiskCategory.SAFE);
        row.setStatus(InterestRow.InterestStatus.PAID_ON_TIME);
        summary.setDetails(List.of(row));
        result.setSummary(summary);

        ProcessingOutcome outcome = new ProcessingOutcome(result, 1);
        when(ledgerFileProcessor.processWithLedgerCount(any(InputStream.class), anyString(), any(LocalDate.class)))
                .thenReturn(outcome);

        AuditRuleResult<List<LedgerResult>> ruleResult = rule37AuditRule.execute(files, context);

        assertNotNull(ruleResult);
        assertEquals(1, ruleResult.findings().size());
        assertEquals(AuditFinding.Severity.INFO, ruleResult.findings().get(0).severity());
        assertTrue(ruleResult.findings().get(0).description().contains("No ITC reversal required"));
    }

    @Test
    @DisplayName("Should propagate LedgerParseException")
    void shouldPropagateParseException() {
        MultipartFile file1 = new MockMultipartFile("file1", "test1.xlsx", "application/vnd.ms-excel", "test1".getBytes());
        List<MultipartFile> files = List.of(file1);

        when(ledgerFileProcessor.processWithLedgerCount(any(InputStream.class), anyString(), any(LocalDate.class)))
                .thenThrow(new LedgerParseException("Invalid format"));

        assertThrows(LedgerParseException.class, () -> rule37AuditRule.execute(files, context));
    }

    @Test
    @DisplayName("Should wrap unknown exceptions in LedgerParseException")
    void shouldWrapUnknownException() {
        MultipartFile file1 = new MockMultipartFile("file1", "test1.xlsx", "application/vnd.ms-excel", "test1".getBytes());
        List<MultipartFile> files = List.of(file1);

        // We use doThrow for generic RuntimeException
        when(ledgerFileProcessor.processWithLedgerCount(any(InputStream.class), anyString(), any(LocalDate.class)))
                .thenThrow(new IllegalArgumentException("System error"));

        LedgerParseException ex = assertThrows(LedgerParseException.class, () -> rule37AuditRule.execute(files, context));
        assertTrue(ex.getMessage().contains("Processing failed"));
    }
}
