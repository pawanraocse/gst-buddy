package com.learning.backendservice.domain.recon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Gstr1Vs3bVs9ReconciliationService {

    private static final int SCALE = 2;
    private static final RoundingMode RM = RoundingMode.HALF_UP;

    public Gstr1Vs3bVs9Result reconcile(Gstr1Vs3bVs9Input input) {
        List<ThreeWayReconDelta> deltas = new ArrayList<>();
        boolean requiresAction = false;
        BigDecimal tolerance = input.reconToleranceAmount();

        // Heads: IGST, CGST, SGST, CESS
        requiresAction |= addDelta(deltas, "IGST", input.gstr1Aggregated().igst(), input.gstr3bAggregated().igst(),
                input.gstr9Table4().igst(), input.gstr9Table9Paid().igst(), tolerance);

        requiresAction |= addDelta(deltas, "CGST", input.gstr1Aggregated().cgst(), input.gstr3bAggregated().cgst(),
                input.gstr9Table4().cgst(), input.gstr9Table9Paid().cgst(), tolerance);

        requiresAction |= addDelta(deltas, "SGST", input.gstr1Aggregated().sgstUtgst(), input.gstr3bAggregated().sgstUtgst(),
                input.gstr9Table4().sgstUtgst(), input.gstr9Table9Paid().sgstUtgst(), tolerance);

        requiresAction |= addDelta(deltas, "CESS", input.gstr1Aggregated().cess(), input.gstr3bAggregated().cess(),
                input.gstr9Table4().cess(), input.gstr9Table9Paid().cess(), tolerance);

        String narrative = "3-way reconciliation completed successfully.";
        if (requiresAction) {
            narrative = "Mismatches found in 3-way reconciliation (GSTR-1 vs 3B vs 9). Please review the gaps and amend returns if necessary.";
        }

        return new Gstr1Vs3bVs9Result(input.financialYear(), deltas, narrative, requiresAction);
    }

    private boolean addDelta(List<ThreeWayReconDelta> deltas, String taxHead,
                             BigDecimal gstr1, BigDecimal gstr3b, BigDecimal gstr9Dec, BigDecimal gstr9Paid,
                             BigDecimal tolerance) {
        BigDecimal d1 = gstr1 != null ? gstr1.setScale(SCALE, RM) : BigDecimal.ZERO;
        BigDecimal d3b = gstr3b != null ? gstr3b.setScale(SCALE, RM) : BigDecimal.ZERO;
        BigDecimal d9Dec = gstr9Dec != null ? gstr9Dec.setScale(SCALE, RM) : BigDecimal.ZERO;
        BigDecimal d9Paid = gstr9Paid != null ? gstr9Paid.setScale(SCALE, RM) : BigDecimal.ZERO;

        BigDecimal delta1Vs3b = d1.subtract(d3b).setScale(SCALE, RM);
        BigDecimal delta3bVs9 = d3b.subtract(d9Paid).setScale(SCALE, RM);
        BigDecimal delta1Vs9 = d1.subtract(d9Dec).setScale(SCALE, RM);

        boolean hasMismatch = delta1Vs3b.abs().compareTo(tolerance) > 0 ||
                              delta3bVs9.abs().compareTo(tolerance) > 0 ||
                              delta1Vs9.abs().compareTo(tolerance) > 0;

        if (d1.compareTo(BigDecimal.ZERO) > 0 || d3b.compareTo(BigDecimal.ZERO) > 0 || 
            d9Dec.compareTo(BigDecimal.ZERO) > 0 || d9Paid.compareTo(BigDecimal.ZERO) > 0) {
            deltas.add(new ThreeWayReconDelta(taxHead, d1, d3b, d9Dec, d9Paid, delta1Vs3b, delta3bVs9, delta1Vs9));
        }

        return hasMismatch;
    }
}
