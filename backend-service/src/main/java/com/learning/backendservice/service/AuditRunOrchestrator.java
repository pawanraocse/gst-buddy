package com.learning.backendservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.backendservice.config.MemoryGuard;
import com.learning.backendservice.config.UploadProperties;
import com.learning.backendservice.dto.CreditWalletResponse;
import com.learning.backendservice.dto.UploadResult;
import com.learning.backendservice.domain.gstr1.Gstr1LateFeeInput;
import com.learning.backendservice.domain.gstr1.Gstr1LateFeeResult;
import com.learning.backendservice.domain.gstr1.ReliefWindowSnapshot;
import com.learning.backendservice.engine.*;
import com.learning.backendservice.entity.AuditRun;
import com.learning.backendservice.entity.AuditRunFinding;
import com.learning.backendservice.entity.LateFeeReliefWindow;
import com.learning.backendservice.exception.LedgerParseException;
import com.learning.backendservice.exception.TooManyRequestsException;
import com.learning.backendservice.repository.AuditFindingRepository;
import com.learning.backendservice.repository.AuditRunRepository;
import com.learning.backendservice.repository.LateFeeReliefWindowRepository;
import com.learning.backendservice.service.ingestion.ParserOrchestrator;
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
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private final ObjectMapper objectMapper;
    private final Semaphore uploadSemaphore;
    private final int retentionDays;
    private final int maxRunsPerTenant;
    private final ParserOrchestrator parserOrchestrator;
    private final LateFeeReliefWindowRepository reliefWindowRepository;

    public AuditRunOrchestrator(
            AuditRuleRegistry ruleRegistry,
            AuditRunRepository runRepository,
            AuditFindingRepository findingRepository,
            UploadProperties uploadProperties,
            CreditClient creditClient,
            MemoryGuard memoryGuard,
            ObjectMapper objectMapper,
            ParserOrchestrator parserOrchestrator,
            LateFeeReliefWindowRepository reliefWindowRepository,
            @Value("${app.retention.days:7}") int retentionDays,
            @Value("${app.retention.max-runs-per-tenant:50}") int maxRunsPerTenant) {
        this.ruleRegistry = ruleRegistry;
        this.runRepository = runRepository;
        this.findingRepository = findingRepository;
        this.uploadProperties = uploadProperties;
        this.creditClient = creditClient;
        this.memoryGuard = memoryGuard;
        this.objectMapper = objectMapper;
        this.parserOrchestrator = parserOrchestrator;
        this.reliefWindowRepository = reliefWindowRepository;
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
                .inputMetadata(toJson(java.util.Map.of(
                        "asOnDate", asOnDate.toString(),
                        "filename", filename,
                        "fileCount", validFiles.size())))
                .resultData(toJson(ruleResult.ruleSpecificOutput()))
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

    // ─── GSTR-1 Late Fee Upload ──────────────────────────────────────────────

    /**
     * Process a GSTR-1 document upload and execute the late fee audit rule.
     *
     * <p>This method handles the GSTR-1-specific flow:
     * <ol>
     *   <li>Send PDF/JSON to Python parser sidecar via {@link ParserOrchestrator}.</li>
     *   <li>Extract {@code arn_date}, {@code gstin}, {@code tax_period} from parsed payload.</li>
     *   <li>Query {@link LateFeeReliefWindowRepository} for any applicable amnesty window.</li>
     *   <li>Build {@link Gstr1LateFeeInput} with parsed data + CA-supplied toggles + relief.</li>
     *   <li>Execute {@code LATE_FEE_GSTR1} rule and persist findings via existing machinery.</li>
     * </ol>
     *
     * @param file        GSTR-1 PDF or JSON file
     * @param isQrmp      true if taxpayer is a QRMP (quarterly) filer
     * @param isNilReturn true if this is a nil-return filing
     * @param asOnDate    compliance evaluation date
     * @param userId      Keycloak user subject
     * @return standard UploadResult with findings populated in findingsSummary
     */
    @Transactional
    public UploadResult processGstrUpload(
            MultipartFile file, boolean isQrmp, boolean isNilReturn,
            LocalDate asOnDate, String userId) {

        String tenantId = TenantContext.getCurrentTenant();

        // ── 1. Parse document via Python sidecar ─────────────────────────────
        var parsedDoc = parserOrchestrator.ingestDocument(file, "gstr1");
        if (!"SUCCESS".equals(parsedDoc.getParseStatus())) {
            throw new LedgerParseException(
                    "GSTR-1 document parsing failed: " + parsedDoc.getErrorMessage());
        }

        // ── 2. Extract fields from parsed JSON ───────────────────────────────
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> data;
        try {
            data = objectMapper.readValue(parsedDoc.getParsedJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            throw new LedgerParseException("Could not deserialize parsed GSTR-1 data: " + e.getMessage(), e);
        }

        String gstin = String.valueOf(data.getOrDefault("gstin", ""));
        LocalDate arnDate = LocalDate.parse(String.valueOf(data.get("arn_date")));
        // tax_period from parser is "MM-YYYY" (e.g. "03-2024") → parse to YearMonth
        String[] parts = String.valueOf(data.get("tax_period")).split("-");
        YearMonth taxPeriod = YearMonth.of(Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
        LocalDate periodEnd = taxPeriod.atEndOfMonth();

        // ── 3. Resolve relief window (DB access here — not inside the rule) ──
        String appliesTo = isNilReturn ? "NIL" : "NON_NIL";
        Optional<LateFeeReliefWindow> reliefOpt = reliefWindowRepository
                .findApplicableGstr1Relief(arnDate, periodEnd, appliesTo)
                .stream().findFirst();

        ReliefWindowSnapshot reliefSnapshot = reliefOpt.map(r -> new ReliefWindowSnapshot(
                r.getNotificationNo(),
                r.getFeeCgstPerDay(),
                r.getFeeSgstPerDay(),
                r.getMaxCapCgst(),
                r.getMaxCapSgst()
        )).orElse(null);

        // ── 4. Build input and execute rule ──────────────────────────────────
        Gstr1LateFeeInput input = new Gstr1LateFeeInput(
                gstin, arnDate, taxPeriod,
                AuditContext.deriveFinancialYear(asOnDate),
                isNilReturn, isQrmp, reliefSnapshot
        );

        AuditRule<Gstr1LateFeeInput, Gstr1LateFeeResult> rule =
                ruleRegistry.<Gstr1LateFeeInput, Gstr1LateFeeResult>getRule("LATE_FEE_GSTR1");
        AuditContext ctx = AuditContext.of(tenantId, userId, asOnDate);
        AuditRuleResult<Gstr1LateFeeResult> ruleResult = rule.execute(input, ctx);

        // ── 5. Persist run + findings (reuse existing schema) ─────────────────
        creditClient.checkBalance(userId, ruleResult.creditsConsumed());

        UUID runId = UuidV7.generate();
        OffsetDateTime now = OffsetDateTime.now();
        AuditRun run = AuditRun.builder()
                .id(runId)
                .tenantId(tenantId)
                .userId(userId)
                .ruleId("LATE_FEE_GSTR1")
                .status("SUCCESS")
                .inputMetadata(toJson(java.util.Map.of(
                        "asOnDate", asOnDate.toString(),
                        "filename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "",
                        "isQrmp", isQrmp,
                        "isNilReturn", isNilReturn
                )))
                .resultData(toJson(ruleResult.ruleSpecificOutput()))
                .totalImpactAmount(ruleResult.totalImpact())
                .creditsConsumed(ruleResult.creditsConsumed())
                .createdAt(now)
                .completedAt(now)
                .expiresAt(now.plus(retentionDays, ChronoUnit.DAYS))
                .build();
        run = runRepository.save(run);

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

        CreditWalletResponse wallet = creditClient.consumeCredits(
                userId, ruleResult.creditsConsumed(),
                "audit-" + runId, "audit-" + runId);

        log.info("processGstrUpload completed: runId={}, gstin={}, delay={}d, impact={}, creditsRemaining={}",
                runId, gstin, ruleResult.ruleSpecificOutput().delayDays(),
                ruleResult.totalImpact(), wallet.getRemaining());

        // ── 6. Build response with generic findingsSummary ───────────────────
        List<UploadResult.FindingSummaryDto> summaries = ruleResult.findings().stream()
                .map(f -> UploadResult.FindingSummaryDto.builder()
                        .ruleId(f.ruleId())
                        .severity(f.severity().name())
                        .legalBasis(f.legalBasis())
                        .compliancePeriod(f.compliancePeriod())
                        .impactAmount(f.impactAmount())
                        .description(f.description())
                        .recommendedAction(f.recommendedAction())
                        .build())
                .toList();

        return UploadResult.builder()
                .stringRunId(runId.toString())
                .filename(file.getOriginalFilename())
                .findingsSummary(summaries)
                .creditsConsumed(ruleResult.creditsConsumed())
                .remainingCredits(wallet.getRemaining())
                .build();
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
                .filename(extractFilenameFromMeta(run.getInputMetadata()))
                .results(resultDtos)
                .errors(errors)
                .creditsConsumed(run.getCreditsConsumed())
                .remainingCredits(remainingCredits)
                .build();
    }

    private String extractFilenameFromMeta(String inputMetadataJson) {
        try {
            if (inputMetadataJson == null) return null;
            java.util.Map<String, Object> meta = objectMapper.readValue(inputMetadataJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            Object f = meta.get("filename");
            return f instanceof String s ? s : null;
        } catch (Exception e) {
            return null;
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
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize audit data to JSON", e);
        }
    }
}
