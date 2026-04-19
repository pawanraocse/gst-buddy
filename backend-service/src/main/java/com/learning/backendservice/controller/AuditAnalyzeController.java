package com.learning.backendservice.controller;

import com.learning.backendservice.dto.UploadResult;
import com.learning.backendservice.engine.AnalysisMode;
import com.learning.backendservice.engine.AuditUserParams;
import com.learning.backendservice.service.AuditRunOrchestrator;
import com.learning.common.constants.HeaderNames;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Unified audit analysis controller.
 *
 * <p>Replaces {@code LedgerUploadController} and {@code GstrDocumentUploadController}
 * as the canonical entry point for all GST compliance analysis.
 *
 * <p>The old controllers continue to function as backward-compatible wrappers
 * delegating to {@link AuditRunOrchestrator#analyzeDocuments}.
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Analysis", description = "Unified document-centric GST compliance analysis")
public class AuditAnalyzeController {

    private final AuditRunOrchestrator orchestrator;

    /**
     * Run a comprehensive audit analysis.
     *
     * <p>The server auto-discovers all applicable GST compliance rules based on the uploaded
     * document types and the selected analysis mode. Credits are consumed flat:
     * <ul>
     *   <li>LEDGER_ANALYSIS: 1 credit per run</li>
     *   <li>GSTR_RULES_ANALYSIS: 20 credits per run</li>
     * </ul>
     *
     * @param files              one or more document files (Excel, PDF, or portal JSON)
     * @param analysisMode       LEDGER_ANALYSIS or GSTR_RULES_ANALYSIS
     * @param asOnDate           compliance evaluation date (ISO-8601, e.g. 2025-03-31)
     * @param isQrmp             true if taxpayer is a QRMP (quarterly) filer (default false)
     * @param isNilReturn        true if the return is a nil-return filing (default false)
     * @param aggregateTurnover  annual aggregate turnover in INR (optional, for GSTR-9 threshold)
     * @return HTTP 201 with comprehensive findings response
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Run comprehensive audit analysis",
            description = "Upload documents and auto-discover all applicable GST compliance rules.")
    public ResponseEntity<UploadResult> analyze(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("analysisMode") AnalysisMode analysisMode,
            @RequestParam("asOnDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOnDate,
            @RequestParam(value = "isQrmp", defaultValue = "false") boolean isQrmp,
            @RequestParam(value = "isNilReturn", defaultValue = "false") boolean isNilReturn,
            @RequestParam(value = "aggregateTurnover", required = false) BigDecimal aggregateTurnover,
            HttpServletRequest request) {

        String userId = request.getHeader(HeaderNames.USER_ID);
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Missing required header: " + HeaderNames.USER_ID);
        }

        AuditUserParams params = new AuditUserParams(isQrmp, isNilReturn, aggregateTurnover, null);

        UploadResult result = orchestrator.analyzeDocuments(
                files, analysisMode, asOnDate, userId, params);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
