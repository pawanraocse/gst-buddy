package com.learning.backendservice.domain.itc;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Section16_4CalculationService")
class Section16_4CalculationServiceTest {

    private final Section16_4CalculationService service = new Section16_4CalculationService();

    @Test
    void testEvaluate_NoExpiredRows() {
        Section16_4Input input = new Section16_4Input(
                "GSTIN1",
                LocalDate.of(2024, 10, 1),
                null,
                null,
                List.of(
                        new Section16_4Input.ItcRow("SUP1", "INV1", LocalDate.of(2024, 5, 1), BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ZERO, false)
                )
        );

        Section16_4Result result = service.evaluate(input);
        assertThat(result.expiredRows()).isEmpty();
        assertThat(result.totalItcClaimed()).isEqualByComparingTo("200");
        assertThat(result.totalExpiredItc()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("should flag expired row based on FY deadline")
    void testEvaluate_WithExpiredRow() {
        // Invoice date is April 2024 -> FY 24-25
        // Deadline is 30-Nov-2025.
        // Claimed on 1-Dec-2025 -> Expired
        Section16_4Input input = new Section16_4Input(
                "GSTIN1",
                LocalDate.of(2025, 12, 1),
                null,
                null,
                List.of(
                        new Section16_4Input.ItcRow("SUP1", "INV1", LocalDate.of(2024, 4, 15), BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ZERO, false)
                )
        );

        Section16_4Result result = service.evaluate(input);
        assertThat(result.expiredRows()).hasSize(1);
        assertThat(result.totalExpiredItc()).isEqualByComparingTo("200");
        assertThat(result.expiredRows().get(0).deadline()).isEqualTo(LocalDate.of(2025, 11, 30));
    }

    @Test
    @DisplayName("should enforce 30-Sep deadline for pre-2024 FYs")
    void testPre2024Deadline() {
        // FY 2022-23 invoice -> deadline 30-Sep-2023
        Section16_4Input input = new Section16_4Input(
                "GSTIN1",
                LocalDate.of(2023, 10, 1), // Claimed 1 day late
                null,
                null,
                List.of(
                        new Section16_4Input.ItcRow("SUP1", "INV1", LocalDate.of(2022, 5, 1), BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ZERO, false)
                )
        );

        Section16_4Result result = service.evaluate(input);
        assertThat(result.expiredRows()).hasSize(1);
        assertThat(result.expiredRows().get(0).deadline()).isEqualTo(LocalDate.of(2023, 9, 30));
    }

    @Test
    @DisplayName("should enforce earlier annual return date")
    void testAnnualReturnDateOverrides() {
        // Post-2024 FY invoice -> statutory deadline 30-Nov-2025
        // But annual return filed 15-Oct-2025
        Section16_4Input input = new Section16_4Input(
                "GSTIN1",
                LocalDate.of(2025, 10, 20), // Claimed after annual return
                null,
                LocalDate.of(2025, 10, 15),
                List.of(
                        new Section16_4Input.ItcRow("SUP1", "INV1", LocalDate.of(2024, 6, 1), BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ZERO, false)
                )
        );

        Section16_4Result result = service.evaluate(input);
        assertThat(result.expiredRows()).hasSize(1);
        assertThat(result.expiredRows().get(0).deadline()).isEqualTo(LocalDate.of(2025, 10, 15));
    }

    @Test
    @DisplayName("should pass if claimed exactly on 30-Nov deadline")
    void testBoundary_ExactDeadline() {
        Section16_4Input input = new Section16_4Input(
                "GSTIN1",
                LocalDate.of(2025, 11, 30),
                null,
                null,
                List.of(
                        new Section16_4Input.ItcRow("SUP1", "INV1", LocalDate.of(2024, 4, 15), BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ZERO, false)
                )
        );

        Section16_4Result result = service.evaluate(input);
        assertThat(result.expiredRows()).isEmpty();
    }

    @Test
    @DisplayName("should handle FY rollover (March invoice)")
    void testFyRollover() {
        // March 2024 invoice -> FY 23-24 -> Deadline 30-Nov-2024
        Section16_4Input input = new Section16_4Input(
                "GSTIN1",
                LocalDate.of(2024, 12, 1),
                null,
                null,
                List.of(
                        new Section16_4Input.ItcRow("SUP1", "INV1", LocalDate.of(2024, 3, 15), BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ZERO, false)
                )
        );

        Section16_4Result result = service.evaluate(input);
        assertThat(result.expiredRows()).hasSize(1);
        assertThat(result.expiredRows().get(0).deadline()).isEqualTo(LocalDate.of(2024, 9, 30));
    }

    @Test
    @DisplayName("should apply Sec 16(5) amnesty for FY 2017-18 to 2020-21")
    void testAmnestyWindow() {
        // FY 2018-19 invoice -> deadline extended to 30-Nov-2021
        Section16_4Input input = new Section16_4Input(
                "GSTIN1",
                LocalDate.of(2021, 12, 1), // Claimed 1-Dec-2021 -> Expired
                null,
                null,
                List.of(
                        new Section16_4Input.ItcRow("SUP1", "INV1", LocalDate.of(2018, 5, 1), BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ZERO, false)
                )
        );

        Section16_4Result result = service.evaluate(input);
        assertThat(result.expiredRows()).hasSize(1);
        assertThat(result.expiredRows().get(0).deadline()).isEqualTo(LocalDate.of(2021, 11, 30));
    }

    @Test
    @DisplayName("should use debit note date for deadline calculation")
    void testDebitNoteDeadlineUsesDnDate() {
        // Debit note issued in FY 24-25, so deadline should be 30-Nov-2025
        Section16_4Input input = new Section16_4Input(
                "GSTIN1",
                LocalDate.of(2025, 10, 1),
                null,
                null,
                List.of(
                        new Section16_4Input.ItcRow("SUP1", "INV1", LocalDate.of(2024, 4, 15), BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ZERO, true)
                )
        );

        Section16_4Result result = service.evaluate(input);
        assertThat(result.expiredRows()).isEmpty();
    }
}
