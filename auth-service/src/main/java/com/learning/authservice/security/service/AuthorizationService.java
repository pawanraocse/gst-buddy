package com.learning.authservice.security.service;

import com.learning.authservice.security.repository.RolePermissionRepository;
import com.learning.authservice.security.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Resolves a user's effective permissions by traversing
 * user_roles → role_permissions → permissions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    /**
     * Check whether a user holds a specific resource:action permission
     * through any of their assigned roles.
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(String userId, String resource, String action) {
        String permissionId = resource + ":" + action;
        Set<String> permissions = getEffectivePermissions(userId);
        return permissions.contains(permissionId);
    }

    /**
     * Returns the full set of permission IDs (resource:action) the user holds.
     */
    @Transactional(readOnly = true)
    public Set<String> getEffectivePermissions(String userId) {
        List<String> roleIds = userRoleRepository.findByUserId(userId)
                .stream()
                .map(ur -> ur.getRoleId())
                .toList();

        if (roleIds.isEmpty()) {
            return Set.of();
        }

        return rolePermissionRepository.findPermissionIdsByRoleIds(roleIds);
    }

    /**
     * Checks if a user holds the super-admin role.
     */
    @Transactional(readOnly = true)
    public boolean isSuperAdmin(String userId) {
        return userRoleRepository.existsByUserIdAndRoleId(userId, "super-admin");
    }

    /**
     * Returns the role IDs assigned to a user.
     */
    @Transactional(readOnly = true)
    public List<String> getUserRoleIds(String userId) {
        return userRoleRepository.findByUserId(userId)
                .stream()
                .map(ur -> ur.getRoleId())
                .toList();
    }
}
