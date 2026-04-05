package com.learning.backendservice.service;

import com.learning.backendservice.config.MemoryGuard;
import com.learning.backendservice.config.UploadProperties;
import com.learning.backendservice.dto.CreditWalletResponse;
import com.learning.backendservice.dto.UploadResult;
import com.learning.backendservice.engine.*;
import com.learning.backendservice.entity.AuditRun;
import com.learning.backendservice.entity.AuditRunFinding;
import com.learning.backendservice.exception.LedgerParseException;
import com.learning.backendservice.exception.TooManyRequestsException;
import com.learning.backendservice.repository.AuditFindingRepository;
import com.learning.backendservice.repository.AuditRunRepository;
import com.learning.backendservice.util.UuidV7;
import com.learning.common.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * Generic audit run orchestrator — coordinates file validation, rule execution,
 * credit consumption, and persistence across all GST compliance rules.
 *
 * <p><b>OOM-safe design (5-layer defense):</b>
 * <ol>
 *   <li>Spring multipart disk-threshold (config) — files &gt;1MB streamed to /tmp</li>
 *   <li>MemoryGuard pre-flight — rejects if estimated peak exceeds safe heap budget</li>
 *   <li>Semaphore concurrency throttle — max N uploads processed simultaneously</li>
 *   <li>Rule execution is sequential per-file (no parallel stream within a run)</li>
 *   <li>JVM flags — HeapDumpOnOutOfMemoryError + ExitOnOutOfMemoryError</li>
 * </ol>
 *
 * <p><b>Transaction boundary:</b> DB persist + credit consume are in one
 * {@code @Transactional} scope. If credit consumption fails, the saved run is
 * rolled back automatically.
 */
