package com.learning.authservice.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Links a Cognito user sub to the seeded system-admin DB row.
 */
public record BootstrapRequest(
        @NotBlank String cognitoSub,
        @NotBlank @Email String email) {
}
