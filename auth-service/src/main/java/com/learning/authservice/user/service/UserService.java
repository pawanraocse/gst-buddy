package com.learning.authservice.user.service;

import com.learning.authservice.user.domain.User;
import com.learning.authservice.user.dto.UserDto;
import com.learning.authservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing users.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Get all users.
     */
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Get all active users.
     */
    @Transactional(readOnly = true)
    public List<UserDto> getActiveUsers() {
        return userRepository.findAllActiveUsers().stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Search users by name or email.
     */
    @Transactional(readOnly = true)
    public List<UserDto> searchUsers(@NonNull String query) {
        return userRepository.searchUsers(query).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Get a user by ID.
     */
    @Transactional(readOnly = true)
    public Optional<UserDto> getUserById(@NonNull String userId) {
        return userRepository.findById(userId).map(this::toDto);
    }

    /**
     * Register or update user on login.
     * Called when user logs in via Cognito or SSO.
     */
    @Transactional
    public UserDto upsertOnLogin(@NonNull String userId, @NonNull String email,
            String name, String source) {
        log.info("Upserting user on login: userId={}, email={}", userId, email);

        Optional<User> existing = userRepository.findById(userId);
        User user;

        if (existing.isPresent()) {
            user = existing.get();
            user.setLastLoginAt(Instant.now());
            user.setStatus("ACTIVE");
            if (name != null && !name.isBlank()) {
                user.setName(name);
            }
        } else {
            // Create new user
            user = User.builder()
                    .userId(userId)
                    .email(email)
                    .name(name)
                    .source(source != null ? source : "COGNITO")
                    .status("ACTIVE")
                    .firstLoginAt(Instant.now())
                    .lastLoginAt(Instant.now())
                    .build();
        }

        User saved = userRepository.save(user);
        return toDto(saved);
    }

    /**
     * Create user from invitation (status=INVITED until they login).
     */
    @Transactional
    public UserDto createFromInvitation(@NonNull String email, String name) {
        log.info("Creating user from invitation: email={}", email);

        // Check if user already exists
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            return toDto(existing.get());
        }

        User user = User.builder()
                .userId("pending-" + email.hashCode()) // Temporary ID until login
                .email(email)
                .name(name)
                .source("INVITATION")
                .status("INVITED")
                .build();

        User saved = userRepository.save(user);
        return toDto(saved);
    }

    /**
     * Disable a user.
     */
    @Transactional
    public void disableUser(@NonNull String userId) {
        log.info("Disabling user: {}", userId);
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus("DISABLED");
            userRepository.save(user);
        });
    }

    /**
     * Re-enable a disabled user.
     */
    @Transactional
    public void enableUser(@NonNull String userId) {
        log.info("Enabling user: {}", userId);
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus("ACTIVE");
            userRepository.save(user);
        });
    }

    /**
     * Get user count by status.
     */
    @Transactional(readOnly = true)
    public long countByStatus(@NonNull String status) {
        return userRepository.countByStatus(status);
    }

    /**
     * Get user count grouped by status.
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> getUserStatsMap() {
        List<Object[]> results = userRepository.countUsersByStatusGrouped();
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        long total = 0;

        for (Object[] row : results) {
            String status = (String) row[0];
            Long count = (Long) row[1];
            stats.put(status.toLowerCase(), count);
            total += count;
        }
        stats.put("total", total);
        return stats;
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .source(user.getSource())
                .firstLoginAt(user.getFirstLoginAt())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
