package com.learning.authservice.security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Maps a user to a role within a tenant.
 */
@Entity
@Table(name = "user_roles", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "user_id", "role_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    @Builder.Default
    private String tenantId = "default";

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "role_id", nullable = false, length = 64)
    private String roleId;

    @Column(name = "assigned_by", length = 255)
    private String assignedBy;

    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private Instant assignedAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;
}
