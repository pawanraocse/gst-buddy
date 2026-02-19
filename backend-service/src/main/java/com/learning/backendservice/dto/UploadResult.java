package com.learning.backendservice.dto;

import com.learning.backendservice.domain.rule37.LedgerResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    /** Number of credits consumed for this analysis. */
    @Builder.Default
    private Integer creditsConsumed = 0;

    /** Remaining credits after this analysis. */
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
        private double totalInterest;
        private double totalItcReversal;
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
        private double principal;
        private int delayDays;
        private double itcAmount;
        private double interest;
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
