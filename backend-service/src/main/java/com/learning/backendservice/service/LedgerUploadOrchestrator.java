package com.learning.backendservice.service;

import com.learning.backendservice.config.MemoryGuard;
import com.learning.backendservice.config.UploadProperties;
import com.learning.backendservice.domain.ledger.LedgerFileProcessor;
import com.learning.backendservice.domain.rule37.LedgerResult;
import com.learning.backendservice.dto.CreditWalletResponse;
import com.learning.backendservice.dto.UploadResult;
import com.learning.backendservice.entity.Rule37CalculationRun;
import com.learning.backendservice.exception.LedgerParseException;
import com.learning.backendservice.exception.TooManyRequestsException;
import com.learning.backendservice.repository.Rule37RunRepository;
import com.learning.common.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * Multi-file ledger upload orchestrator.
 * <p>
 * OOM-safe design (5-layer defense):
 * <ol>
 *   <li>Spring multipart disk-threshold (config) — files >1MB streamed to /tmp</li>
 *   <li>MemoryGuard pre-flight — rejects if estimated peak exceeds safe heap budget</li>
 *   <li>Semaphore concurrency throttle — max N uploads processed simultaneously</li>
 *   <li>Sequential per-file processing — one POI Workbook at a time</li>
 *   <li>JVM flags — HeapDumpOnOutOfMemoryError + ExitOnOutOfMemoryError</li>
 * </ol>
 */
