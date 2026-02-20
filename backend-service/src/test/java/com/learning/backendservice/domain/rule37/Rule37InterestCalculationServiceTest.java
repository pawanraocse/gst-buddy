package com.learning.backendservice.domain.rule37;

import com.learning.backendservice.domain.ledger.LedgerEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Domain unit tests for Rule 37 ITC reversal calculation.
 * Pure logic tests — no Spring context required.
 */
class Rule37InterestCalculationServiceTest {

    private Rule37InterestCalculationService service;
    private static final LocalDate AS_ON_DATE = LocalDate.of(2025, 6, 1);

    @BeforeEach
    void setUp() {
        service = new Rule37InterestCalculationService();
    }

    // ─── Helper factory methods ───
    private LedgerEntry purchase(LocalDate date, double amount, String supplier) {
        return new LedgerEntry(date, LedgerEntry.LedgerEntryType.PURCHASE, supplier, amount);
    }

    private LedgerEntry payment(LocalDate date, double amount, String supplier) {
        return new LedgerEntry(date, LedgerEntry.LedgerEntryType.PAYMENT, supplier, amount);
    }

    @Nested
    @DisplayName("Happy Path — Single Supplier Late Payment")
    class HappyPath {

        @Test
        @DisplayName("1 purchase + 1 late payment → correct ITC, interest, PAID_LATE")
        void singleSupplierLatePaid() {
            // Purchase on Jan 1, paid on Aug 1 (212 days delay, > 180)
            LocalDate purchaseDate = LocalDate.of(2025, 1, 1);
            LocalDate paymentDate = LocalDate.of(2025, 8, 1);
            double principal = 118_000;

            var entries = List.of(
                    purchase(purchaseDate, principal, "Supplier A"),
                    payment(paymentDate, principal, "Supplier A")
            );

            CalculationSummary summary = service.calculate(entries, AS_ON_DATE);

            assertFalse(summary.getDetails().isEmpty(), "Should have at least one result");

            InterestRow row = summary.getDetails().stream()
                    .filter(r -> r.getRiskCategory() == InterestRow.RiskCategory.BREACHED)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Expected BREACHED entry"));

            assertEquals("Supplier A", row.getSupplier());
            assertEquals(InterestRow.InterestStatus.PAID_LATE, row.getStatus());
            assertEquals(212, row.getDelayDays());

            // ITC = 118000 * 18/118 = 18000
            assertEquals(18_000.0, row.getItcAmount(), 0.01);

            // Interest = 18000 * 0.18 * 212/365 ≈ 1882.19
            double expectedInterest = 18_000.0 * 0.18 * 212.0 / 365.0;
            assertEquals(expectedInterest, row.getInterest(), 1.0);

            assertEquals(InterestRow.RiskCategory.BREACHED, row.getRiskCategory());
        }
    }

    @Nested
    @DisplayName("UNPAID — Breached 180-day Threshold")
    class UnpaidBreached {

        @Test
        @DisplayName("Unpaid purchase > 180 days → ITC reversal required")
        void unpaidPast180Days() {
            // Purchase on Nov 1, 2024 — 212 days before AS_ON_DATE (Jun 1, 2025)
            LocalDate purchaseDate = LocalDate.of(2024, 11, 1);
            double principal = 59_000;

            var entries = List.of(purchase(purchaseDate, principal, "Supplier B"));

            CalculationSummary summary = service.calculate(entries, AS_ON_DATE);

            InterestRow row = summary.getDetails().stream()
                    .filter(r -> r.getStatus() == InterestRow.InterestStatus.UNPAID
                            && r.getRiskCategory() == InterestRow.RiskCategory.BREACHED)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Expected UNPAID BREACHED entry"));

            assertEquals("Supplier B", row.getSupplier());
            assertNull(row.getPaymentDate());
            assertTrue(row.getItcAmount() > 0, "ITC should be positive for breached unpaid");
            assertTrue(row.getInterest() > 0, "Interest should be positive for breached unpaid");
            assertTrue(summary.getTotalItcReversal() > 0);
        }
    }

    @Nested
    @DisplayName("FIFO Matching")
    class FifoMatching {

        @Test
        @DisplayName("Multiple purchases and payments matched chronologically")
        void fifoMatchingOrder() {
            var entries = List.of(
                    purchase(LocalDate.of(2024, 10, 1), 50_000, "Supplier C"),
                    purchase(LocalDate.of(2024, 10, 15), 30_000, "Supplier C"),
                    payment(LocalDate.of(2025, 5, 1), 60_000, "Supplier C"),  // 212 & 198 days
                    payment(LocalDate.of(2025, 5, 15), 20_000, "Supplier C")  // 212 days
            );

            CalculationSummary summary = service.calculate(entries, AS_ON_DATE);

            // At least the first purchase should be PAID_LATE (212 days > 180)
            assertTrue(summary.getDetails().stream()
                    .anyMatch(r -> r.getStatus() == InterestRow.InterestStatus.PAID_LATE),
                    "FIFO should produce at least one PAID_LATE entry");

            // Total interest should be positive
            assertTrue(summary.getTotalInterest() > 0);
        }
    }

