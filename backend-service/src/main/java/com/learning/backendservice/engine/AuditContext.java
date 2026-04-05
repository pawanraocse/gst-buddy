package com.learning.backendservice.engine;

import java.time.LocalDate;

/**
 * Immutable execution context passed to every {@link AuditRule#execute} invocation.
 *
 * <p>The context is constructed by the orchestrator from the HTTP request and
 * Keycloak token claims. Rules must not modify or store the context.
 *
 * @param tenantId      Multi-tenant identifier (from request header / TenantContext)
 * @param userId        Keycloak user subject (sub claim) triggering this run
 * @param financialYear GST financial year in "YYYY-YY" format, e.g. "2024-25"
 * @param asOnDate      Date as of which the compliance position is evaluated
 */
public record AuditContext(
        String tenantId,
        String userId,
        String financialYear,
        LocalDate asOnDate
) {
    /**
     * Derive financial year string from an asOnDate.
     * GST FY runs April–March, so Apr-Mar = one financial year.
     *
     * @param date any date within the desired FY
     * @return FY string, e.g. "2024-25"
     */
    public static String deriveFinancialYear(LocalDate date) {
        int year = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
        return year + "-" + String.format("%02d", (year + 1) % 100);
    }

    /**
     * Convenience factory — derives the FY automatically from asOnDate.
     */
    public static AuditContext of(String tenantId, String userId, LocalDate asOnDate) {
        return new AuditContext(tenantId, userId, deriveFinancialYear(asOnDate), asOnDate);
    }
}
