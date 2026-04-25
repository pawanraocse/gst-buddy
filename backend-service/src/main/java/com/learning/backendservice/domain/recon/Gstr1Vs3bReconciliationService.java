package com.learning.backendservice.domain.recon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure calculator for GSTR-1 vs GSTR-3B reconciliation.
 *
 * <p><b>Legal basis</b>: Section 37 (GSTR-1 outward supply declaration) and
 * Section 39 (GSTR-3B self-assessment return), CGST Act 2017. A mismatch
 * between the two returns creates a risk of demand under Section 73/74.
 *
 * <p><b>Design</b>: Stateless, no Spring annotations, no DB access.
 * All arithmetic uses {@code BigDecimal} with scale=2 and {@code HALF_UP} rounding.
 *
 * <p><b>Algorithm</b>:
 * <ol>
 *   <li>For each tax head (IGST, CGST, SGST/UTGST, CESS):
 *     <ul>
 *       <li>delta = gstr1Amount − gstr3bAmount</li>
 *       <li>deltaPercent = |delta| / denominator × 100, where denominator =
 *           max(gstr1Amount, gstr3bAmount) — avoids divide-by-zero and gives
 *           the more conservative (higher) percentage.</li>
 *       <li>Classify severity using tolerance thresholds.</li>
 *     </ul>
 *   </li>
 *   <li>Overall severity = worst-case across all heads.</li>
 * </ol>
 *
 * <p><b>Severity thresholds</b> (|deltaPercent|):
 * <ul>
 *   <li>|delta| ≤ toleranceAmount AND |deltaPercent| ≤ tolerancePercent → MATCH</li>
 *   <li>|deltaPercent| &lt; 5%  → MINOR</li>
 *   <li>5% ≤ |deltaPercent| &lt; 20% → MATERIAL</li>
 *   <li>|deltaPercent| ≥ 20% → CRITICAL</li>
 * </ul>
 */
public class Gstr1Vs3bReconciliationService {

    private static final BigDecimal HUNDRED  = new BigDecimal("100");
    private static final BigDecimal FIVE     = new BigDecimal("5");
    private static final BigDecimal TWENTY   = new BigDecimal("20");
    private static final int        SCALE    = 2;
    private static final RoundingMode RM     = RoundingMode.HALF_UP;

    /**
     * Reconcile GSTR-1 declared liability against GSTR-3B Table 6.1 tax payable.
     *
     * @param input pre-built input carrying both documents' data and tolerance config
     * @return reconciliation result with per-head deltas and overall severity
     */
    public Gstr1Vs3bResult reconcile(Gstr1Vs3bInput input) {
        LiabilitySummary  g1  = input.gstr1Liability();
        TaxPaymentSummary g3b = input.gstr3bTaxPayable();

        List<ReconDelta> deltas = new ArrayList<>(4);
        deltas.add(computeDelta("IGST",       g1.igst(),      g3b.igst(),      input));
        deltas.add(computeDelta("CGST",       g1.cgst(),      g3b.cgst(),      input));
        deltas.add(computeDelta("SGST/UTGST", g1.sgstUtgst(), g3b.sgstUtgst(), input));
        deltas.add(computeDelta("CESS",       g1.cess(),      g3b.cess(),      input));

        BigDecimal totalDelta = deltas.stream()
                .map(d -> d.delta().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RM);

        ReconSeverity overall = deltas.stream()
                .map(ReconDelta::severity)
                .max(Enum::compareTo)
                .orElse(ReconSeverity.MATCH);

        String narrative = buildNarrative(input.taxPeriod(), overall, totalDelta, deltas);

        return new Gstr1Vs3bResult(
                input.taxPeriod(), List.copyOf(deltas), totalDelta, overall, narrative
        );
    }

    // ── Per-head delta computation ───────────────────────────────────────────

    private ReconDelta computeDelta(
            String taxHead,
            BigDecimal gstr1Amount,
            BigDecimal gstr3bAmount,
            Gstr1Vs3bInput input) {

        // delta = gstr1 − gstr3b  (positive → under-reported in 3B)
        BigDecimal delta = gstr1Amount.subtract(gstr3bAmount).setScale(SCALE, RM);

        // deltaPercent = |delta| / max(gstr1, gstr3b) × 100
        BigDecimal denominator = gstr1Amount.abs().max(gstr3bAmount.abs());
        BigDecimal deltaPercent;
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            // Both are zero → perfect MATCH
            deltaPercent = BigDecimal.ZERO;
        } else {
            deltaPercent = delta.abs()
                    .divide(denominator, 10, RM)
                    .multiply(HUNDRED)
                    .setScale(SCALE, RM);
        }

        ReconSeverity severity = classify(delta.abs(), deltaPercent,
                input.reconToleranceAmount(), input.reconTolerancePercent());

        return new ReconDelta(taxHead, gstr1Amount, gstr3bAmount, delta, deltaPercent, severity);
    }

    // ── Severity classification ──────────────────────────────────────────────

    private ReconSeverity classify(
            BigDecimal absDelta,
            BigDecimal deltaPercent,
            BigDecimal toleranceAmount,
            BigDecimal tolerancePercent) {

        // Convert stored fraction (0.0001) to percent (0.01) for comparison
        BigDecimal tolerancePct = tolerancePercent.multiply(HUNDRED).setScale(SCALE, RM);

        // Within tolerance on BOTH amount AND percent → MATCH
        if (absDelta.compareTo(toleranceAmount) <= 0
                && deltaPercent.compareTo(tolerancePct) <= 0) {
            return ReconSeverity.MATCH;
        }

        if (deltaPercent.compareTo(FIVE) < 0)   return ReconSeverity.MINOR;
        if (deltaPercent.compareTo(TWENTY) < 0) return ReconSeverity.MATERIAL;
        return ReconSeverity.CRITICAL;
    }

    // ── Narrative builder ────────────────────────────────────────────────────

    private String buildNarrative(
            YearMonth taxPeriod,
            ReconSeverity overall,
            BigDecimal totalDelta,
            List<ReconDelta> deltas) {

        if (overall == ReconSeverity.MATCH) {
            return String.format(
                    "GSTR-1 vs GSTR-3B reconciliation for %s: all tax heads match within tolerance. "
                  + "No corrective action required. (Section 37 + Section 39, CGST Act 2017)",
                    taxPeriod);
        }

        StringBuilder mismatches = new StringBuilder();
        for (ReconDelta d : deltas) {
            if (d.severity() != ReconSeverity.MATCH) {
                mismatches.append(String.format(
                        "%s: GSTR-1=₹%.2f, GSTR-3B=₹%.2f, delta=₹%.2f (%.2f%%) [%s]; ",
                        d.taxHead(), d.gstr1Amount(), d.gstr3bAmount(),
                        d.delta(), d.deltaPercent(), d.severity()));
            }
        }

        return String.format(
                "GSTR-1 vs GSTR-3B reconciliation for %s — Overall: %s. "
              + "Total absolute mismatch: ₹%.2f. "
              + "Mismatched heads: %s"
              + "Per Section 37 + Section 39, CGST Act 2017. "
              + "File GSTR-1 amendment or DRC-03 payment as appropriate.",
                taxPeriod, overall, totalDelta, mismatches);
    }
}
