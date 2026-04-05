package com.learning.backendservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;


/**
 * DTO for a generic audit run response.
 * Replaces {@code Rule37RunResponse}; works for all GST compliance rules.
 *
 * <p>The {@code id} field is serialized as a UUID v7 string.
 * Migrated legacy runs will have UUID v4 strings.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditRunResponse {

    /** UUID v7 string — replaces the former Long id */
    private final String id;

    /** e.g. "RULE_37_ITC_REVERSAL" */
    private final String ruleId;

    /** Human-readable label, e.g. "Rule 37 — 180-Day ITC Reversal" */
    private final String ruleDisplayName;

    /** PENDING | RUNNING | SUCCESS | FAILED */
    private final String status;

    /** Aggregate financial impact (ITC reversal + interest + penalties) */
    private final BigDecimal totalImpactAmount;

    private final Integer creditsConsumed;

    private final OffsetDateTime createdAt;

    private final OffsetDateTime completedAt;

    private final OffsetDateTime expiresAt;

    private final String userId;

    /**
     * Rule-specific input params as-stored (e.g. asOnDate, filename).
     * Always included.
     */
    private final Object inputMetadata;

    /**
     * Rule-specific result data (e.g. List&lt;LedgerResult&gt; for Rule 37).
     * Only included when fetching a single run by ID (not in list responses).
     */
    private final Object resultData;
}
