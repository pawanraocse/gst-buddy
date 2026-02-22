package com.learning.authservice.admin.service;

import com.learning.authservice.admin.dto.AdminUserDetailDto;
import com.learning.authservice.admin.dto.AdminUserDetailDto.WalletSummaryDto;
import com.learning.authservice.credit.entity.UserCreditWallet;
import com.learning.authservice.credit.repository.UserCreditWalletRepository;
import com.learning.authservice.security.entity.UserRole;
import com.learning.authservice.security.repository.UserRoleRepository;
import com.learning.authservice.user.domain.User;
import com.learning.authservice.user.repository.UserRepository;
import com.learning.common.infra.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Admin-level user management operations (cross-tenant, super-admin only).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserCreditWalletRepository walletRepository;

    @Transactional(readOnly = true)
    public Page<AdminUserDetailDto> listAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toDetailDto);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailDto getUserDetail(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        return toDetailDto(user);
    }

    @Transactional
    public void enableUser(String userId) {
        User user = requireUser(userId);
        user.setStatus("ACTIVE");
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        log.info("Admin enabled user: {}", userId);
    }

    @Transactional
    public void suspendUser(String userId) {
        User user = requireUser(userId);
        user.setStatus("DISABLED");
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        log.info("Admin suspended user: {}", userId);
    }

    @Transactional
    public void deleteUser(String userId) {
        User user = requireUser(userId);
        userRoleRepository.findByUserId(userId)
                .forEach(ur -> userRoleRepository.delete(ur));
        userRepository.delete(user);
        log.info("Admin deleted user: {}", userId);
    }

    @Transactional
    public void assignRole(String userId, String roleId, String assignedBy) {
        requireUser(userId);
        if (userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            log.info("User {} already has role {}", userId, roleId);
            return;
        }
        UserRole ur = UserRole.builder()
                .userId(userId)
                .roleId(roleId)
                .assignedBy(assignedBy)
                .build();
        userRoleRepository.save(ur);
        log.info("Admin assigned role {} to user {}", roleId, userId);
    }

    @Transactional
    public void removeRole(String userId, String roleId) {
        requireUser(userId);
        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
        log.info("Admin removed role {} from user {}", roleId, userId);
    }

    /**
     * Bootstrap: link a Cognito sub to the seeded system admin placeholder.
     */
    @Transactional
    public AdminUserDetailDto bootstrapSystemAdmin(String cognitoSub, String email) {
        User placeholder = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(
                        "No seeded user found with email: " + email));

        if (placeholder.getUserId().equals(cognitoSub)) {
            return toDetailDto(placeholder);
        }

        // Transfer roles from placeholder to real Cognito sub
        List<UserRole> roles = userRoleRepository.findByUserId(placeholder.getUserId());

        userRoleRepository.deleteAll(roles);
        userRepository.delete(placeholder);

        User admin = User.builder()
                .userId(cognitoSub)
                .tenantId(placeholder.getTenantId())
                .email(email)
                .name(placeholder.getName())
                .status("ACTIVE")
                .source("COGNITO")
                .firstLoginAt(Instant.now())
                .lastLoginAt(Instant.now())
                .build();
        userRepository.save(admin);

        for (UserRole role : roles) {
            UserRole newRole = UserRole.builder()
                    .userId(cognitoSub)
                    .tenantId(role.getTenantId())
                    .roleId(role.getRoleId())
                    .assignedBy("SYSTEM_BOOTSTRAP")
                    .build();
            userRoleRepository.save(newRole);
        }

        log.info("System admin bootstrapped: email={}, cognitoSub={}", email, cognitoSub);
        return toDetailDto(admin);
    }

    private User requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private AdminUserDetailDto toDetailDto(User user) {
        List<String> roleIds = userRoleRepository.findByUserId(user.getUserId())
                .stream()
                .map(UserRole::getRoleId)
                .toList();

        WalletSummaryDto walletDto = walletRepository
                .findByUserIdAndTenantId(user.getUserId(), user.getTenantId())
                .map(w -> WalletSummaryDto.builder()
                        .total(w.getTotalCredits())
                        .used(w.getConsumedCredits())
                        .remaining(w.getRemainingCredits())
                        .build())
                .orElse(WalletSummaryDto.builder().total(0).used(0).remaining(0).build());

        return AdminUserDetailDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .source(user.getSource())
                .tenantId(user.getTenantId())
                .roles(roleIds)
                .wallet(walletDto)
                .firstLoginAt(user.getFirstLoginAt())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
