package com.learning.authservice.security.service;

import com.learning.authservice.security.entity.UserRole;
import com.learning.authservice.security.repository.RolePermissionRepository;
import com.learning.authservice.security.repository.UserRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private RolePermissionRepository rolePermissionRepository;
    @InjectMocks
    private AuthorizationService authorizationService;

    private static final String USER_ID = "test-user-id";

    @Nested
    @DisplayName("hasPermission")
    class HasPermissionTests {

        @Test
        @DisplayName("returns true when user holds the required permission")
        void returnsTrueWhenPermissionExists() {
            var ur = UserRole.builder().userId(USER_ID).roleId("admin").build();
            when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of(ur));
            when(rolePermissionRepository.findPermissionIdsByRoleIds(List.of("admin")))
                    .thenReturn(Set.of("credit:manage", "user:read"));

            boolean result = authorizationService.hasPermission(USER_ID, "credit", "manage");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("returns false when user lacks the required permission")
        void returnsFalseWhenPermissionMissing() {
            var ur = UserRole.builder().userId(USER_ID).roleId("viewer").build();
            when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of(ur));
            when(rolePermissionRepository.findPermissionIdsByRoleIds(List.of("viewer")))
                    .thenReturn(Set.of("user:read"));

            boolean result = authorizationService.hasPermission(USER_ID, "credit", "manage");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("returns false when user has no roles")
        void returnsFalseWhenNoRoles() {
            when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of());

            boolean result = authorizationService.hasPermission(USER_ID, "credit", "manage");

            assertThat(result).isFalse();
            verify(rolePermissionRepository, never()).findPermissionIdsByRoleIds(anyList());
        }
    }

    @Nested
    @DisplayName("getEffectivePermissions")
    class GetEffectivePermissionsTests {

        @Test
        @DisplayName("aggregates permissions across multiple roles")
        void aggregatesAcrossRoles() {
            var r1 = UserRole.builder().userId(USER_ID).roleId("admin").build();
            var r2 = UserRole.builder().userId(USER_ID).roleId("editor").build();
            when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of(r1, r2));
            when(rolePermissionRepository.findPermissionIdsByRoleIds(List.of("admin", "editor")))
                    .thenReturn(Set.of("user:read", "user:write", "credit:manage"));

            Set<String> perms = authorizationService.getEffectivePermissions(USER_ID);

            assertThat(perms).containsExactlyInAnyOrder("user:read", "user:write", "credit:manage");
        }

        @Test
        @DisplayName("returns empty set for user with no roles")
        void returnsEmptyForNoRoles() {
            when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of());

            Set<String> perms = authorizationService.getEffectivePermissions(USER_ID);

            assertThat(perms).isEmpty();
        }
    }

    @Nested
    @DisplayName("isSuperAdmin")
    class IsSuperAdminTests {

        @Test
        @DisplayName("returns true when user has super-admin role")
        void trueForSuperAdmin() {
            when(userRoleRepository.existsByUserIdAndRoleId(USER_ID, "super-admin")).thenReturn(true);
            assertThat(authorizationService.isSuperAdmin(USER_ID)).isTrue();
        }

        @Test
        @DisplayName("returns false when user lacks super-admin role")
        void falseForNonSuperAdmin() {
            when(userRoleRepository.existsByUserIdAndRoleId(USER_ID, "super-admin")).thenReturn(false);
            assertThat(authorizationService.isSuperAdmin(USER_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("getUserRoleIds")
    class GetUserRoleIdsTests {

        @Test
        @DisplayName("returns all role IDs for a user")
        void returnsAllRoleIds() {
            var r1 = UserRole.builder().userId(USER_ID).roleId("admin").build();
            var r2 = UserRole.builder().userId(USER_ID).roleId("editor").build();
            when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of(r1, r2));

            List<String> roleIds = authorizationService.getUserRoleIds(USER_ID);

            assertThat(roleIds).containsExactly("admin", "editor");
        }

        @Test
        @DisplayName("returns empty list for user with no roles")
        void returnsEmptyForNoRoles() {
            when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of());

            List<String> roleIds = authorizationService.getUserRoleIds(USER_ID);

            assertThat(roleIds).isEmpty();
        }
    }
}
