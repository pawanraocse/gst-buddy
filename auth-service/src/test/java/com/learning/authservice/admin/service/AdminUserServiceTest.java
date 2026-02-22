package com.learning.authservice.admin.service;

import com.learning.authservice.admin.dto.AdminUserDetailDto;
import com.learning.authservice.credit.entity.UserCreditWallet;
import com.learning.authservice.credit.repository.UserCreditWalletRepository;
import com.learning.authservice.security.entity.UserRole;
import com.learning.authservice.security.repository.UserRoleRepository;
import com.learning.authservice.user.domain.User;
import com.learning.authservice.user.repository.UserRepository;
import com.learning.common.infra.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private UserCreditWalletRepository walletRepository;
    @InjectMocks private AdminUserService adminUserService;

    private static final String USER_ID = "u-123";

    @Nested
    @DisplayName("getUserDetail")
    class GetUserDetailTests {

        @Test
        @DisplayName("returns user detail with roles and wallet")
        void returnsDetailWithRolesAndWallet() {
            User user = buildUser(USER_ID, "alice@test.com", "ACTIVE");
            var role = UserRole.builder().userId(USER_ID).roleId("admin").build();
            var wallet = UserCreditWallet.builder()
                    .userId(USER_ID).tenantId("default")
                    .totalCredits(100).consumedCredits(30).build();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of(role));
            when(walletRepository.findByUserIdAndTenantId(USER_ID, "default"))
                    .thenReturn(Optional.of(wallet));

            AdminUserDetailDto dto = adminUserService.getUserDetail(USER_ID);

            assertThat(dto.userId()).isEqualTo(USER_ID);
            assertThat(dto.email()).isEqualTo("alice@test.com");
            assertThat(dto.roles()).containsExactly("admin");
            assertThat(dto.wallet().total()).isEqualTo(100);
            assertThat(dto.wallet().used()).isEqualTo(30);
            assertThat(dto.wallet().remaining()).isEqualTo(70);
        }

        @Test
        @DisplayName("returns zero-wallet when no wallet exists")
        void returnsZeroWalletWhenMissing() {
            User user = buildUser(USER_ID, "bob@test.com", "ACTIVE");
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(walletRepository.findByUserIdAndTenantId(USER_ID, "default"))
                    .thenReturn(Optional.empty());

            AdminUserDetailDto dto = adminUserService.getUserDetail(USER_ID);

            assertThat(dto.wallet().total()).isEqualTo(0);
            assertThat(dto.wallet().remaining()).isEqualTo(0);
        }

        @Test
        @DisplayName("throws NotFoundException for unknown user")
        void throwsForUnknownUser() {
            when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminUserService.getUserDetail("nonexistent"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("nonexistent");
        }
    }

    @Nested
    @DisplayName("enableUser")
    class EnableUserTests {

        @Test
        @DisplayName("sets user status to ACTIVE")
        void setsStatusToActive() {
            User user = buildUser(USER_ID, "alice@test.com", "DISABLED");
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            adminUserService.enableUser(USER_ID);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("throws NotFoundException for unknown user")
        void throwsForUnknownUser() {
            when(userRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminUserService.enableUser("missing"))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("suspendUser")
    class SuspendUserTests {

        @Test
        @DisplayName("sets user status to DISABLED")
        void setsStatusToDisabled() {
            User user = buildUser(USER_ID, "alice@test.com", "ACTIVE");
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            adminUserService.suspendUser(USER_ID);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("DISABLED");
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUserTests {

        @Test
        @DisplayName("deletes user and their roles")
        void deletesUserAndRoles() {
            User user = buildUser(USER_ID, "alice@test.com", "ACTIVE");
            var role = UserRole.builder().userId(USER_ID).roleId("admin").build();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of(role));

            adminUserService.deleteUser(USER_ID);

            verify(userRoleRepository).delete(role);
            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("throws NotFoundException for unknown user")
        void throwsForUnknownUser() {
            when(userRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminUserService.deleteUser("missing"))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("assignRole")
    class AssignRoleTests {

        @Test
        @DisplayName("assigns role when not already assigned")
        void assignsNewRole() {
            User user = buildUser(USER_ID, "alice@test.com", "ACTIVE");
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRoleRepository.existsByUserIdAndRoleId(USER_ID, "editor")).thenReturn(false);

            adminUserService.assignRole(USER_ID, "editor", "admin-1");

            ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);
            verify(userRoleRepository).save(captor.capture());
            assertThat(captor.getValue().getRoleId()).isEqualTo("editor");
            assertThat(captor.getValue().getAssignedBy()).isEqualTo("admin-1");
        }

        @Test
        @DisplayName("skips if role already assigned (idempotent)")
        void skipsIfAlreadyAssigned() {
            User user = buildUser(USER_ID, "alice@test.com", "ACTIVE");
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRoleRepository.existsByUserIdAndRoleId(USER_ID, "admin")).thenReturn(true);

            adminUserService.assignRole(USER_ID, "admin", "admin-1");

            verify(userRoleRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("removeRole")
    class RemoveRoleTests {

        @Test
        @DisplayName("removes role from user")
        void removesRole() {
            User user = buildUser(USER_ID, "alice@test.com", "ACTIVE");
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            adminUserService.removeRole(USER_ID, "admin");

            verify(userRoleRepository).deleteByUserIdAndRoleId(USER_ID, "admin");
        }
    }

    @Nested
    @DisplayName("bootstrapSystemAdmin")
    class BootstrapTests {

        @Test
        @DisplayName("returns existing detail when cognito sub already matches")
        void returnsExistingWhenAlreadyLinked() {
            User existing = buildUser("cognito-sub-1", "admin@gst-buddy.local", "ACTIVE");
            when(userRepository.findByEmail("admin@gst-buddy.local"))
                    .thenReturn(Optional.of(existing));
            when(userRoleRepository.findByUserId("cognito-sub-1")).thenReturn(List.of());
            when(walletRepository.findByUserIdAndTenantId("cognito-sub-1", "default"))
                    .thenReturn(Optional.empty());

            AdminUserDetailDto dto = adminUserService.bootstrapSystemAdmin(
                    "cognito-sub-1", "admin@gst-buddy.local");

            assertThat(dto.userId()).isEqualTo("cognito-sub-1");
            verify(userRepository, never()).delete(any());
        }

        @Test
        @DisplayName("transfers placeholder to real cognito sub")
        void transfersPlaceholderToRealSub() {
            User placeholder = buildUser("SYSTEM_ADMIN_PLACEHOLDER", "admin@gst-buddy.local", "ACTIVE");
            var oldRole = UserRole.builder().userId("SYSTEM_ADMIN_PLACEHOLDER")
                    .tenantId("default").roleId("super-admin").build();

            when(userRepository.findByEmail("admin@gst-buddy.local"))
                    .thenReturn(Optional.of(placeholder));
            when(userRoleRepository.findByUserId("SYSTEM_ADMIN_PLACEHOLDER"))
                    .thenReturn(List.of(oldRole));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRoleRepository.findByUserId("real-cognito-sub")).thenReturn(List.of(
                    UserRole.builder().userId("real-cognito-sub").roleId("super-admin").build()
            ));
            when(walletRepository.findByUserIdAndTenantId("real-cognito-sub", "default"))
                    .thenReturn(Optional.empty());

            AdminUserDetailDto dto = adminUserService.bootstrapSystemAdmin(
                    "real-cognito-sub", "admin@gst-buddy.local");

            verify(userRepository).delete(placeholder);
            verify(userRoleRepository).deleteAll(List.of(oldRole));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getUserId()).isEqualTo("real-cognito-sub");
            assertThat(userCaptor.getValue().getSource()).isEqualTo("COGNITO");

            ArgumentCaptor<UserRole> roleCaptor = ArgumentCaptor.forClass(UserRole.class);
            verify(userRoleRepository).save(roleCaptor.capture());
            assertThat(roleCaptor.getValue().getUserId()).isEqualTo("real-cognito-sub");
            assertThat(roleCaptor.getValue().getRoleId()).isEqualTo("super-admin");
        }

        @Test
        @DisplayName("throws NotFoundException when seeded email not found")
        void throwsWhenEmailNotFound() {
            when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminUserService.bootstrapSystemAdmin(
                    "sub-1", "missing@test.com"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("missing@test.com");
        }
    }

    private User buildUser(String userId, String email, String status) {
        return User.builder()
                .userId(userId)
                .tenantId("default")
                .email(email)
                .name("Test User")
                .status(status)
                .source("COGNITO")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
