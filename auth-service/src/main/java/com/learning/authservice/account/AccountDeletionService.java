package com.learning.authservice.account;

import com.learning.authservice.security.repository.UserRoleRepository;
import com.learning.authservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling account deletion.
 * Deletes user from the local database. Cognito deletion is handled
 * separately via admin CLI or a future async processor.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    /**
     * Delete user's account from local database.
     *
     * @param userId    User ID (Cognito sub or placeholder)
     * @param userEmail User's email (used for lookup if userId doesn't match)
     */
    @Transactional
    public void deleteAccount(String userId, String userEmail) {
        log.info("Deleting account: userId={}, userEmail={}", userId, userEmail);

        var user = userRepository.findById(userId)
                .or(() -> userRepository.findByEmail(userEmail));

        if (user.isEmpty()) {
            log.warn("User not found for deletion: userId={}, email={}", userId, userEmail);
            return;
        }

        String actualUserId = user.get().getUserId();

        userRoleRepository.findByUserId(actualUserId)
                .forEach(userRoleRepository::delete);

        userRepository.delete(user.get());
        log.info("Account deleted from database: userId={}, email={}", actualUserId, userEmail);
    }

    /**
     * Legacy overload for backward compatibility.
     */
    public void deleteAccount(String tenantId, String userEmail, String idpType) {
        deleteAccount(tenantId, userEmail);
    }
}
