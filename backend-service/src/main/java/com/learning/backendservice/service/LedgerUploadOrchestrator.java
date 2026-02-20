package com.learning.backendservice.service;

import com.learning.backendservice.config.UploadProperties;
import com.learning.backendservice.domain.ledger.LedgerFileProcessor;
import com.learning.backendservice.domain.rule37.LedgerResult;
import com.learning.backendservice.dto.CreditWalletResponse;
import com.learning.backendservice.dto.UploadResult;
import com.learning.backendservice.entity.Rule37CalculationRun;
import com.learning.backendservice.exception.LedgerParseException;
import com.learning.backendservice.repository.Rule37RunRepository;
import com.learning.common.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Multi-file ledger upload orchestrator. OOM-safe: processes files sequentially.
 */
@Service
public class LedgerUploadOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(LedgerUploadOrchestrator.class);

    private final LedgerFileProcessor ledgerFileProcessor;
    private final Rule37RunRepository runRepository;
    private final UploadProperties uploadProperties;
    private final CreditClient creditClient;
    private final int retentionDays;
    private final int maxRunsPerTenant;

    public LedgerUploadOrchestrator(LedgerFileProcessor ledgerFileProcessor,
                                    Rule37RunRepository runRepository,
                                    UploadProperties uploadProperties,
                                    CreditClient creditClient,
                                    @Value("${app.retention.days:7}") int retentionDays,
                                    @Value("${app.retention.max-runs-per-tenant:50}") int maxRunsPerTenant) {
        this.ledgerFileProcessor = ledgerFileProcessor;
        this.runRepository = runRepository;
        this.uploadProperties = uploadProperties;
        this.creditClient = creditClient;
        this.retentionDays = retentionDays;
        this.maxRunsPerTenant = maxRunsPerTenant;
    }

    public UploadResult processUpload(List<MultipartFile> files, java.time.LocalDate asOnDate, String createdBy) {
        validateRequest(files);

        // ── Per-tenant saved calculation limit ──
        String tenantId = TenantContext.getCurrentTenant();
        long currentCount = runRepository.countByTenantId(tenantId);
        if (currentCount >= maxRunsPerTenant) {
            throw new IllegalArgumentException(
                    "Maximum saved calculations (" + maxRunsPerTenant + ") reached. "
                    + "Delete old calculations or wait for expired ones to be removed.");
        }

        // ── Pre-validate: user must have at least 1 credit ──
        CreditWalletResponse wallet = creditClient.checkBalance(createdBy, 1);
        int remainingCredits = wallet.getRemaining();

        List<LedgerResult> results = new ArrayList<>();
        List<UploadResult.FileUploadError> errors = new ArrayList<>();
        DataSize maxSize = uploadProperties.getMaxFileSize();
        int creditsConsumed = 0;

        for (MultipartFile file : files) {
            // ── Per-ledger credit check ──
            if (remainingCredits < 1) {
                errors.add(UploadResult.FileUploadError.builder()
                        .filename(file.getOriginalFilename())
                        .message("Insufficient credits. Purchase more credits to continue.")
                        .build());
                continue;
            }

            if (file.isEmpty()) {
                errors.add(UploadResult.FileUploadError.builder()
                        .filename(file.getOriginalFilename())
                        .message("File is empty")
                        .build());
                continue;
            }
            if (file.getSize() > maxSize.toBytes()) {
                errors.add(UploadResult.FileUploadError.builder()
                        .filename(file.getOriginalFilename())
                        .message("File exceeds max size " + maxSize)
                        .build());
                continue;
            }

            try {
                // Parse first (no credit charged if parsing fails)
                LedgerResult result = ledgerFileProcessor.process(file.getInputStream(), file.getOriginalFilename(), asOnDate);

                // Consume 1 credit after successful parse
                String idempotencyKey = "analysis-" + createdBy + "-" + UUID.randomUUID();
                CreditWalletResponse walletAfter = creditClient.consumeCredits(
                        createdBy, 1, idempotencyKey, idempotencyKey);
                remainingCredits = walletAfter.getRemaining();
                creditsConsumed++;

                results.add(result);
                log.info("Ledger processed: file={}, userId={}, creditsRemaining={}",
                        file.getOriginalFilename(), createdBy, remainingCredits);
            } catch (LedgerParseException e) {
                log.warn("Parse error for {}: {}", file.getOriginalFilename(), e.getMessage());
                errors.add(UploadResult.FileUploadError.builder()
                        .filename(file.getOriginalFilename())
                        .message(e.getMessage())
                        .build());
            } catch (com.learning.backendservice.exception.InsufficientCreditsException e) {
                log.warn("Credits exhausted during processing for {}: {}", file.getOriginalFilename(), e.getMessage());
                errors.add(UploadResult.FileUploadError.builder()
                        .filename(file.getOriginalFilename())
                        .message("Insufficient credits. Purchase more credits to continue.")
                        .build());
                remainingCredits = 0; // Skip remaining files
            } catch (Exception e) {
                log.warn("Processing error for {}: {}", file.getOriginalFilename(), e.getMessage());
                errors.add(UploadResult.FileUploadError.builder()
                        .filename(file.getOriginalFilename())
                        .message("Processing failed: " + e.getMessage())
                        .build());
            }
        }

        if (results.isEmpty()) {
            throw new IllegalArgumentException("All files failed. " + errors.stream()
                    .map(e -> e.getFilename() + ": " + e.getMessage())
                    .collect(Collectors.joining("; ")));
        }

        double totalInterest = results.stream()
                .mapToDouble(r -> r.getSummary().getTotalInterest())
                .sum();
        double totalItcReversal = results.stream()
                .mapToDouble(r -> r.getSummary().getTotalItcReversal())
                .sum();

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plus(retentionDays, ChronoUnit.DAYS);
        String filename = results.size() == 1
                ? results.get(0).getLedgerName()
                : results.size() + " files - " + asOnDate;

        Rule37CalculationRun run = Rule37CalculationRun.builder()
                .tenantId(tenantId)
                .filename(filename)
                .asOnDate(asOnDate)
                .totalInterest(java.math.BigDecimal.valueOf(totalInterest))
                .totalItcReversal(java.math.BigDecimal.valueOf(totalItcReversal))
                .calculationData(results)
                .createdAt(now)
                .createdBy(createdBy)
                .expiresAt(expiresAt)
                .build();

        run = runRepository.save(run);

        List<UploadResult.LedgerResultDto> resultDtos = results.stream()
                .map(r -> UploadResult.LedgerResultDto.builder()
                        .ledgerName(r.getLedgerName())
                        .summary(toSummaryDto(r.getSummary()))
                        .build())
                .toList();

        return UploadResult.builder()
                .runId(run.getId())
                .filename(run.getFilename())
                .results(resultDtos)
                .errors(errors)
                .creditsConsumed(creditsConsumed)
                .remainingCredits(remainingCredits)
                .build();
    }

    private void validateRequest(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided");
        }
        if (files.size() > uploadProperties.getMaxFiles()) {
            throw new IllegalArgumentException("Too many files. Max: " + uploadProperties.getMaxFiles());
        }
    }

    private static UploadResult.CalculationSummaryDto toSummaryDto(com.learning.backendservice.domain.rule37.CalculationSummary s) {
        return UploadResult.CalculationSummaryDto.builder()
                .totalInterest(s.getTotalInterest())
                .totalItcReversal(s.getTotalItcReversal())
                .details(s.getDetails().stream()
                        .map(r -> UploadResult.InterestRowDto.builder()
                                .supplier(r.getSupplier())
                                .purchaseDate(r.getPurchaseDate() != null ? r.getPurchaseDate().toString() : null)
                                .paymentDate(r.getPaymentDate() != null ? r.getPaymentDate().toString() : "Unpaid")
                                .principal(r.getPrincipal())
                                .delayDays(r.getDelayDays())
                                .itcAmount(r.getItcAmount())
                                .interest(r.getInterest())
                                .status(r.getStatus().name())
                                .build())
                        .toList())
                .build();
    }

    private static String getFileNameWithoutExtension(String filename) {
        if (filename == null) return "Unknown";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
