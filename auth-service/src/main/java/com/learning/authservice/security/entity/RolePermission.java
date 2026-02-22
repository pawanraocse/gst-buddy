package com.learning.authservice.security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * Maps a role to a permission within a tenant.
 */
@Entity
@Table(name = "role_permissions")
@IdClass(RolePermission.RolePermissionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission {

    @Id
    @Column(name = "tenant_id", nullable = false, length = 64)
    @Builder.Default
    private String tenantId = "default";

    @Id
    @Column(name = "role_id", nullable = false, length = 64)
    private String roleId;

    @Id
    @Column(name = "permission_id", nullable = false, length = 128)
    private String permissionId;

    @Column(name = "granted_at", nullable = false)
    @Builder.Default
    private Instant grantedAt = Instant.now();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RolePermissionId implements Serializable {
        private String tenantId;
        private String roleId;
        private String permissionId;
    }
}
