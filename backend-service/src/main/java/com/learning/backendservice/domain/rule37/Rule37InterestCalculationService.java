package com.learning.backendservice.domain.rule37;

import com.learning.backendservice.domain.ledger.LedgerEntry;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Rule 37 (180-day ITC reversal) interest calculation service.
 *
 * <p>
 * <b>Formulas (GST compliant):</b>
 * <ul>
 * <li>ITC Amount = principal × (18 / 118)</li>
 * <li>Interest = itcAmount × 0.18 × delayDays / 365</li>
 * </ul>
 *
 * <p>
 * <b>Algorithm:</b> FIFO purchase/payment matching per supplier.
 *
 * @see Rule37InterestCalculator
 */
@Service
public class Rule37InterestCalculationService implements Rule37InterestCalculator {

    // GST calculation constants
    private static final double ITC_RATE = 18.0 / 118.0;
    private static final double INTEREST_RATE = 0.18;
    private static final int DAYS_THRESHOLD = 180;
    private static final int AT_RISK_THRESHOLD = 150;
    private static final int DAYS_IN_YEAR = 365;
    private static final int DECIMAL_PLACES = 2;
    /** Tolerance for floating-point dust after FIFO matching (prevents infinite loops). */
    private static final double AMOUNT_EPSILON = 0.001;

    @Override
    public CalculationSummary calculate(List<LedgerEntry> entries, LocalDate asOnDate) {
        var queues = partitionBySupplier(entries);
        var results = processAllSuppliers(queues, asOnDate);
        return buildSummary(results, asOnDate);
    }

    /**
     * Partitions ledger entries into purchase and payment queues per supplier.
     */
    private SupplierQueues partitionBySupplier(List<LedgerEntry> entries) {
        Map<String, List<MutableLedgerItem>> purchases = new LinkedHashMap<>();
        Map<String, List<MutableLedgerItem>> payments = new LinkedHashMap<>();

        entries.stream()
                .sorted(Comparator.comparing(LedgerEntry::getDate))
                .forEach(entry -> {
                    var map = entry.getEntryType() == LedgerEntry.LedgerEntryType.PURCHASE
                            ? purchases
                            : payments;
                    map.computeIfAbsent(entry.getSupplier(), k -> new ArrayList<>())
                            .add(new MutableLedgerItem(entry.getDate(), entry.getAmount()));
                });

        return new SupplierQueues(purchases, payments);
    }

    /**
     * Processes all suppliers and collects interest rows.
     */
    private List<InterestRow> processAllSuppliers(SupplierQueues queues, LocalDate asOnDate) {
        List<InterestRow> results = new ArrayList<>();

        queues.purchases().forEach((supplier, purchaseQueue) -> {
            var paymentQueue = new ArrayList<>(
                    queues.payments().getOrDefault(supplier, List.of()));
            var pQueue = new ArrayList<>(purchaseQueue);

            // FIFO matching for PAID_LATE entries
            processFifoMatching(supplier, pQueue, paymentQueue, asOnDate, results);

            // Remaining purchases are UNPAID
            processUnpaidPurchases(supplier, pQueue, asOnDate, results);
        });

        return results;
    }

    /**
     * FIFO matching algorithm: matches purchases against payments chronologically.
     */
    private void processFifoMatching(String supplier, List<MutableLedgerItem> purchases,
            List<MutableLedgerItem> payments, LocalDate asOnDate, List<InterestRow> results) {

        while (!purchases.isEmpty() && !payments.isEmpty()) {
            var purchase = purchases.getFirst();
            var payment = payments.getFirst();
            double matched = Math.min(purchase.amount(), payment.amount());
            int delayDays = daysBetween(purchase.date(), payment.date());

            if (delayDays > DAYS_THRESHOLD) {
                results.add(createInterestRow(supplier, purchase.date(), payment.date(),
                        matched, delayDays, InterestRow.InterestStatus.PAID_LATE, asOnDate));
            }

            // Reduce amounts and remove exhausted items
            purchase.reduceBy(matched);
            payment.reduceBy(matched);
            if (purchase.isExhausted())
                purchases.removeFirst();
            if (payment.isExhausted())
                payments.removeFirst();
        }
    }

    /**
     * Processes remaining unpaid purchases.
     * <ul>
     *   <li>Purchases > 180 days: BREACHED — full ITC reversal + interest</li>
     *   <li>Purchases 151-180 days: AT_RISK — early warning, zero interest/ITC</li>
     * </ul>
     */
    private void processUnpaidPurchases(String supplier, List<MutableLedgerItem> purchases,
            LocalDate asOnDate, List<InterestRow> results) {

        for (MutableLedgerItem purchase : purchases) {
            int days = daysBetween(purchase.date(), asOnDate);

            if (days > DAYS_THRESHOLD) {
                // BREACHED: ITC reversal required under Section 16(2)
                results.add(createInterestRow(
                        supplier, purchase.date(), null, purchase.amount(),
                        days, InterestRow.InterestStatus.UNPAID, asOnDate));
            } else if (days > AT_RISK_THRESHOLD) {
                // AT_RISK: approaching deadline — early warning with zero liability
                results.add(createAtRiskRow(
                        supplier, purchase.date(), purchase.amount(), days, asOnDate));
            }
        }
    }

