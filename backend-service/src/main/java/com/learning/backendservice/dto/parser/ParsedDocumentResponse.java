package com.learning.backendservice.dto.parser;

import java.util.List;
import java.util.Map;

public record ParsedDocumentResponse(
    String status,
    String doc_type,
    String classifier_confidence,
    Map<String, Object> extracted_data,
    long extraction_time_ms,
    String parser_version,
    List<String> warnings
) {}
