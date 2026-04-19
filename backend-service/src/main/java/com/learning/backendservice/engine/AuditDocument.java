package com.learning.backendservice.engine;

import java.time.YearMonth;
import java.util.Map;

/**
 * A parsed document within the audit pipeline context.
 *
 * <p>Created by {@code DocumentTypeResolver} after classifying and parsing each
 * uploaded file. The raw {@link org.springframework.web.multipart.MultipartFile}
 * is NOT stored here — only the extracted, typed data that rules need.
 *
 * @param documentType     classified type of this document
 * @param originalFilename upload filename (for error messages / audit trail)
 * @param parsedJson       raw JSON returned by the Python parser sidecar
 *                         ({@code null} for ledger documents)
 * @param extractedFields  typed key fields extracted from the document
 *                         (e.g. {@code "arn_date"}, {@code "tax_period"}, {@code "gstin"}).
 *                         For ledger docs the raw {@code MultipartFile} is stored under key {@code "rawFile"}.
 * @param taxPeriod        the GST tax period this document belongs to (parsed from the document)
 * @param gstin            GSTIN extracted from the document, Modulo-11 validated by {@code DocumentTypeResolver}
 */
public record AuditDocument(
        DocumentType documentType,
        String originalFilename,
        String parsedJson,
        Map<String, Object> extractedFields,
        YearMonth taxPeriod,
        String gstin
) {}
