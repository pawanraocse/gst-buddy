package com.learning.backendservice.controller;

import com.learning.backendservice.dto.UploadResult;
import com.learning.backendservice.service.AuditRunOrchestrator;
import com.learning.common.constants.HeaderNames;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Set;

/**
 * REST controller for GSTR document uploads (GSTR-1, and future GSTR-3B, GSTR-9).
 *
 * <p>Unlike {@link LedgerUploadController} which handles raw Excel ledger files,
 * this controller accepts PDF and JSON files exported directly from the GST portal.
 * After parsing via the Python sidecar, the appropriate audit rule is executed
 * and findings are returned immediately in the response.
 *
 * <p><b>Endpoint:</b> {@code POST /api/v1/gstr/upload}
 */
@RestController
@RequestMapping("/api/v1/gstr")
@RequiredArgsConstructor
@Tag(name = "GSTR Document Upload", description = "Upload GSTR-1 PDF or JSON files for compliance audit")
public class GstrDocumentUploadController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".json");

    private final AuditRunOrchestrator orchestrator;

    @Operation(
            summary = "Upload and audit GSTR-1 document",
            description = "Upload a GSTR-1 PDF or JSON file. The document is parsed by the Python sidecar, "
                        + "late fee exposure is computed per Section 47(1) CGST Act 2017, and findings are returned.")
    @ApiResponse(responseCode = "201", description = "Audit completed successfully",
            content = @Content(schema = @Schema(implementation = UploadResult.class)))
    @ApiResponse(responseCode = "400", description = "Invalid file type, missing header, or parse failure")
    @ApiResponse(responseCode = "402", description = "Insufficient credits")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResult> uploadGstrDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isQrmp",      defaultValue = "false") boolean isQrmp,
            @RequestParam(value = "isNilReturn",  defaultValue = "false") boolean isNilReturn,
            @RequestParam("asOnDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOnDate,
            HttpServletRequest request) {

        // ── Validate extension ──────────────────────────────────────────────
        String name = file.getOriginalFilename();
        if (name == null || ALLOWED_EXTENSIONS.stream().noneMatch(ext -> name.toLowerCase().endsWith(ext))) {
            throw new IllegalArgumentException(
                    "Invalid file type: '" + name + "'. Only .pdf and .json files are accepted for GSTR uploads.");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }

        // ── Validate user identity ──────────────────────────────────────────
        String userId = request.getHeader(HeaderNames.USER_ID);
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing required header: " + HeaderNames.USER_ID
                    + ". User identification is required for credit consumption.");
        }

        UploadResult result = orchestrator.processGstrUpload(file, isQrmp, isNilReturn, asOnDate, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