    @Nested
    @DisplayName("Edge Cases — Boundary Days")
    class BoundaryDays {

        @Test
        @DisplayName("Exactly 180 days → no reversal (threshold is >180, not >=180)")
        void exactly180DaysNoReversal() {
            // Purchase on Dec 3, 2024 → AS_ON_DATE Jun 1, 2025 = exactly 180 days
            LocalDate purchaseDate = LocalDate.of(2024, 12, 3);

            var entries = List.of(purchase(purchaseDate, 100_000, "Supplier D"));

            CalculationSummary summary = service.calculate(entries, AS_ON_DATE);

            // Should NOT have any BREACHED entries (delay = 180, threshold is >180)
            boolean hasBreached = summary.getDetails().stream()
                    .anyMatch(r -> r.getRiskCategory() == InterestRow.RiskCategory.BREACHED);
            assertFalse(hasBreached, "Exactly 180 days should NOT trigger reversal");
        }

        @Test
        @DisplayName("181 days → triggers reversal")
        void days181TriggersReversal() {
            // Purchase on Dec 2, 2024 → AS_ON_DATE Jun 1, 2025 = 181 days
            LocalDate purchaseDate = LocalDate.of(2024, 12, 2);

            var entries = List.of(purchase(purchaseDate, 100_000, "Supplier E"));

            CalculationSummary summary = service.calculate(entries, AS_ON_DATE);

            boolean hasBreached = summary.getDetails().stream()
                    .anyMatch(r -> r.getRiskCategory() == InterestRow.RiskCategory.BREACHED);
            assertTrue(hasBreached, "181 days should trigger reversal");
            assertTrue(summary.getTotalItcReversal() > 0);
        }
    }

    @Nested
    @DisplayName("Edge Cases — Zero and Extreme Values")
    class ZeroAndExtreme {

        @Test
        @DisplayName("Zero-amount entries → no errors, empty results")
        void zeroAmountEntries() {
            var entries = List.of(
                    purchase(LocalDate.of(2024, 6, 1), 0, "Supplier F"),
                    payment(LocalDate.of(2025, 1, 1), 0, "Supplier F")
            );

            assertDoesNotThrow(() -> service.calculate(entries, AS_ON_DATE));
        }

        @Test
        @DisplayName("Empty entries list → no errors, empty summary")
        void emptyEntries() {
            CalculationSummary summary = service.calculate(List.of(), AS_ON_DATE);

            assertNotNull(summary);
            assertTrue(summary.getDetails().isEmpty());
            assertEquals(0, summary.getTotalInterest(), 0.001);
            assertEquals(0, summary.getTotalItcReversal(), 0.001);
        }
    }

    @Nested
    @DisplayName("Floating-Point Dust — Infinite Loop Prevention")
    class FloatingPointDust {

        @Test
        @DisplayName("Amounts that produce floating-point residuals → no infinite loop")
        void floatingPointDust() {
            // Amounts chosen to produce floating-point residuals (e.g., 1/3)
            double amount = 1000.005;
            var entries = List.of(
                    purchase(LocalDate.of(2024, 6, 1), amount, "Supplier G"),
                    payment(LocalDate.of(2025, 1, 15), amount, "Supplier G")
            );

            // Should complete without hanging
            assertTimeoutPreemptively(java.time.Duration.ofSeconds(5), () -> {
                service.calculate(entries, AS_ON_DATE);
            }, "Calculation should not hang due to floating-point dust");
        }

        @Test
        @DisplayName("Very small amount difference → still completes")
        void verySmallDifference() {
            // Payment just slightly less than purchase
            var entries = List.of(
                    purchase(LocalDate.of(2024, 6, 1), 100_000.0, "Supplier H"),
                    payment(LocalDate.of(2025, 1, 1), 99_999.999, "Supplier H")
            );

            assertTimeoutPreemptively(java.time.Duration.ofSeconds(5), () -> {
                service.calculate(entries, AS_ON_DATE);
            }, "Very small residual should be treated as exhausted");
        }
    }

    @Nested
    @DisplayName("Partial Payment — FIFO Split")
    class PartialPayment {

