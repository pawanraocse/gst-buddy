package com.learning.backendservice.infra.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.backendservice.dto.parser.ParsedDocumentResponse;
import com.learning.backendservice.dto.parser.ParserError;
import com.learning.backendservice.exception.LedgerParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class ParserClient {
    private static final Logger log = LoggerFactory.getLogger(ParserClient.class);
    
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public ParserClient(
            @Value("${app.parser.url:http://localhost:8090}") String baseUrl,
            @Value("${app.parser.enabled:true}") boolean enabled,
            ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public ParsedDocumentResponse extract(MultipartFile file, String docTypeHint) {
        if (!enabled) {
            log.warn("Parser service is disabled. Skipping extraction for {}", file.getOriginalFilename());
            return null;
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());
        if (docTypeHint != null) {
            body.add("doc_type_hint", docTypeHint);
        }

        log.info("Sending document {} to parser service...", file.getOriginalFilename());

        return restClient.post()
                .uri("/api/v1/extract")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .exchange((request, response) -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        return objectMapper.readValue(response.getBody(), ParsedDocumentResponse.class);
                    } else {
                        handleErrorResponse(response);
                        return null; // unreachable due to exception in handleErrorResponse
                    }
                });
    }

    private void handleErrorResponse(ClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();
        String responseBody = new String(response.getBody().readAllBytes());

        try {
            ParserError error = objectMapper.readValue(responseBody, ParserError.class);
            log.error("Parser service returned error {}: {}", status, error.error_message());
            throw new LedgerParseException("Parser Error (" + error.error_code() + "): " + error.error_message());
        } catch (JsonProcessingException e) {
            // Body is not parseable JSON — log raw response and throw generic error
            log.error("Parser service returned unparseable error {}: {}", status, responseBody);
            throw new LedgerParseException("Document extraction failed with status " + status);
        }
    }
}
