package com.learning.authservice.admin.controller;

import com.learning.authservice.admin.dto.AdminUserDetailDto;
import com.learning.authservice.admin.dto.BootstrapRequest;
import com.learning.authservice.admin.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * One-time bootstrap endpoint to link a Cognito user to the seeded
 * system admin row. Protected by an internal API key (same as /credits/grant).
 */
@RestController
@RequestMapping("/api/v1/admin/bootstrap")
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapController {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final AdminUserService adminUserService;

    @Value("${app.internal.api-key:#{null}}")
    private String internalApiKey;

    @PostMapping
    public ResponseEntity<AdminUserDetailDto> bootstrapSystemAdmin(
            @Valid @RequestBody BootstrapRequest request,
            @RequestHeader(value = INTERNAL_API_KEY_HEADER, required = false) String providedKey) {

        requireInternalApiKey(providedKey);

        AdminUserDetailDto admin = adminUserService.bootstrapSystemAdmin(
                request.cognitoSub(), request.email());
        return ResponseEntity.ok(admin);
    }

    private void requireInternalApiKey(String provided) {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            log.warn("Internal API key not configured — bootstrap endpoint is open in dev mode");
            return;
        }
        if (!internalApiKey.equals(provided)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API key");
        }
    }
}
