package com.learning.backendservice.domain.risk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Collections;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SupplierRiskScoringService")
class SupplierRiskScoringServiceTest {

    private final SupplierRiskScoringService service = new SupplierRiskScoringService();

    @Test
    @DisplayName("should not flag active supplier")
    void testEvaluate_ActiveSupplier() {
        SupplierRiskInput input = new SupplierRiskInput(
                "GSTIN1",
                List.of(
                        new SupplierRiskInput.SupplierData("29AABCU9603R1ZM", "Sup A", "ACTIVE", new BigDecimal("1000"), new BigDecimal("180"))
                )
        );

        SupplierRiskResult result = service.evaluate(input);
        assertThat(result.highRiskCount()).isZero();
        assertThat(result.highRiskExposure()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.supplierScores().get(0).status()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("should flag cancelled supplier")
    void testEvaluate_CancelledSupplier() {
        SupplierRiskInput input = new SupplierRiskInput(
                "GSTIN1",
                List.of(
                        new SupplierRiskInput.SupplierData("07ASXPD9282E1Z8", "Sup B", "CANCELLED", new BigDecimal("1000"), new BigDecimal("180"))
                )
        );

        SupplierRiskResult result = service.evaluate(input);
        assertThat(result.highRiskCount()).isEqualTo(1);
        assertThat(result.highRiskExposure()).isEqualByComparingTo(new BigDecimal("180"));
        assertThat(result.supplierScores().get(0).status()).isEqualTo("CANCELLED");
        assertThat(result.supplierScores().get(0).riskCategory()).isEqualTo("CRITICAL");
    }

    @Test
    @DisplayName("should handle mixed active and cancelled suppliers")
    void testEvaluate_MixedSuppliers() {
        SupplierRiskInput input = new SupplierRiskInput(
                "GSTIN1",
                List.of(
                        new SupplierRiskInput.SupplierData("29AABCU9603R1ZM", "Sup A", "ACTIVE", new BigDecimal("1000"), new BigDecimal("180")),
                        new SupplierRiskInput.SupplierData("07ASXPD9282E1Z8", "Sup B", "CANCELLED", new BigDecimal("500"), new BigDecimal("90"))
                )
        );

        SupplierRiskResult result = service.evaluate(input);
        assertThat(result.highRiskCount()).isEqualTo(1);
        assertThat(result.highRiskExposure()).isEqualByComparingTo(new BigDecimal("90"));
    }

    @Test
    @DisplayName("should handle empty suppliers list")
    void testEvaluate_EmptySuppliers() {
        SupplierRiskInput input = new SupplierRiskInput("GSTIN1", Collections.emptyList());
        SupplierRiskResult result = service.evaluate(input);
        assertThat(result.highRiskCount()).isZero();
        assertThat(result.highRiskExposure()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.supplierScores()).isEmpty();
    }

    @Test
    @DisplayName("should handle null status gracefully")
    void testEvaluate_NullStatus() {
        SupplierRiskInput input = new SupplierRiskInput(
                "GSTIN1",
                List.of(
                        new SupplierRiskInput.SupplierData("29AABCU9603R1ZM", "Sup A", null, new BigDecimal("1000"), new BigDecimal("180"))
                )
        );

        SupplierRiskResult result = service.evaluate(input);
        assertThat(result.highRiskCount()).isZero();
        assertThat(result.highRiskExposure()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
