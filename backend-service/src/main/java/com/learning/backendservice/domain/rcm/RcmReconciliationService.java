package com.learning.backendservice.domain.rcm;

import com.learning.backendservice.domain.shared.PurchaseRegisterRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pure calculator for RCM reconciliation (Books vs GSTR-3B Table 3.1(d)).
 *
 * <p><b>Legal Basis</b>: Section 9(3)/9(4) CGST Act 2017. RCM liability
 * must be paid in cash and then claimed as ITC.
 */
public class RcmReconciliationService {

    private static final int SCALE = 2;
    private static final RoundingMode RM = RoundingMode.HALF_UP;

    public RcmRecoResult reconcile(RcmRecoInput input) {
        // Filter RCM rows from books
        List<PurchaseRegisterRow> rcmBooks = input.purchaseRegisterRows().stream()
                .filter(PurchaseRegisterRow::rcmFlag)
                .toList();

        // Aggregate by tax head
        BigDecimal booksIgst = rcmBooks.stream().map(PurchaseRegisterRow::igst).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(SCALE, RM);
        BigDecimal booksCgst = rcmBooks.stream().map(PurchaseRegisterRow::cgst).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(SCALE, RM);
        BigDecimal booksSgst = rcmBooks.stream().map(PurchaseRegisterRow::sgst).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(SCALE, RM);
        BigDecimal booksCess = rcmBooks.stream().map(PurchaseRegisterRow::cess).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(SCALE, RM);

        List<RcmMismatch> mismatches = new ArrayList<>(4);
        mismatches.add(computeMismatch("IGST", booksIgst, input.gstr3bRcmDeclared().igst(), input.reconToleranceAmount()));
        mismatches.add(computeMismatch("CGST", booksCgst, input.gstr3bRcmDeclared().cgst(), input.reconToleranceAmount()));
        mismatches.add(computeMismatch("SGST/UTGST", booksSgst, input.gstr3bRcmDeclared().sgstUtgst(), input.reconToleranceAmount()));
        mismatches.add(computeMismatch("CESS", booksCess, input.gstr3bRcmDeclared().cess(), input.reconToleranceAmount()));

        BigDecimal totalAbsoluteMismatch = mismatches.stream()
                .map(m -> m.delta().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RM);

        // Supplier breakdown
        List<RcmSupplierBreakdown> breakdown = rcmBooks.stream()
                .collect(Collectors.groupingBy(PurchaseRegisterRow::supplierGstin))
                .entrySet().stream()
                .map(entry -> {
                    String gstin = entry.getKey();
                    List<PurchaseRegisterRow> rows = entry.getValue();
                    BigDecimal totalTaxable = rows.stream().map(PurchaseRegisterRow::taxableValue).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(SCALE, RM);
                    BigDecimal totalTax = rows.stream().map(PurchaseRegisterRow::totalTax).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(SCALE, RM);
                    return new RcmSupplierBreakdown(gstin, rows.size(), totalTaxable, totalTax);
                })
                .sorted((a, b) -> b.totalRcmTax().compareTo(a.totalRcmTax()))
                .toList();

        BigDecimal totalRcmTaxableValueInBooks = breakdown.stream()
                .map(RcmSupplierBreakdown::totalTaxableValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RM);

        String narrative = buildNarrative(mismatches, rcmBooks.size(), totalAbsoluteMismatch);

        return new RcmRecoResult(
                input.taxPeriod(),
                List.copyOf(mismatches),
                breakdown,
                totalAbsoluteMismatch,
                rcmBooks.size(),
                totalRcmTaxableValueInBooks,
                narrative
        );
    }

    private RcmMismatch computeMismatch(String taxHead, BigDecimal booksAmount, BigDecimal gstr3bAmount, BigDecimal tolerance) {
        BigDecimal delta = booksAmount.subtract(gstr3bAmount).setScale(SCALE, RM);
        
        RcmMismatchType type;
        if (delta.abs().compareTo(tolerance) <= 0) {
            type = RcmMismatchType.MATCHED;
        } else if (delta.compareTo(BigDecimal.ZERO) > 0) {
            type = RcmMismatchType.UNDECLARED_RCM;
        } else {
            type = RcmMismatchType.OVER_DECLARED_RCM;
        }

        return new RcmMismatch(taxHead, booksAmount, gstr3bAmount, delta, type);
    }

    private String buildNarrative(List<RcmMismatch> mismatches, int rcmBooksCount, BigDecimal totalAbsoluteMismatch) {
        boolean allMatch = mismatches.stream().allMatch(m -> m.type() == RcmMismatchType.MATCHED);

        if (allMatch) {
            if (rcmBooksCount == 0 && totalAbsoluteMismatch.compareTo(BigDecimal.ZERO) == 0) {
                return "RCM reconciliation: No RCM invoices in books and none declared in GSTR-3B. Match confirmed.";
            }
            return "RCM reconciliation: GSTR-3B Table 3.1(d) matches purchase register RCM invoices within tolerance. No corrective action required.";
        }

        StringBuilder sb = new StringBuilder("RCM mismatch detected: ");
        for (RcmMismatch m : mismatches) {
            if (m.type() != RcmMismatchType.MATCHED) {
                sb.append(String.format("%s (Books: ₹%.2f, 3B: ₹%.2f); ", m.taxHead(), m.booksAmount(), m.gstr3bAmount()));
            }
        }
        sb.append(String.format("Total absolute mismatch: ₹%.2f.", totalAbsoluteMismatch));
        return sb.toString();
    }
}
