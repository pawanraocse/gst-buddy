package com.learning.backendservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UploadResult {

    /** UUID v7 string — primary run identifier for all new runs */
    private String stringRunId;

    /**
     * @deprecated Use {@link #stringRunId} instead. Kept for backward compatibility
     *             during the frontend migration window.
     */
    @Deprecated
    private Long runId;
    private String filename;
    @Builder.Default
    private List<LedgerResultDto> results = new ArrayList<>();
    @Builder.Default
    private List<FileUploadError> errors = new ArrayList<>();

    /**
     * Generic findings list — populated for rules that do NOT produce LedgerResultDto
     * (e.g. GSTR-1 Late Fee, GSTR-3B Late Fee). Each entry is a summary of one
     * {@link com.learning.backendservice.engine.AuditFinding}.
     */
    @Builder.Default
    private List<FindingSummaryDto> findingsSummary = new ArrayList<>();

    @Builder.Default
    private Integer creditsConsumed = 0;

    @Builder.Default
    private Integer remainingCredits = 0;

    // Rule-specific structured results for the frontend side-by-side views
    private Object threeWayReconFindings;
    private Object itcMismatches;
    private Object rcmMismatches;

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FindingSummaryDto {
        private String ruleId;
        private String severity;
        private String legalBasis;
        private String compliancePeriod;
        private BigDecimal impactAmount;
        private String description;
        private String recommendedAction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LedgerResultDto {
        private String ledgerName;
        private CalculationSummaryDto summary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculationSummaryDto {
        private BigDecimal totalInterest;
        private BigDecimal totalItcReversal;
        private List<InterestRowDto> details;
        // Production enhancements — match saved-run shape
        @Builder.Default
        private int atRiskCount = 0;
        @Builder.Default
        private BigDecimal atRiskAmount = BigDecimal.ZERO;
        @Builder.Default
        private int breachedCount = 0;
        private String calculationDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterestRowDto {
        private String supplier;
        private String invoiceNumber;
        private String purchaseDate;
        private String paymentDate;
        private BigDecimal originalInvoiceValue;
        private BigDecimal principal;
        private int delayDays;
        private BigDecimal itcAmount;
        private BigDecimal interest;
        private String status;
        // Production enhancements — match saved-run shape
        private String paymentDeadline;
        private String riskCategory;
        private String gstr3bPeriod;
        private int daysToDeadline;
        private String itcAvailmentDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileUploadError {
        private String filename;
        private String message;
    }
}
