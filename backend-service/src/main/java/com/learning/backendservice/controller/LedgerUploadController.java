package com.learning.backendservice.controller;

import com.learning.backendservice.dto.UploadResult;
import com.learning.backendservice.service.Rule37CalculationRunService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/ledgers")
@RequiredArgsConstructor
@Tag(name = "Ledger Upload", description = "Upload Tally/Busy ledger Excel files for Rule 37 calculation")
public class LedgerUploadController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".xlsx", ".xls");

    private final Rule37CalculationRunService runService;

    @Operation(summary = "Upload ledger files", description = "Upload one or more Tally/Busy ledger Excel files (.xlsx, .xls)")
    @ApiResponse(responseCode = "201", description = "Upload successful", content = @Content(schema = @Schema(implementation = UploadResult.class)))
    @ApiResponse(responseCode = "400", description = "Validation error or all files failed")
    @ApiResponse(responseCode = "413", description = "File too large or too many files")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResult> uploadLedgers(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("asOnDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOnDate,
            HttpServletRequest request) {
        // Validate file extensions
        for (MultipartFile file : files) {
            String name = file.getOriginalFilename();
            if (name == null || ALLOWED_EXTENSIONS.stream().noneMatch(ext -> name.toLowerCase().endsWith(ext))) {
                throw new IllegalArgumentException(
                        "Invalid file type: '" + name + "'. Only .xlsx and .xls files are accepted.");
            }
        }

        String createdBy = request.getHeader(HeaderNames.USER_ID);
        if (createdBy == null || createdBy.isBlank()) {
            createdBy = "system";
        }
        UploadResult result = runService.processUpload(files, asOnDate, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
