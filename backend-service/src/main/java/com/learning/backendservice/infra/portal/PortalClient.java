package com.learning.backendservice.infra.portal;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stubbed client for the GST Portal API.
 */
@Component
public class PortalClient {

    private final Map<String, GstinStatus> stubbedStatusMap = new ConcurrentHashMap<>();

    public PortalClient() {
        // Test data should be configured in tests via setStubStatus
    }

    public GstinStatus getGstinStatus(String gstin) {
        // Return a dummy status if not found in the stub map
        return stubbedStatusMap.getOrDefault(gstin, new GstinStatus("ACTIVE", null));
    }

    public void setStubStatus(String gstin, String status, LocalDate cancellationDate) {
        stubbedStatusMap.put(gstin, new GstinStatus(status, cancellationDate));
    }

    public record GstinStatus(String status, LocalDate cancellationDate) {}
}
