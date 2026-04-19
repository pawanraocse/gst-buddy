package com.learning.backendservice.engine;

import java.math.BigDecimal;

/**
 * Typed user-supplied parameters for an analysis run, sent alongside file uploads.
 *
 * <p>Replaces the previous untyped {@code Map<String,Object> userParams}.
 * The {@code InputResolver} for each rule extracts the relevant subset.
 *
 * @param isQrmp              whether the taxpayer is a QRMP filer
 *                            (affects GSTR-1 / 3B due dates and late fee calculations)
 * @param isNilReturn         whether the return being assessed was a nil return
 *                            (affects late fee caps per Section 47)
 * @param aggregateTurnover   annual aggregate turnover in INR
 *                            (affects GSTR-9 exemption thresholds and Rule 86B applicability)
 * @param stateCode           optional override state code (2-digit, e.g. "29" for Karnataka).
 *                            If {@code null}, derived from GSTIN positions 0–1.
 */
public record AuditUserParams(
        boolean isQrmp,
        boolean isNilReturn,
        BigDecimal aggregateTurnover,
        String stateCode
) {
    /**
     * Safe defaults — non-QRMP, non-nil, no turnover provided, no state override.
     * Used for backward-compatible {@code processUpload()} wrapper calls.
     */
    public static AuditUserParams defaults() {
        return new AuditUserParams(false, false, null, null);
    }
}
