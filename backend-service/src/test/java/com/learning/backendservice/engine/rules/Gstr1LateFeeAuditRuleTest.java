package com.learning.backendservice.engine.rules;

import com.learning.backendservice.domain.gstr1.Gstr1LateFeeCalculatorService;
import com.learning.backendservice.domain.gstr1.Gstr1LateFeeInput;
import com.learning.backendservice.domain.gstr1.Gstr1LateFeeResult;
import com.learning.backendservice.engine.AuditContext;
import com.learning.backendservice.engine.AuditFinding;
import com.learning.backendservice.engine.AuditRuleResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link Gstr1LateFeeAuditRule}.
 *
 * <p>The calculator is mocked — these tests verify the <em>rule</em> behavior:
 * correct metadata, finding severity, legal basis string, impact amount,
 * and description formatting. Math correctness is covered in
 * {@code Gstr1LateFeeCalculatorServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Gstr1LateFeeAuditRule")
class Gstr1LateFeeAuditRuleTest {

    @Mock
    private Gstr1LateFeeCalculatorService calculator;

    @InjectMocks
    private Gstr1LateFeeAuditRule rule;

    private static final AuditContext CTX = AuditContext.of("tenant-1", "user-1",
            LocalDate.of(2024, 4, 30));

    private static final Gstr1LateFeeInput NORMAL_INPUT = new Gstr1LateFeeInput(
            "29XXXXX1234X1ZX",
            LocalDate.of(2024, 4, 15),
            YearMonth.of(2024, 3),
            "2024-25", false, false, null
    );

    // ── Metadata ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getRuleId() returns LATE_FEE_GSTR1")
    void ruleId() {
        assertThat(rule.getRuleId()).isEqualTo("LATE_FEE_GSTR1");
    }

    @Test
    @DisplayName("getLegalBasis() returns exact Section 47(1) citation")
    void legalBasis() {
        assertThat(rule.getLegalBasis()).isEqualTo("Section 47(1), CGST Act 2017");
    }

    @Test
    @DisplayName("getCategory() returns COMPLIANCE")
    void category() {
        assertThat(rule.getCategory()).isEqualTo("COMPLIANCE");
    }

    // ── On Time Filing ────────────────────────────────────────────────────────

    @Test
    @DisplayName("execute() returns INFO finding when filed on time")
    void onTimeFiling() {
        when(calculator.calculate(any())).thenReturn(
                Gstr1LateFeeResult.onTime(
                        LocalDate.of(2024, 4, 11),
                        LocalDate.of(2024, 4, 10)
                )
        );

        AuditRuleResult<Gstr1LateFeeResult> result = rule.execute(NORMAL_INPUT, CTX);

        assertThat(result.findings()).hasSize(1);
        assertThat(result.findings().get(0).severity()).isEqualTo(AuditFinding.Severity.INFO);
        assertThat(result.totalImpact().compareTo(BigDecimal.ZERO)).isZero();
        assertThat(result.creditsConsumed()).isEqualTo(1);
    }

    // ── Late Filing ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("execute() returns HIGH finding when filed late")
    void lateFiling() {
        Gstr1LateFeeResult calcResult = new Gstr1LateFeeResult(
                LocalDate.of(2024, 4, 11),
                LocalDate.of(2024, 4, 15),
                4,
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                new BigDecimal("200.00"),
                false, null
        );
        when(calculator.calculate(any())).thenReturn(calcResult);

        AuditRuleResult<Gstr1LateFeeResult> result = rule.execute(NORMAL_INPUT, CTX);

        assertThat(result.findings()).hasSize(1);
        AuditFinding finding = result.findings().get(0);
        assertThat(finding.severity()).isEqualTo(AuditFinding.Severity.HIGH);
        assertThat(finding.legalBasis()).isEqualTo("Section 47(1), CGST Act 2017");
        assertThat(finding.impactAmount().compareTo(new BigDecimal("200.00"))).isZero();
        assertThat(result.totalImpact().compareTo(new BigDecimal("200.00"))).isZero();
    }

    @Test
    @DisplayName("execute(): description contains delay days, due date, and ARN date")
    void descriptionContainsKeyFacts() {
        Gstr1LateFeeResult calcResult = new Gstr1LateFeeResult(
                LocalDate.of(2024, 4, 11),
                LocalDate.of(2024, 4, 15),
                4,
                new BigDecimal("100.00"), new BigDecimal("100.00"),
                new BigDecimal("200.00"), false, null
        );
        when(calculator.calculate(any())).thenReturn(calcResult);

        AuditRuleResult<Gstr1LateFeeResult> result = rule.execute(NORMAL_INPUT, CTX);
        String desc = result.findings().get(0).description();

        assertThat(desc).contains("4 day(s) late");
        assertThat(desc).contains("2024-04-11");   // due date
        assertThat(desc).contains("2024-04-15");   // ARN date
        assertThat(desc).contains("Section 47(1)");
    }

    @Test
    @DisplayName("execute(): finding compliancePeriod contains FY and tax period")
    void compliancePeriodFormat() {
        Gstr1LateFeeResult calcResult = new Gstr1LateFeeResult(
                LocalDate.of(2024, 4, 11), LocalDate.of(2024, 4, 15),
                4, new BigDecimal("100.00"), new BigDecimal("100.00"),
                new BigDecimal("200.00"), false, null
        );
        when(calculator.calculate(any())).thenReturn(calcResult);

        AuditRuleResult<Gstr1LateFeeResult> result = rule.execute(NORMAL_INPUT, CTX);
        String period = result.findings().get(0).compliancePeriod();

        assertThat(period).contains("FY: 2024-25");
        assertThat(period).contains("2024-03");
    }

    @Test
    @DisplayName("execute(): relief applied notification appears in description")
    void reliefAppliedInDescription() {
        Gstr1LateFeeResult calcResult = new Gstr1LateFeeResult(
                LocalDate.of(2024, 4, 11), LocalDate.of(2024, 5, 15),
                34, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                true, "Notification No. 19/2021-CT"
        );
        when(calculator.calculate(any())).thenReturn(calcResult);

        AuditRuleResult<Gstr1LateFeeResult> result = rule.execute(NORMAL_INPUT, CTX);
        String desc = result.findings().get(0).description();

        assertThat(desc).contains("19/2021");
    }
}
