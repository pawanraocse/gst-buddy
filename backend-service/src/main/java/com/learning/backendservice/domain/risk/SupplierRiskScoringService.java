package com.learning.backendservice.domain.risk;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service that scores supplier GSTIN risk based on registration status.
 * Legal basis: Section 16(2)(c), CGST Act 2017 — ITC is allowable only if
 * tax has actually been paid to the government by the supplier.
 *
 * <p>Pure domain service — no Spring annotations, no DB or API calls.
 */
public class SupplierRiskScoringService {

    public SupplierRiskResult evaluate(SupplierRiskInput input) {
        List<SupplierScore> scores = new ArrayList<>();
        int highRiskCount = 0;
        BigDecimal highRiskExposure = BigDecimal.ZERO;

        for (SupplierRiskInput.SupplierData sd : input.suppliers()) {
            // Section 16(2)(c): ITC only valid if supplier has paid tax to govt.
            // Risk tiers per PRD D-C:
            //   CANCELLED   → CRITICAL (ITC must be reversed immediately)
            //   NON_FILER   → HIGH     (tax not remitted → ITC at risk)
            //   SUSPENDED   → HIGH     (similar risk profile to non-filer)
            //   COMPOSITION → MEDIUM   (Section 10(4): no ITC from composition dealer)
            //   ACTIVE      → LOW      (safe)
            final int riskScore;
            final String riskCategory;
            final String normalizedStatus = sd.status() != null ? sd.status().toUpperCase() : "UNKNOWN";

            switch (normalizedStatus) {
                case "CANCELLED" -> {
                    riskScore = 100;
                    riskCategory = "CRITICAL";
                    highRiskCount++;
                    highRiskExposure = highRiskExposure.add(sd.totalTax());
                }
                case "NON_FILER", "SUSPENDED" -> {
                    riskScore = 80;
                    riskCategory = "HIGH";
                    highRiskCount++;
                    highRiskExposure = highRiskExposure.add(sd.totalTax());
                }
                case "COMPOSITION" -> {
                    riskScore = 50;
                    riskCategory = "MEDIUM";
                }
                default -> {
                    // ACTIVE or UNKNOWN → treat as low risk
                    riskScore = 10;
                    riskCategory = "LOW";
                }
            }

            scores.add(new SupplierScore(
                    sd.gstin(),
                    sd.name(),
                    normalizedStatus,
                    riskScore,
                    riskCategory,
                    sd.totalTax()
            ));
        }

        return new SupplierRiskResult(
                input.suppliers().size(),
                highRiskCount,
                highRiskExposure,
                scores
        );
    }
}