@Service
public class LedgerUploadOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(LedgerUploadOrchestrator.class);

    private final LedgerFileProcessor ledgerFileProcessor;
    private final Rule37RunRepository runRepository;
    private final UploadProperties uploadProperties;
    private final CreditClient creditClient;
    private final MemoryGuard memoryGuard;
    private final Semaphore uploadSemaphore;
    private final int retentionDays;
    private final int maxRunsPerTenant;

    public LedgerUploadOrchestrator(LedgerFileProcessor ledgerFileProcessor,
                                    Rule37RunRepository runRepository,
                                    UploadProperties uploadProperties,
                                    CreditClient creditClient,
                                    MemoryGuard memoryGuard,
                                    @Value("${app.retention.days:7}") int retentionDays,
                                    @Value("${app.retention.max-runs-per-tenant:50}") int maxRunsPerTenant) {
        this.ledgerFileProcessor = ledgerFileProcessor;
        this.runRepository = runRepository;
        this.uploadProperties = uploadProperties;
        this.creditClient = creditClient;
        this.memoryGuard = memoryGuard;
        this.retentionDays = retentionDays;
        this.maxRunsPerTenant = maxRunsPerTenant;
        this.uploadSemaphore = new Semaphore(uploadProperties.getMaxConcurrentUploads());

        log.info("LedgerUploadOrchestrator initialized: maxFiles={}, maxConcurrentUploads={}, retentionDays={}",
                uploadProperties.getMaxFiles(), uploadProperties.getMaxConcurrentUploads(), retentionDays);
    }

    public UploadResult processUpload(List<MultipartFile> files, java.time.LocalDate asOnDate, String createdBy) {
        validateRequest(files);

        // ── Layer 2A: Pre-flight memory guard ──
        memoryGuard.checkMemoryBudget(files);

        // ── Layer 3: Concurrency throttle ──
        if (!uploadSemaphore.tryAcquire()) {
            log.warn("Upload rejected: semaphore exhausted, userId={}, fileCount={}", createdBy, files.size());
            throw new TooManyRequestsException(
                    "Server is processing other uploads. Please try again in a moment. "
                    + "(Max " + uploadProperties.getMaxConcurrentUploads() + " concurrent uploads allowed)");
        }

        try {
            return doProcessUpload(files, asOnDate, createdBy);
        } finally {
            uploadSemaphore.release();
        }
    }

    /**
     * Core processing logic — runs under semaphore protection.
     * @Transactional ensures DB operations are atomic (ISSUE-003).
     */
    @org.springframework.transaction.annotation.Transactional
    private UploadResult doProcessUpload(List<MultipartFile> files, java.time.LocalDate asOnDate, String createdBy) {
        // ── Per-tenant saved calculation limit ──
        String tenantId = TenantContext.getCurrentTenant();
        long currentCount = runRepository.countByTenantId(tenantId);
        if (currentCount >= maxRunsPerTenant) {
            throw new IllegalArgumentException(
                    "Maximum saved calculations (" + maxRunsPerTenant + ") reached. "
                    + "Delete old calculations or wait for expired ones to be removed.");
        }

        // ── Phase 1: Parse all files and count total ledgers (distinct suppliers) ──
        List<LedgerFileProcessor.ProcessingOutcome> outcomes = new ArrayList<>();
        List<UploadResult.FileUploadError> errors = new ArrayList<>();
        DataSize maxSize = uploadProperties.getMaxFileSize();
        int totalLedgerCount = 0;

        for (MultipartFile file : files) {
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
                LedgerFileProcessor.ProcessingOutcome outcome =
                        ledgerFileProcessor.processWithLedgerCount(
                                file.getInputStream(), file.getOriginalFilename(), asOnDate);
                outcomes.add(outcome);
                totalLedgerCount += outcome.ledgerCount();
                log.info("Ledger parsed: file={}, ledgersFound={}, userId={}",
                        file.getOriginalFilename(), outcome.ledgerCount(), createdBy);
            } catch (LedgerParseException e) {
                log.warn("Parse error for {}: {}", file.getOriginalFilename(), e.getMessage());
                errors.add(UploadResult.FileUploadError.builder()
                        .filename(file.getOriginalFilename())
                        .message(e.getMessage())
                        .build());
            } catch (Exception e) {
                log.warn("Processing error for {}: {}", file.getOriginalFilename(), e.getMessage());
                errors.add(UploadResult.FileUploadError.builder()
                        .filename(file.getOriginalFilename())
                        .message("Processing failed: " + e.getMessage())
                        .build());
            }
        }

        if (outcomes.isEmpty()) {
            throw new IllegalArgumentException("All files failed. " + errors.stream()
                    .map(e -> e.getFilename() + ": " + e.getMessage())
                    .collect(Collectors.joining("; ")));
        }

        // ── Phase 2: Pre-validate credits (1 credit per distinct supplier/ledger) ──
        creditClient.checkBalance(createdBy, totalLedgerCount);

        // ── Phase 3: Build and persist the calculation run FIRST (ISSUE-002) ──
        // Save before consuming credits so data is never lost.
        // If credit consumption fails, we rollback the saved run.
        List<LedgerResult> results = outcomes.stream()
                .map(LedgerFileProcessor.ProcessingOutcome::result)
                .toList();

        BigDecimal totalInterest = results.stream()
                .map(r -> r.getSummary().getTotalInterest())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalItcReversal = results.stream()
                .map(r -> r.getSummary().getTotalItcReversal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plus(retentionDays, ChronoUnit.DAYS);
        String filename = results.size() == 1
                ? results.get(0).getLedgerName()
                : results.size() + " files - " + asOnDate;

        Rule37CalculationRun run = Rule37CalculationRun.builder()
                .tenantId(tenantId)
                .filename(filename)
                .asOnDate(asOnDate)
                .totalInterest(totalInterest)
                .totalItcReversal(totalItcReversal)
                .calculationData(results)
                .createdAt(now)
                .createdBy(createdBy)
                .expiresAt(expiresAt)
                .build();

        run = runRepository.save(run);

        // ── Phase 4: Consume credits AFTER save (ISSUE-002) ──
        // If this fails, @Transactional ensures the saved run is rolled back.
        String idempotencyKey = "analysis-" + createdBy + "-" + UUID.randomUUID();
        CreditWalletResponse walletAfter;
        try {
            walletAfter = creditClient.consumeCredits(
                    createdBy, totalLedgerCount, idempotencyKey, idempotencyKey);
        } catch (Exception e) {
            log.error("Credit consumption failed after save, triggering rollback: userId={}, runId={}",
                    createdBy, run.getId(), e);
            throw e; // @Transactional will rollback the DB save
        }
        int remainingCredits = walletAfter.getRemaining();

        log.info("Credits consumed: userId={}, ledgers={}, creditsRemaining={}",
                createdBy, totalLedgerCount, remainingCredits);

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
                .creditsConsumed(totalLedgerCount)
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

    private static UploadResult.CalculationSummaryDto toSummaryDto(
            com.learning.backendservice.domain.rule37.CalculationSummary s) {
        return UploadResult.CalculationSummaryDto.builder()
                .totalInterest(s.getTotalInterest())
                .totalItcReversal(s.getTotalItcReversal())
                .atRiskCount(s.getAtRiskCount())
                .atRiskAmount(s.getAtRiskAmount())
                .breachedCount(s.getBreachedCount())
                .calculationDate(s.getCalculationDate() != null
                        ? s.getCalculationDate().toString() : null)
                .details(s.getDetails().stream()
                        .map(r -> UploadResult.InterestRowDto.builder()
                                .supplier(r.getSupplier())
                                .invoiceNumber(r.getInvoiceNumber())
                                .purchaseDate(r.getPurchaseDate() != null
                                        ? r.getPurchaseDate().toString() : null)
                                .paymentDate(r.getPaymentDate() != null
                                        ? r.getPaymentDate().toString() : null)
                                .originalInvoiceValue(r.getOriginalInvoiceValue())
                                .principal(r.getPrincipal())
                                .delayDays(r.getDelayDays())
                                .itcAmount(r.getItcAmount())
                                .interest(r.getInterest())
                                .status(r.getStatus().name())
                                .paymentDeadline(r.getPaymentDeadline() != null
                                        ? r.getPaymentDeadline().toString() : null)
                                .riskCategory(r.getRiskCategory() != null
                                        ? r.getRiskCategory().name() : null)
                                .gstr3bPeriod(r.getGstr3bPeriod())
                                .daysToDeadline(r.getDaysToDeadline())
                                .itcAvailmentDate(r.getItcAvailmentDate() != null
                                        ? r.getItcAvailmentDate().toString() : null)
                                .build())
                        .toList())
                .build();
    }
}
