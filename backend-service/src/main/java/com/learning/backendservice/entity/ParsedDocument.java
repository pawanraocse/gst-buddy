package com.learning.backendservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "parsed_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedDocument {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    @Column(name = "doc_type", nullable = false, length = 30)
    private String docType;

    @Column(name = "s3_raw_key", nullable = false, length = 500)
    private String s3RawKey;

    @Column(name = "template_id", length = 100)
    private String templateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parsed_json", columnDefinition = "jsonb")
    private String parsedJson;

    @Column(name = "parser_version", nullable = false, length = 20)
    @Builder.Default
    private String parserVersion = "1.0";

    @Column(name = "parse_status", nullable = false, length = 20)
    @Builder.Default
    private String parseStatus = "PENDING";

    @Column(name = "parse_duration_ms")
    private Integer parseDurationMs;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
