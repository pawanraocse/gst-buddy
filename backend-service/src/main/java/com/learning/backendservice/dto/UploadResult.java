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

    @Builder.Default
    private Integer creditsConsumed = 0;

    @Builder.Default
    private Integer remainingCredits = 0;

    public boolean hasErrors() {
        return !errors.isEmpty();
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
