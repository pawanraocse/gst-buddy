package com.learning.backendservice.domain.gstr1;

import java.math.BigDecimal;

/**
 * Immutable snapshot of a resolved CBIC relief window.
 *
 * <p>Pre-built by the orchestrator (service layer) from a {@code LateFeeReliefWindow}
 * entity, then passed into {@link Gstr1LateFeeInput} so the rule and calculator
 * remain database-free per the {@code AuditRule} contract.
 *
 * <p>Any field that is {@code null} means the standard rate/cap applies for
 * that dimension. The calculator checks for null and falls back to statutory defaults.
 *
 * @param notificationNo  CBIC notification reference, e.g. "Notification No. 19/2021-CT"
 * @param feeCgstPerDay   reduced CGST daily rate. {@code null} = fee waived (₹0).
 * @param feeSgstPerDay   reduced SGST daily rate. {@code null} = fee waived (₹0).
 * @param maxCapCgst      reduced CGST cap per return. {@code null} = standard cap applies.
 * @param maxCapSgst      reduced SGST cap per return. {@code null} = standard cap applies.
 */
public record ReliefWindowSnapshot(
        String     notificationNo,
        BigDecimal feeCgstPerDay,
        BigDecimal feeSgstPerDay,
        BigDecimal maxCapCgst,
        BigDecimal maxCapSgst
) {
    /** Convenience — maps a complete waiver notification (₹0 fees, ₹500 cap). */
    public static ReliefWindowSnapshot waiver(String notificationNo) {
        return new ReliefWindowSnapshot(
                notificationNo,
                BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("250"), new BigDecimal("250")
        );
    }
}
