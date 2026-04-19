package com.learning.backendservice.engine;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Immutable execution context passed to every {@link AuditRule#execute} invocation.
 *
 * <p>Extended from the original single-rule context to support the document-centric
 * pipeline: multi-document uploads, typed user params, and pre-loaded shared resources.
 *
 * <p>Rules must NOT modify or store the context. All fields are immutable.
 *
 * @param tenantId        multi-tenant identifier (from request header / TenantContext)
 * @param userId          Keycloak user subject (sub claim) triggering this run
 * @param financialYear   GST financial year in "YYYY-YY" format, e.g. "2024-25"
 * @param asOnDate        date as of which the compliance position is evaluated
 * @param analysisMode    the analysis mode selected by the user
 * @param documents       parsed documents in this analysis run (immutable list)
 * @param userParams      typed user-supplied parameters (QRMP flag, nil-return flag, etc.)
 * @param sharedResources pre-loaded shared DB resources (relief windows, state codes)
 * @param stateCode       2-digit state code extracted from the primary GSTIN (e.g. "29" for Karnataka)
 */
public record AuditContext(
        String tenantId,
        String userId,
        String financialYear,
        LocalDate asOnDate,
        AnalysisMode analysisMode,
        List<AuditDocument> documents,
        AuditUserParams userParams,
        SharedResources sharedResources,
        String stateCode
) {

    /**
     * Derive GST financial year string from a given date.
     * GST FY runs April–March.
     *
     * @param date any date within the desired FY
     * @return FY string, e.g. "2024-25"
     */
    public static String deriveFinancialYear(LocalDate date) {
        int year = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
        return year + "-" + String.format("%02d", (year + 1) % 100);
    }

    /**
     * Backward-compatible factory for existing single-rule orchestrator code.
     * Produces a LEDGER_ANALYSIS context with no documents and empty resources.
     *
     * @param tenantId tenant identifier
     * @param userId   Keycloak sub claim
     * @param asOnDate compliance evaluation date
     */
    public static AuditContext of(String tenantId, String userId, LocalDate asOnDate) {
        return new AuditContext(
                tenantId, userId, deriveFinancialYear(asOnDate), asOnDate,
                AnalysisMode.LEDGER_ANALYSIS, List.of(),
                AuditUserParams.defaults(), SharedResources.empty(), null
        );
    }

    /**
     * Full factory for the new document-centric pipeline.
     * Derives {@code stateCode} automatically from the primary GSTIN in the document set.
     *
     * @param tenantId  tenant identifier
     * @param userId    Keycloak sub claim
     * @param asOnDate  compliance evaluation date
     * @param mode      selected analysis mode
     * @param documents parsed and classified document set
     * @param params    user-supplied parameters (QRMP, nil-return, turnover, etc.)
     * @param resources pre-loaded shared DB resources (call {@link SharedResources#empty()} initially)
     */
    public static AuditContext forAnalysis(
            String tenantId, String userId, LocalDate asOnDate,
            AnalysisMode mode, List<AuditDocument> documents,
            AuditUserParams params, SharedResources resources) {

        String stateCode = documents.stream()
                .map(AuditDocument::gstin)
                .filter(g -> g != null && g.length() >= 2)
                .map(g -> g.substring(0, 2))
                .findFirst()
                .orElse(null);

        return new AuditContext(
                tenantId, userId, deriveFinancialYear(asOnDate), asOnDate,
                mode, List.copyOf(documents), params, resources, stateCode
        );
    }

    /**
     * Rebuild this context with enriched shared resources (immutable pattern).
     * Called by the orchestrator after {@code ContextEnricher.loadResources()}.
     */
    public AuditContext withSharedResources(SharedResources resources) {
        return new AuditContext(
                tenantId, userId, financialYear, asOnDate,
                analysisMode, documents, userParams, resources, stateCode
        );
    }

    // ── Document helpers ──

    /** Returns the first document of the given type, if present. */
    public Optional<AuditDocument> getDocument(DocumentType type) {
        return documents.stream()
                .filter(d -> d.documentType() == type)
                .findFirst();
    }

    /** Returns all documents of the given type (multi-period uploads). */
    public List<AuditDocument> getDocuments(DocumentType type) {
        return documents.stream()
                .filter(d -> d.documentType() == type)
                .toList();
    }

    /** Returns {@code true} if at least one document of the given type is present. */
    public boolean hasDocument(DocumentType type) {
        return documents.stream().anyMatch(d -> d.documentType() == type);
    }

    /** Returns the set of all document types present in this analysis run. */
    public Set<DocumentType> getAvailableDocumentTypes() {
        return documents.stream()
                .map(AuditDocument::documentType)
                .collect(Collectors.toUnmodifiableSet());
    }
}