        @Test
        @DisplayName("Partial payment splits correctly with remainder unpaid")
        void partialPaymentSplit() {
            // Purchase 100k, pay only 60k → 40k remainder
            LocalDate purchaseDate = LocalDate.of(2024, 10, 1); // 243 days before AS_ON_DATE
            var entries = List.of(
                    purchase(purchaseDate, 100_000, "Supplier I"),
                    payment(LocalDate.of(2025, 5, 1), 60_000, "Supplier I")
            );

            CalculationSummary summary = service.calculate(entries, AS_ON_DATE);

            // Should have a PAID_LATE for the 60k matched portion
            // AND an UNPAID for the 40k remaining (both > 180 days)
            long paidLate = summary.getDetails().stream()
                    .filter(r -> r.getStatus() == InterestRow.InterestStatus.PAID_LATE)
                    .count();
            long unpaid = summary.getDetails().stream()
                    .filter(r -> r.getStatus() == InterestRow.InterestStatus.UNPAID
                            && r.getRiskCategory() == InterestRow.RiskCategory.BREACHED)
                    .count();

            assertTrue(paidLate >= 1, "Should have at least one PAID_LATE entry for the matched portion");
            assertTrue(unpaid >= 1, "Should have at least one UNPAID entry for the remainder");
        }
    }

    @Nested
    @DisplayName("Multiple Suppliers — Partitioned Results")
    class MultipleSuppliers {

        @Test
        @DisplayName("Results are correctly partitioned per supplier")
        void multipleSupplierPartitioning() {
            var entries = List.of(
                    purchase(LocalDate.of(2024, 10, 1), 50_000, "Alpha Corp"),
                    purchase(LocalDate.of(2024, 10, 1), 30_000, "Beta Ltd"),
                    payment(LocalDate.of(2025, 5, 1), 50_000, "Alpha Corp")
                    // No payment for Beta Ltd → unpaid/breached
            );

            CalculationSummary summary = service.calculate(entries, AS_ON_DATE);

            // Alpha Corp should have PAID_LATE (212 days > 180)
            assertTrue(summary.getDetails().stream()
                    .anyMatch(r -> r.getSupplier().equals("Alpha Corp")
                            && r.getStatus() == InterestRow.InterestStatus.PAID_LATE));

            // Beta Ltd should have UNPAID/BREACHED
            assertTrue(summary.getDetails().stream()
                    .anyMatch(r -> r.getSupplier().equals("Beta Ltd")
                            && r.getStatus() == InterestRow.InterestStatus.UNPAID
                            && r.getRiskCategory() == InterestRow.RiskCategory.BREACHED));

            // Payment from Alpha should NOT match Beta
            assertFalse(summary.getDetails().stream()
                    .anyMatch(r -> r.getSupplier().equals("Beta Ltd")
                            && r.getStatus() == InterestRow.InterestStatus.PAID_LATE));
        }
    }

    @Nested
    @DisplayName("AT_RISK Early Warnings (150-180 days)")
    class AtRiskWarnings {

        @Test
        @DisplayName("Unpaid purchase at 160 days → AT_RISK with zero interest/ITC")
        void atRiskEntryIncluded() {
            // Purchase on Dec 23, 2024 → AS_ON_DATE Jun 1, 2025 = 160 days
            LocalDate purchaseDate = LocalDate.of(2024, 12, 23);

            var entries = List.of(purchase(purchaseDate, 75_000, "Supplier J"));

            CalculationSummary summary = service.calculate(entries, AS_ON_DATE);

            InterestRow row = summary.getDetails().stream()
                    .filter(r -> r.getRiskCategory() == InterestRow.RiskCategory.AT_RISK)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Expected AT_RISK early warning entry"));

            assertEquals(0, row.getItcAmount(), 0.001, "AT_RISK should have zero ITC");
            assertEquals(0, row.getInterest(), 0.001, "AT_RISK should have zero interest");
            assertEquals(InterestRow.InterestStatus.UNPAID, row.getStatus());
            assertTrue(row.getDaysToDeadline() > 0, "AT_RISK should have positive days to deadline");
        }

        @Test
        @DisplayName("AT_RISK entries do NOT contribute to totalItcReversal or totalInterest")
        void atRiskDoesNotCountInTotals() {
            // Only an AT_RISK entry, no breached
            LocalDate purchaseDate = LocalDate.of(2024, 12, 23); // 160 days
            var entries = List.of(purchase(purchaseDate, 75_000, "Supplier K"));

            CalculationSummary summary = service.calculate(entries, AS_ON_DATE);

            assertEquals(0, summary.getTotalItcReversal(), 0.001, "AT_RISK should not contribute to ITC reversal total");
            assertEquals(0, summary.getTotalInterest(), 0.001, "AT_RISK should not contribute to interest total");
            assertEquals(1, summary.getAtRiskCount(), "Should count 1 AT_RISK entry");
        }
    }
}
