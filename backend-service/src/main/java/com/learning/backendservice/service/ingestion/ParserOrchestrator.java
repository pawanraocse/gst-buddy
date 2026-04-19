package com.learning.backendservice.service.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.backendservice.dto.parser.ParsedDocumentResponse;
import com.learning.backendservice.entity.ParsedDocument;
import com.learning.backendservice.infra.parser.ParserClient;
import com.learning.backendservice.repository.ParsedDocumentRepository;
import com.learning.backendservice.util.UuidV7;
import com.learning.common.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;

@Service
public class ParserOrchestrator {
    
    private static final Logger log = LoggerFactory.getLogger(ParserOrchestrator.class);

    private final ParserClient parserClient;
    private final ParsedDocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    // TODO: inject S3 StorageClient when available for WORM workflow
    // private final StorageClient storageClient;

    public ParserOrchestrator(ParserClient parserClient, 
                              ParsedDocumentRepository documentRepository,
                              ObjectMapper objectMapper) {
        this.parserClient = parserClient;
        this.documentRepository = documentRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Complete ingestion flow: 
     * 1. Save RAW file to WORM storage (S3).
     * 2. Call python parser sidecar.
     * 3. Sync result to PostgreSQL (ParsedDocument).
     */
    @Transactional
    public ParsedDocument ingestDocument(MultipartFile file, String docTypeHint) {
        String tenantId = TenantContext.getCurrentTenant();
        
        // 1. Upload to S3
        String s3Key = "tenant-" + tenantId + "/raw/" + file.getOriginalFilename();
        String bucketName = "gstbuddies-raw-documents"; 
        // storageClient.upload(bucketName, s3Key, file.getInputStream());
        log.info("Simulated S3 Upload: {} to bucket: {}", s3Key, bucketName);

        // 2. Call Python Sidecar
        ParsedDocument parsedDocEntity = ParsedDocument.builder()
                .id(UuidV7.generate())
                .tenantId(tenantId)
                .s3RawKey(s3Key)
                .originalFilename(file.getOriginalFilename())
                .parseStatus("PENDING")
                .docType(docTypeHint != null ? docTypeHint : "UNKNOWN")
                .createdAt(OffsetDateTime.now())
                .build();
        
        // Save initial pending state
        parsedDocEntity = documentRepository.save(parsedDocEntity);

        try {
            ParsedDocumentResponse response = parserClient.extract(file, docTypeHint);

            if (response != null && "SUCCESS".equals(response.status())) {
                parsedDocEntity.setParseStatus("SUCCESS");
                parsedDocEntity.setDocType(response.doc_type());
                parsedDocEntity.setParserVersion(response.parser_version());
                parsedDocEntity.setParseDurationMs((int) response.extraction_time_ms());
                parsedDocEntity.setParsedJson(objectMapper.writeValueAsString(response.extracted_data()));
            } else {
                parsedDocEntity.setParseStatus("FAILED");
                parsedDocEntity.setErrorMessage("Parser returned non-success status or null response.");
            }
        } catch (Exception e) {
            log.error("Parser extraction failed for file {}", file.getOriginalFilename(), e);
            parsedDocEntity.setParseStatus("FAILED");
            parsedDocEntity.setErrorMessage("Exception: " + e.getMessage());
        }

        return documentRepository.save(parsedDocEntity);
    }
}
