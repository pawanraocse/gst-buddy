package com.learning.backendservice.infra.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.backendservice.dto.parser.ParsedDocumentResponse;
import com.learning.backendservice.exception.LedgerParseException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParserClientTest {

    private MockWebServer mockWebServer;
    private ParserClient parserClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        objectMapper = new ObjectMapper();
        
        // Initialize client to point to the mock server instead of localhost:8090
        parserClient = new ParserClient(mockWebServer.url("/").toString(), true, objectMapper);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testExtract_Success() throws Exception {
        // Arrange
        String mockJsonResponse = """
                {
                    "status": "SUCCESS",
                    "doc_type": "GSTR1_PDF",
                    "classifier_confidence": "HIGH",
                    "extraction_time_ms": 45,
                    "parser_version": "1.0.0",
                    "warnings": [],
                    "extracted_data": {
                        "gstin": "07ASXPD9282E1Z8",
                        "arn_date": "2024-05-11"
                    }
                }
                """;
        
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockJsonResponse)
                .addHeader("Content-Type", "application/json"));

        MultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy content".getBytes());

        // Act
        ParsedDocumentResponse response = parserClient.extract(file, "GSTR1_PDF");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.doc_type()).isEqualTo("GSTR1_PDF");
        assertThat(response.extracted_data()).containsEntry("arn_date", "2024-05-11");
        assertThat(response.extracted_data()).containsEntry("gstin", "07ASXPD9282E1Z8");
    }

    @Test
    void testExtract_Error_LedgerParseExceptionThrown() {
        // Arrange
        String mockErrorResponse = """
                {
                    "status": "FAILED",
                    "error_code": "UNKNOWN_FORMAT",
                    "error_message": "Could not classify document.",
                    "suggestions": []
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(422)
                .setBody(mockErrorResponse)
                .addHeader("Content-Type", "application/json"));

        MultipartFile file = new MockMultipartFile("file", "invalid.bin", "application/octet-stream", "dummy".getBytes());

        // Act & Assert
        assertThatThrownBy(() -> parserClient.extract(file, null))
                .isInstanceOf(LedgerParseException.class)
                .hasMessageContaining("UNKNOWN_FORMAT")
                .hasMessageContaining("Could not classify document");
    }

    @Test
    void testExtract_SkippedWhenDisabled() {
        // Arrange
        ParserClient disabledClient = new ParserClient(mockWebServer.url("/").toString(), false, objectMapper);
        MultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy".getBytes());

        // Act
        ParsedDocumentResponse response = disabledClient.extract(file, null);

        // Assert
        assertThat(response).isNull(); // Returns early when enabled=false
    }
}
