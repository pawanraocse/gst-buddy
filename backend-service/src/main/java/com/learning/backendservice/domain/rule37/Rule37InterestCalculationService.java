package com.learning.backendservice.domain.rule37;

import com.learning.backendservice.domain.ledger.LedgerEntry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
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
 * All financial calculations use {@link BigDecimal} with HALF_UP rounding.
 *
 * @see Rule37InterestCalculator
 */
@Service
public class Rule37InterestCalculationService implements Rule37InterestCalculator {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final int SCALE = 2;
    private static final RoundingMode RM = RoundingMode.HALF_UP;

    private static final BigDecimal ITC_NUMERATOR = new BigDecimal("18");
    private static final BigDecimal ITC_DENOMINATOR = new BigDecimal("118");
    private static final BigDecimal INTEREST_RATE = new BigDecimal("0.18");
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    private static final int DAYS_THRESHOLD = 180;
    private static final int AT_RISK_THRESHOLD = 150;

    private static final BigDecimal AMOUNT_EPSILON = new BigDecimal("0.001");

    @Override
    public CalculationSummary calculate(List<LedgerEntry> entries, LocalDate asOnDate) {
        var queues = partitionBySupplier(entries);
        var results = processAllSuppliers(queues, asOnDate);
        return buildSummary(results, asOnDate);
    }

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
                            .add(new MutableLedgerItem(entry.getDate(),
                                    BigDecimal.valueOf(entry.getAmount())));
                });

        return new SupplierQueues(purchases, payments);
    }

    private List<InterestRow> processAllSuppliers(SupplierQueues queues, LocalDate asOnDate) {
        List<InterestRow> results = new ArrayList<>();

        queues.purchases().forEach((supplier, purchaseQueue) -> {
            var paymentQueue = new ArrayList<>(
                    queues.payments().getOrDefault(supplier, List.of()));
            var pQueue = new ArrayList<>(purchaseQueue);

            processFifoMatching(supplier, pQueue, paymentQueue, asOnDate, results);
            processUnpaidPurchases(supplier, pQueue, asOnDate, results);
        });

        return results;
    }

    private void processFifoMatching(String supplier, List<MutableLedgerItem> purchases,
            List<MutableLedgerItem> payments, LocalDate asOnDate, List<InterestRow> results) {

        while (!purchases.isEmpty() && !payments.isEmpty()) {
            var purchase = purchases.getFirst();
            var payment = payments.getFirst();
            BigDecimal matched = purchase.amount().min(payment.amount());
            int delayDays = daysBetween(purchase.date(), payment.date());

            if (delayDays > DAYS_THRESHOLD) {
                results.add(createInterestRow(supplier, purchase.date(), payment.date(),
                        matched, delayDays, InterestRow.InterestStatus.PAID_LATE, asOnDate));
            }

            purchase.reduceBy(matched);
            payment.reduceBy(matched);
            if (purchase.isExhausted())
                purchases.removeFirst();
            if (payment.isExhausted())
                payments.removeFirst();
        }
    }

    private void processUnpaidPurchases(String supplier, List<MutableLedgerItem> purchases,
            LocalDate asOnDate, List<InterestRow> results) {

        for (MutableLedgerItem purchase : purchases) {
            int days = daysBetween(purchase.date(), asOnDate);

            if (days > DAYS_THRESHOLD) {
                results.add(createInterestRow(
                        supplier, purchase.date(), null, purchase.amount(),
                        days, InterestRow.InterestStatus.UNPAID, asOnDate));
            } else if (days > AT_RISK_THRESHOLD) {
                results.add(createAtRiskRow(
                        supplier, purchase.date(), purchase.amount(), days, asOnDate));
            }
        }
    }

    private InterestRow createInterestRow(String supplier, LocalDate purchaseDate,
            LocalDate paymentDate, BigDecimal principal, int delayDays,
            InterestRow.InterestStatus status, LocalDate asOnDate) {

        LocalDate deadline = purchaseDate.plusDays(DAYS_THRESHOLD);
        var itcInterest = computeItcAndInterest(principal, delayDays);

        return InterestRow.builder()
                .supplier(supplier)
                .purchaseDate(purchaseDate)
                .paymentDate(paymentDate)
                .principal(principal.setScale(SCALE, RM))
                .delayDays(delayDays)
                .itcAmount(itcInterest.itcAmount())
                .interest(itcInterest.interest())
                .status(status)
                .paymentDeadline(deadline)
                .riskCategory(categorizeRisk(delayDays))
                .gstr3bPeriod(formatGstr3bPeriod(deadline))
                .daysToDeadline(daysBetween(asOnDate, deadline))
                .itcAvailmentDate(null)
                .build();
    }

    private InterestRow createAtRiskRow(String supplier, LocalDate purchaseDate,
            BigDecimal principal, int delayDays, LocalDate asOnDate) {

        LocalDate deadline = purchaseDate.plusDays(DAYS_THRESHOLD);

        return InterestRow.builder()
                .supplier(supplier)
                .purchaseDate(purchaseDate)
                .paymentDate(null)
                .principal(principal.setScale(SCALE, RM))
                .delayDays(delayDays)
                .itcAmount(BigDecimal.ZERO)
                .interest(BigDecimal.ZERO)
                .status(InterestRow.InterestStatus.UNPAID)
                .paymentDeadline(deadline)
                .riskCategory(InterestRow.RiskCategory.AT_RISK)
                .gstr3bPeriod(formatGstr3bPeriod(deadline))
                .daysToDeadline(daysBetween(asOnDate, deadline))
                .itcAvailmentDate(null)
                .build();
    }

    private CalculationSummary buildSummary(List<InterestRow> results, LocalDate asOnDate) {
        BigDecimal totalInterest = results.stream()
                .map(InterestRow::getInterest)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalItcReversal = results.stream()
                .filter(r -> r.getStatus() == InterestRow.InterestStatus.UNPAID)
                .map(InterestRow::getItcAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var atRiskRows = results.stream()
                .filter(r -> r.getRiskCategory() == InterestRow.RiskCategory.AT_RISK)
                .toList();

        BigDecimal atRiskAmount = atRiskRows.stream()
                .map(InterestRow::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long breachedCount = results.stream()
                .filter(r -> r.getRiskCategory() == InterestRow.RiskCategory.BREACHED)
                .count();

        return CalculationSummary.builder()
                .totalInterest(totalInterest.setScale(SCALE, RM))
                .totalItcReversal(totalItcReversal.setScale(SCALE, RM))
                .details(results)
                .atRiskCount(atRiskRows.size())
                .atRiskAmount(atRiskAmount.setScale(SCALE, RM))
                .breachedCount((int) breachedCount)
                .calculationDate(asOnDate)
                .build();
    }

    private static ItcInterest computeItcAndInterest(BigDecimal principal, int delayDays) {
        BigDecimal itcAmount = principal.multiply(ITC_NUMERATOR, MC)
                .divide(ITC_DENOMINATOR, SCALE, RM);
        BigDecimal interest = itcAmount.multiply(INTEREST_RATE, MC)
                .multiply(BigDecimal.valueOf(delayDays), MC)
                .divide(DAYS_IN_YEAR, SCALE, RM);
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

    private record ItcInterest(BigDecimal itcAmount, BigDecimal interest) {
    }

    private record SupplierQueues(
            Map<String, List<MutableLedgerItem>> purchases,
            Map<String, List<MutableLedgerItem>> payments) {
    }

    private static final class MutableLedgerItem {
        private final LocalDate date;
        private BigDecimal amount;

        MutableLedgerItem(LocalDate date, BigDecimal amount) {
            this.date = date;
            this.amount = amount;
        }

        LocalDate date() {
            return date;
        }

        BigDecimal amount() {
            return amount;
        }

        void reduceBy(BigDecimal value) {
            this.amount = this.amount.subtract(value);
        }

        boolean isExhausted() {
            return amount.compareTo(AMOUNT_EPSILON) <= 0;
        }
    }
}
