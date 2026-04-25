package com.learning.backendservice.domain.gstr2a;

/**
 * Immutable snapshot of GSTIN status loaded into SharedResources.
 */
public record GstinStatusSnapshot(
        String status,
        String lastFilingPeriod
) {}
