package com.learning.backendservice.domain.recon;

import java.util.List;

public record Gstr1Vs3bVs9Result(
        String financialYear,
        List<ThreeWayReconDelta> deltas,
        String narrative,
        boolean requiresAction
) {}
