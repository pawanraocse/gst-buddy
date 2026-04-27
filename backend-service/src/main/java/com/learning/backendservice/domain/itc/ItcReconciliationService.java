package com.learning.backendservice.domain.itc;

import com.learning.backendservice.domain.shared.PurchaseRegisterRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ItcReconciliationService {

    private static final int SCALE = 2;
    private static final RoundingMode RM = RoundingMode.HALF_UP;

    public ItcRecoResult reconcile(ItcRecoInput input) {
        List<PurchaseRegisterRow> books = input.purchaseRegisterRows();
        List<PurchaseRegisterRow> gstr2b = input.gstr2bRows();

        List<ItcMismatch> mismatches = new ArrayList<>();
        BigDecimal tolerance = input.reconToleranceAmount();

        // 1. Index 2B rows by a key
        Map<String, PurchaseRegisterRow> unmapped2b = new HashMap<>();
        for (PurchaseRegisterRow row : gstr2b) {
            unmapped2b.put(matchKey(row.supplierGstin(), row.invoiceNo()), row);
        }

        int matchedCount = 0;
        BigDecimal totalItcAtRisk = BigDecimal.ZERO;

        // 2. Process Books rows
        for (PurchaseRegisterRow bRow : books) {
            String exactKey = matchKey(bRow.supplierGstin(), bRow.invoiceNo());

            if (unmapped2b.containsKey(exactKey)) {
                PurchaseRegisterRow gRow = unmapped2b.remove(exactKey);
                // Compare amounts
                BigDecimal bTax = bRow.totalTax().setScale(SCALE, RM);
                BigDecimal gTax = gRow.totalTax().setScale(SCALE, RM);
                BigDecimal delta = bTax.subtract(gTax).setScale(SCALE, RM);

                if (delta.abs().compareTo(tolerance) <= 0) {
                    // Exact match
                    matchedCount++;
                } else {
                    mismatches.add(new ItcMismatch(
                            bRow.invoiceNo(), bRow.supplierGstin(), bRow, gRow, ItcMismatchType.AMOUNT_MISMATCH, delta
                    ));
                    if (delta.compareTo(BigDecimal.ZERO) > 0) {
                        totalItcAtRisk = totalItcAtRisk.add(delta); // Risk is the excess claimed
                    }
                }
            } else {
                // Try fuzzy match (same invoice no, same total tax, but GSTIN differs)
                String normInv = normalizeInvoice(bRow.invoiceNo());
                BigDecimal bTax = bRow.totalTax().setScale(SCALE, RM);
                
                PurchaseRegisterRow fuzzyMatch = null;
                String fuzzyKey = null;

                for (Map.Entry<String, PurchaseRegisterRow> entry : unmapped2b.entrySet()) {
                    PurchaseRegisterRow candidate = entry.getValue();
                    if (normalizeInvoice(candidate.invoiceNo()).equals(normInv)) {
                        BigDecimal cTax = candidate.totalTax().setScale(SCALE, RM);
                        if (bTax.subtract(cTax).abs().compareTo(tolerance) <= 0) {
                            fuzzyMatch = candidate;
                            fuzzyKey = entry.getKey();
                            break;
                        }
                    }
                }

                if (fuzzyMatch != null) {
                    unmapped2b.remove(fuzzyKey);
                    mismatches.add(new ItcMismatch(
                            bRow.invoiceNo(), bRow.supplierGstin(), bRow, fuzzyMatch, ItcMismatchType.GSTIN_MISMATCH, BigDecimal.ZERO
                    ));
                } else {
                    // Missing in 2B
                    BigDecimal risk = bRow.totalTax().setScale(SCALE, RM);
                    mismatches.add(new ItcMismatch(
                            bRow.invoiceNo(), bRow.supplierGstin(), bRow, null, ItcMismatchType.MISSING_IN_2B, risk
                    ));
                    totalItcAtRisk = totalItcAtRisk.add(risk);
                }
            }
        }

        // 3. Any remaining 2B rows are missing in books
        for (PurchaseRegisterRow gRow : unmapped2b.values()) {
            BigDecimal missingBooksTax = gRow.totalTax().setScale(SCALE, RM).negate(); // negative delta since books=0
            mismatches.add(new ItcMismatch(
                    gRow.invoiceNo(), gRow.supplierGstin(), null, gRow, ItcMismatchType.MISSING_IN_BOOKS, missingBooksTax
            ));
        }

        String narrative = buildNarrative(mismatches, matchedCount, totalItcAtRisk);

        return new ItcRecoResult(
                input.taxPeriod(),
                mismatches,
                totalItcAtRisk,
                matchedCount,
                books.size(),
                gstr2b.size(),
                narrative
        );
    }

    private String matchKey(String gstin, String inv) {
        return (gstin == null ? "" : gstin.trim().toUpperCase()) + "|" + normalizeInvoice(inv);
    }

    private String normalizeInvoice(String inv) {
        if (inv == null) return "";
        // Keep only alphanumeric
        return inv.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
    }

    private String buildNarrative(List<ItcMismatch> mismatches, int matchedCount, BigDecimal risk) {
        if (mismatches.isEmpty()) {
            return String.format("Perfect match. All %d invoices from Books are present in GSTR-2B. ITC fully eligible.", matchedCount);
        }
        
        long missingIn2b = mismatches.stream().filter(m -> m.type() == ItcMismatchType.MISSING_IN_2B).count();
        long amtMismatch = mismatches.stream().filter(m -> m.type() == ItcMismatchType.AMOUNT_MISMATCH).count();
        
        return String.format("Reconciliation complete: %d matched perfectly. %d invoices missing in 2B. %d invoices have amount mismatches. Total ITC at Risk: ₹%.2f per Section 16(2)(aa).", 
                matchedCount, missingIn2b, amtMismatch, risk);
    }
}
