package com.learning.backendservice.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.backendservice.entity.ParsedDocument;
import com.learning.backendservice.exception.LedgerParseException;
import com.learning.backendservice.service.ingestion.ParserOrchestrator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;
import java.util.Map;

/**
 * Resolves uploaded MultipartFiles into typed AuditDocuments.
 * Calls the Python parser sidecar to classify and extract fields.
 */
@Component
@RequiredArgsConstructor
public class DocumentTypeResolver {

    private static final Logger log = LoggerFactory.getLogger(DocumentTypeResolver.class);
    private final ParserOrchestrator parserOrchestrator;
    private final ObjectMapper objectMapper;

    public AuditDocument resolve(MultipartFile file, AnalysisMode mode) {
        // If it's a raw purchase ledger for Rule 37
        if (mode == AnalysisMode.LEDGER_ANALYSIS) {
            return new AuditDocument(
                    DocumentType.PURCHASE_LEDGER,
                    file.getOriginalFilename(),
                    null,
                    Map.of("rawFile", file),
                    null,
                    null
            );
        }

        // For GSTR compliance, run through parser sidecar
        ParsedDocument parsedDoc = parserOrchestrator.ingestDocument(file, null);

        if (!"SUCCESS".equals(parsedDoc.getParseStatus())) {
            throw new LedgerParseException("Failed to parse document " + file.getOriginalFilename() + ": " + parsedDoc.getErrorMessage());
        }

        DocumentType docType = mapDocType(parsedDoc.getDocType());

        Map<String, Object> extractedFields;
        try {
            extractedFields = objectMapper.readValue(parsedDoc.getParsedJson(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new LedgerParseException("Failed to deserialize parsed JSON for " + file.getOriginalFilename(), e);
        }

        String gstin = (String) extractedFields.get("gstin");
        YearMonth taxPeriod = null;
        Object rawPeriod = extractedFields.get("tax_period");
        if (rawPeriod != null) {
            String periodStr = String.valueOf(rawPeriod);
            if (periodStr.contains("-")) {
                String[] parts = periodStr.split("-");
                try {
                    taxPeriod = YearMonth.of(Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
                } catch (Exception e) {
                    log.warn("Failed to parse tax_period: {}", periodStr);
                }
            }
        }

        return new AuditDocument(
                docType,
                file.getOriginalFilename(),
                parsedDoc.getParsedJson(),
                extractedFields,
                taxPeriod,
                gstin
        );
    }

    private DocumentType mapDocType(String parserType) {
        if (parserType == null) return DocumentType.GSTR_1; // Fallback
        return switch (parserType) {
            case "GSTR1_PDF", "GSTR1_JSON" -> DocumentType.GSTR_1;
            case "GSTR3B_PDF" -> DocumentType.GSTR_3B;
            case "GSTR2A_PDF", "GSTR2A_JSON" -> DocumentType.GSTR_2A;
            case "GSTR2B_PDF" -> DocumentType.GSTR_2B;
            case "GSTR9_PDF" -> DocumentType.GSTR_9;
            case "GSTR9C_PDF" -> DocumentType.GSTR_9C;
            default -> {
                log.warn("Unknown parser type {}, falling back to GSTR_1", parserType);
                yield DocumentType.GSTR_1;
            }
        };
    }
}
