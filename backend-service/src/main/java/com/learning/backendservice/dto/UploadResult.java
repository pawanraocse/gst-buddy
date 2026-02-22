package com.learning.backendservice.dto;

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
public class UploadResult {

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
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterestRowDto {
        private String supplier;
        private String purchaseDate;
        private String paymentDate;
        private BigDecimal principal;
        private int delayDays;
        private BigDecimal itcAmount;
        private BigDecimal interest;
        private String status;
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