@Service
public class AuditRunOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AuditRunOrchestrator.class);

    private final AuditRuleRegistry ruleRegistry;
    private final AuditRunRepository runRepository;
    private final AuditFindingRepository findingRepository;
    private final UploadProperties uploadProperties;
    private final CreditClient creditClient;
    private final MemoryGuard memoryGuard;
    private final Semaphore uploadSemaphore;
    private final int retentionDays;
    private final int maxRunsPerTenant;

    public AuditRunOrchestrator(
            AuditRuleRegistry ruleRegistry,
            AuditRunRepository runRepository,
            AuditFindingRepository findingRepository,
            UploadProperties uploadProperties,
            CreditClient creditClient,
            MemoryGuard memoryGuard,
            @Value("${app.retention.days:7}") int retentionDays,
            @Value("${app.retention.max-runs-per-tenant:50}") int maxRunsPerTenant) {
        this.ruleRegistry = ruleRegistry;
        this.runRepository = runRepository;
        this.findingRepository = findingRepository;
        this.uploadProperties = uploadProperties;
        this.creditClient = creditClient;
        this.memoryGuard = memoryGuard;
        this.retentionDays = retentionDays;
        this.maxRunsPerTenant = maxRunsPerTenant;
        this.uploadSemaphore = new Semaphore(uploadProperties.getMaxConcurrentUploads());

        log.info("AuditRunOrchestrator initialized: maxFiles={}, maxConcurrentUploads={}, retentionDays={}",
                uploadProperties.getMaxFiles(), uploadProperties.getMaxConcurrentUploads(), retentionDays);
    }

    /**
     * Process a multi-file upload for the specified audit rule.
     *
     * @param files     uploaded ledger/document files
     * @param asOnDate  compliance evaluation date
     * @param ruleId    rule identifier (must exist in {@link AuditRuleRegistry})
     * @param userId    Keycloak user subject
     * @return upload result containing run ID, findings summary, credit info, and errors
     */
    public UploadResult processUpload(
            List<MultipartFile> files, LocalDate asOnDate, String ruleId, String userId) {

        // ── Validate: ruleId must exist ──
        if (!ruleRegistry.hasRule(ruleId)) {
            throw new IllegalArgumentException(
                    "Unknown audit rule: '" + ruleId + "'. Check /api/v1/audit/rules for available rules.");
        }

        // ── Validate: file list ──
        validateFiles(files);

        // ── Layer 2: Pre-flight memory guard ──
        memoryGuard.checkMemoryBudget(files);

        // ── Layer 3: Concurrency throttle ──
        if (!uploadSemaphore.tryAcquire()) {
            log.warn("Upload rejected: semaphore exhausted, userId={}, ruleId={}, fileCount={}",
                    userId, ruleId, files.size());
            throw new TooManyRequestsException(
                    "Server is processing other uploads. Please try again in a moment. "
                    + "(Max " + uploadProperties.getMaxConcurrentUploads() + " concurrent uploads allowed)");
        }

        try {
            return doProcessUpload(files, asOnDate, ruleId, userId);
        } finally {
            uploadSemaphore.release();
        }
    }

    /**
     * Core processing logic — runs under semaphore protection.
     * @Transactional ensures DB + credit operations are atomic.
     */
    @Transactional
    private UploadResult doProcessUpload(
            List<MultipartFile> files, LocalDate asOnDate, String ruleId, String userId) {

        String tenantId = TenantContext.getCurrentTenant();

        // ── Per-tenant run limit ──
        long currentCount = runRepository.countByTenantId(tenantId);
        if (currentCount >= maxRunsPerTenant) {
            throw new IllegalArgumentException(
                    "Maximum saved audit runs (" + maxRunsPerTenant + ") reached. "
                    + "Delete old runs or wait for expired ones to be cleaned up.");
        }

        // ── Validate file sizes and filter empties ──
        List<MultipartFile> validFiles = new ArrayList<>();
        List<UploadResult.FileUploadError> errors = new ArrayList<>();
        DataSize maxSize = uploadProperties.getMaxFileSize();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                errors.add(UploadResult.FileUploadError.builder()
                        .filename(file.getOriginalFilename()).message("File is empty").build());
                continue;
            }
            if (file.getSize() > maxSize.toBytes()) {
                errors.add(UploadResult.FileUploadError.builder()
                        .filename(file.getOriginalFilename())
                        .message("File exceeds max size " + maxSize).build());
                continue;
            }
            validFiles.add(file);
        }

        if (validFiles.isEmpty()) {
            throw new IllegalArgumentException(
                    "All files failed validation. " + errors.stream()
                    .map(e -> e.getFilename() + ": " + e.getMessage())
                    .collect(Collectors.joining("; ")));
        }

        // ── Execute the audit rule ──
        AuditRule<List<MultipartFile>, Object> rule = ruleRegistry.getRule(ruleId);
        AuditContext ctx = AuditContext.of(tenantId, userId, asOnDate);

        AuditRuleResult<Object> ruleResult;
        try {
            ruleResult = rule.execute(validFiles, ctx);
        } catch (LedgerParseException e) {
            errors.add(UploadResult.FileUploadError.builder()
                    .filename("unknown").message(e.getMessage()).build());
            throw e;
        }

        // ── Phase 1: Pre-validate credits BEFORE persisting ──
        int totalLedgerCount = ruleResult.creditsConsumed();
        creditClient.checkBalance(userId, totalLedgerCount);

        // ── Phase 2: Persist AuditRun (with UUID v7) ──
        UUID runId = UuidV7.generate();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plus(retentionDays, ChronoUnit.DAYS);

        // Derive filename from files
        String filename = validFiles.size() == 1
                ? validFiles.get(0).getOriginalFilename()
                : validFiles.size() + " files - " + asOnDate;

        AuditRun run = AuditRun.builder()
                .id(runId)
                .tenantId(tenantId)
                .userId(userId)
                .ruleId(ruleId)
                .status("SUCCESS")
                .inputMetadata(java.util.Map.of(
                        "asOnDate", asOnDate.toString(),
                        "filename", filename,
                        "fileCount", validFiles.size()))
                .resultData(ruleResult.ruleSpecificOutput())
                .totalImpactAmount(ruleResult.totalImpact())
                .creditsConsumed(totalLedgerCount)
                .createdAt(now)
                .completedAt(now)
                .expiresAt(expiresAt)
                .build();

        run = runRepository.save(run);

        // ── Phase 3: Persist findings ──
        List<AuditRunFinding> findingEntities = new ArrayList<>();
        for (AuditFinding f : ruleResult.findings()) {
            findingEntities.add(AuditRunFinding.builder()
                    .id(UuidV7.generate())
                    .auditRun(run)
                    .tenantId(tenantId)
                    .ruleId(f.ruleId())
                    .severity(f.severity().name())
                    .legalBasis(f.legalBasis())
                    .compliancePeriod(f.compliancePeriod())
                    .impactAmount(f.impactAmount())
                    .description(f.description())
                    .recommendedAction(f.recommendedAction())
                    .autoFixAvailable(f.autoFixAvailable())
                    .createdAt(now)
                    .build());
        }
        findingRepository.saveAll(findingEntities);

        // ── Phase 4: Consume credits AFTER save ──
        // On failure, @Transactional rolls back the DB save automatically.
        String idempotencyKey = "audit-" + run.getId();
        CreditWalletResponse walletAfter;
        try {
            walletAfter = creditClient.consumeCredits(userId, totalLedgerCount, idempotencyKey, idempotencyKey);
        } catch (Exception e) {
            log.error("Credit consumption failed after save, triggering rollback: userId={}, runId={}",
                    userId, run.getId(), e);
            throw e;
        }

        log.info("AuditRunOrchestrator completed: runId={}, ruleId={}, tenantId={}, findings={}, impact={}, creditsRemaining={}",
                run.getId(), ruleId, tenantId, findingEntities.size(), ruleResult.totalImpact(), walletAfter.getRemaining());

        return buildUploadResult(run, ruleResult, errors, walletAfter.getRemaining());
    }

    // ─── Private Helpers ────────────────────────────────────────────────────

    private void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided");
        }
        if (files.size() > uploadProperties.getMaxFiles()) {
            throw new IllegalArgumentException("Too many files. Max: " + uploadProperties.getMaxFiles());
        }
    }

    private UploadResult buildUploadResult(
            AuditRun run,
            AuditRuleResult<Object> ruleResult,
            List<UploadResult.FileUploadError> errors,
            int remainingCredits) {

        // For Rule 37 backward compatibility: extract LedgerResult DTOs from ruleSpecificOutput
        List<UploadResult.LedgerResultDto> resultDtos = new ArrayList<>();
        if (ruleResult.ruleSpecificOutput() instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof com.learning.backendservice.domain.rule37.LedgerResult lr) {
                    resultDtos.add(UploadResult.LedgerResultDto.builder()
                            .ledgerName(lr.getLedgerName())
                            .summary(toSummaryDto(lr.getSummary()))
                            .build());
                }
            }
        }

        return UploadResult.builder()
                .stringRunId(run.getId().toString())
                .filename((String) ((java.util.Map<?,?>) run.getInputMetadata()).get("filename"))
                .results(resultDtos)
                .errors(errors)
                .creditsConsumed(run.getCreditsConsumed())
                .remainingCredits(remainingCredits)
                .build();
    }

    private static UploadResult.CalculationSummaryDto toSummaryDto(
            com.learning.backendservice.domain.rule37.CalculationSummary s) {
        return UploadResult.CalculationSummaryDto.builder()
                .totalInterest(s.getTotalInterest())
                .totalItcReversal(s.getTotalItcReversal())
                .atRiskCount(s.getAtRiskCount())
                .atRiskAmount(s.getAtRiskAmount())
                .breachedCount(s.getBreachedCount())
                .calculationDate(s.getCalculationDate() != null ? s.getCalculationDate().toString() : null)
                .details(s.getDetails().stream()
                        .map(r -> UploadResult.InterestRowDto.builder()
                                .supplier(r.getSupplier())
                                .invoiceNumber(r.getInvoiceNumber())
                                .purchaseDate(r.getPurchaseDate() != null ? r.getPurchaseDate().toString() : null)
                                .paymentDate(r.getPaymentDate() != null ? r.getPaymentDate().toString() : null)
                                .originalInvoiceValue(r.getOriginalInvoiceValue())
                                .principal(r.getPrincipal())
                                .delayDays(r.getDelayDays())
                                .itcAmount(r.getItcAmount())
                                .interest(r.getInterest())
                                .status(r.getStatus().name())
                                .paymentDeadline(r.getPaymentDeadline() != null ? r.getPaymentDeadline().toString() : null)
                                .riskCategory(r.getRiskCategory() != null ? r.getRiskCategory().name() : null)
                                .gstr3bPeriod(r.getGstr3bPeriod())
                                .daysToDeadline(r.getDaysToDeadline())
                                .itcAvailmentDate(r.getItcAvailmentDate() != null ? r.getItcAvailmentDate().toString() : null)
                                .build())
                        .toList())
                .build();
    }
}
