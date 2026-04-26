package com.learning.backendservice.domain.pos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Core domain service for validating Place of Supply vs Tax Split in GSTR-1.
 * Pure business logic — no Spring annotations, no DB access.
 *
 * <p><b>Legal basis:</b> Sections 10-13, IGST Act 2017 & Section 7-8, CGST Act 2017.
 * Validates that intra-state invoices charge CGST/SGST and inter-state invoices charge IGST.
 */
public class PosValidationService {

    /**
     * Validates a batch of invoices against POS rules.
     */
    public PosValidationResult validate(PosValidationInput input) {
        List<PosMismatch> mismatches = new ArrayList<>();
        int checked = 0;

        if (input.supplierStateCode() == null || input.supplierStateCode().length() < 2) {
            return new PosValidationResult(0, 0, mismatches);
        }

        String supplierState = input.supplierStateCode().substring(0, 2);

        for (PosValidationInput.InvoiceData inv : input.invoices()) {
            if (isZeroRated(inv)) {
                continue; // Skip zero-rated (no tax split to validate)
            }

            checked++;

            boolean isSez = isSezSupply(inv.tableSection());
            String posState = getPosState(inv.placeOfSupply());
            
            boolean isExport = "96".equals(posState);
            boolean isOtherTerritory = "97".equals(posState);

            boolean isInterState;

            if (isSez || isExport || isOtherTerritory) {
                isInterState = true;
            } else {
                if (posState == null) {
                    continue; // cannot determine pos, skip
                }
                isInterState = !supplierState.equals(posState);
            }

            boolean hasIgst = isGreaterThanZero(inv.igst());
            boolean hasCgstSgst = isGreaterThanZero(inv.cgst()) || isGreaterThanZero(inv.sgst());

            if (isInterState) {
                // Should only have IGST. If CGST/SGST found -> Wrong Intra-state split
                if (hasCgstSgst) {
                    mismatches.add(new PosMismatch(
                            inv.invoiceNo(), inv.placeOfSupply(), inv.igst(), inv.cgst(), inv.sgst(),
                            PosMismatch.MismatchType.INTERSTATE_SUPPLY_WITH_CGST_SGST
                    ));
                }
            } else {
                // Should only have CGST/SGST. If IGST found -> Wrong Inter-state split
                if (hasIgst) {
                    mismatches.add(new PosMismatch(
                            inv.invoiceNo(), inv.placeOfSupply(), inv.igst(), inv.cgst(), inv.sgst(),
                            PosMismatch.MismatchType.INTRASTATE_SUPPLY_WITH_IGST
                    ));
                }
            }
        }

        return new PosValidationResult(checked, mismatches.size(), mismatches);
    }

    private boolean isZeroRated(PosValidationInput.InvoiceData inv) {
        // If all taxes are 0, might be zero-rated or exempt. Skip.
        return !isGreaterThanZero(inv.igst()) && !isGreaterThanZero(inv.cgst()) && !isGreaterThanZero(inv.sgst());
    }

    private boolean isSezSupply(String tableSection) {
        if (tableSection == null) return false;
        String section = tableSection.toUpperCase();
        return section.contains("4B") || section.contains("4C") || section.contains("SEZ");
    }

    private String getPosState(String placeOfSupply) {
        if (placeOfSupply == null || placeOfSupply.length() < 2) return null;
        return placeOfSupply.substring(0, 2);
    }

    private boolean isGreaterThanZero(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
}
