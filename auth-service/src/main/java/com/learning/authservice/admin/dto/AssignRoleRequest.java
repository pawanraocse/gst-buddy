package com.learning.authservice.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for assigning or removing a role.
 */
public record AssignRoleRequest(
        @NotBlank String roleId) {
}