    /**
     * Creates an InterestRow with all computed fields (BREACHED or PAID_LATE).
     */
    private InterestRow createInterestRow(String supplier, LocalDate purchaseDate,
            LocalDate paymentDate, double principal, int delayDays,
            InterestRow.InterestStatus status, LocalDate asOnDate) {

        LocalDate deadline = purchaseDate.plusDays(DAYS_THRESHOLD);
        var itcInterest = computeItcAndInterest(principal, delayDays);

        return InterestRow.builder()
                .supplier(supplier)
                .purchaseDate(purchaseDate)
                .paymentDate(paymentDate)
                .principal(principal)
                .delayDays(delayDays)
                .itcAmount(itcInterest.itcAmount())
                .interest(itcInterest.interest())
                .status(status)
                .paymentDeadline(deadline)
                .riskCategory(categorizeRisk(delayDays))
                .gstr3bPeriod(formatGstr3bPeriod(deadline))
                .daysToDeadline(daysBetween(asOnDate, deadline))
                .itcAvailmentDate(null) // GSTR-3B readiness: populated when GSTR-3B data available
                .build();
    }

    /**
     * Creates an AT_RISK early-warning row (151-180 days unpaid).
     * No ITC reversal or interest yet — purely informational.
     */
    private InterestRow createAtRiskRow(String supplier, LocalDate purchaseDate,
            double principal, int delayDays, LocalDate asOnDate) {

        LocalDate deadline = purchaseDate.plusDays(DAYS_THRESHOLD);

        return InterestRow.builder()
                .supplier(supplier)
                .purchaseDate(purchaseDate)
                .paymentDate(null)
                .principal(principal)
                .delayDays(delayDays)
                .itcAmount(0)    // No reversal yet
                .interest(0)     // No interest yet
                .status(InterestRow.InterestStatus.UNPAID)
                .paymentDeadline(deadline)
                .riskCategory(InterestRow.RiskCategory.AT_RISK)
                .gstr3bPeriod(formatGstr3bPeriod(deadline))
                .daysToDeadline(daysBetween(asOnDate, deadline))
                .itcAvailmentDate(null)
                .build();
    }

    /**
     * Builds the calculation summary with aggregated metrics.
     */
    private CalculationSummary buildSummary(List<InterestRow> results, LocalDate asOnDate) {
        double totalInterest = results.stream()
                .mapToDouble(InterestRow::getInterest)
                .sum();

        double totalItcReversal = results.stream()
                .filter(r -> r.getStatus() == InterestRow.InterestStatus.UNPAID)
                .mapToDouble(InterestRow::getItcAmount)
                .sum();

        var atRiskRows = results.stream()
                .filter(r -> r.getRiskCategory() == InterestRow.RiskCategory.AT_RISK)
                .toList();

        long breachedCount = results.stream()
                .filter(r -> r.getRiskCategory() == InterestRow.RiskCategory.BREACHED)
                .count();

        return CalculationSummary.builder()
                .totalInterest(round(totalInterest))
                .totalItcReversal(round(totalItcReversal))
                .details(results)
                .atRiskCount(atRiskRows.size())
                .atRiskAmount(round(atRiskRows.stream().mapToDouble(InterestRow::getPrincipal).sum()))
                .breachedCount((int) breachedCount)
                .calculationDate(asOnDate)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pure calculation methods (Single Responsibility)
    // ─────────────────────────────────────────────────────────────────────────────

    private static ItcInterest computeItcAndInterest(double principal, int delayDays) {
        double itcAmount = round(principal * ITC_RATE);
        double interest = round(itcAmount * INTEREST_RATE * delayDays / DAYS_IN_YEAR);
        return new ItcInterest(itcAmount, interest);
    }

    private static InterestRow.RiskCategory categorizeRisk(int delayDays) {
        if (delayDays <= AT_RISK_THRESHOLD)
            return InterestRow.RiskCategory.SAFE;
        if (delayDays <= DAYS_THRESHOLD)
            return InterestRow.RiskCategory.AT_RISK;
        return InterestRow.RiskCategory.BREACHED;
    }

    private static String formatGstr3bPeriod(LocalDate deadline) {
        LocalDate reportingMonth = deadline.plusMonths(1);
        Month month = reportingMonth.getMonth();
        return month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + reportingMonth.getYear();
    }

    private static int daysBetween(LocalDate from, LocalDate to) {
        return (int) ChronoUnit.DAYS.between(from, to);
    }

    private static double round(double value) {
        double factor = Math.pow(10, DECIMAL_PLACES);
        return Math.round(value * factor) / factor;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Immutable records for data transfer (modern Java 17+)
    // ─────────────────────────────────────────────────────────────────────────────

    private record ItcInterest(double itcAmount, double interest) {
    }

    private record SupplierQueues(
            Map<String, List<MutableLedgerItem>> purchases,
            Map<String, List<MutableLedgerItem>> payments) {
    }

    /**
     * Mutable ledger item for FIFO matching (amount reduces during processing).
     */
    private static final class MutableLedgerItem {
        private final LocalDate date;
        private double amount;

        MutableLedgerItem(LocalDate date, double amount) {
            this.date = date;
            this.amount = amount;
        }

        LocalDate date() {
            return date;
        }

        double amount() {
            return amount;
        }

        void reduceBy(double value) {
            this.amount -= value;
        }

        boolean isExhausted() {
            return amount <= AMOUNT_EPSILON;
        }
    }
}
