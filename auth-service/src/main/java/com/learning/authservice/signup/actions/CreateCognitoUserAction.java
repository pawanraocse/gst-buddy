package com.learning.authservice.signup.actions;

import com.learning.authservice.signup.CognitoUserRegistrar;
import com.learning.authservice.signup.pipeline.SignupAction;
import com.learning.authservice.signup.pipeline.SignupActionException;
import com.learning.authservice.signup.pipeline.SignupContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserStatusType;

/**
 * Action to create Cognito user.
 * 
 * Order: 30
 * 
 * Skipped for SSO signups (user already exists in Cognito).
 * For normal signups, creates user and triggers verification email.
 */
@Component
@Order(30)
@Slf4j
@RequiredArgsConstructor
public class CreateCognitoUserAction implements SignupAction {

    private final CognitoUserRegistrar cognitoUserRegistrar;
    private final CognitoIdentityProviderClient cognitoClient;
    private final com.learning.authservice.config.CognitoProperties cognitoProperties;

    @Override
    public String getName() {
        return "CreateCognitoUser";
    }

    @Override
    public int getOrder() {
        return 30;
    }

    @Override
    public boolean supports(SignupContext ctx) {
        // SSO users already exist in Cognito - skip this action
        return !ctx.isSsoSignup();
    }

    @Override
    public boolean isAlreadyDone(SignupContext ctx) {
        // We only consider it "done" if the user exists AND is already confirmed.
        // If they exist but are UNCONFIRMED, we need to execute to trigger a resend.
        UserStatusType status = cognitoUserRegistrar.getUserStatus(ctx.getEmail());
        if (status == null) {
            return false;
        }

        log.info("Cognito user status for {}: {}", ctx.getEmail(), status);

        if (status == UserStatusType.CONFIRMED) {
            log.info("User {} is already CONFIRMED in Cognito. Marking as already verified.", ctx.getEmail());
            ctx.setAlreadyVerified(true);
            return true;
        }

        // For UNCONFIRMED or other states, we return false to allow execute() to handle it
        return false;
    }

    @Override
    public void execute(SignupContext ctx) throws SignupActionException {
        try {
            log.info("Creating Cognito user: {}", ctx.getEmail());

            String role = "admin"; // First user is always admin
            String tenantId = ctx.getTenantId();
            if (tenantId == null) {
                throw new IllegalStateException("TenantId cannot be null. GenerateTenantIdAction must be executed before CreateCognitoUserAction.");
            }

            UserStatusType status = cognitoUserRegistrar.getUserStatus(ctx.getEmail());

            if (status == UserStatusType.UNCONFIRMED) {
                log.info("User exists but is UNCONFIRMED. Resending confirmation code for: {}", ctx.getEmail());
                cognitoUserRegistrar.resendConfirmationCode(ctx.getEmail());
                ctx.setMetadata("cognitoResult", CognitoUserRegistrar.RegistrationResult.ALREADY_EXISTS);
                return;
            }

            CognitoUserRegistrar.RegistrationResult result = cognitoUserRegistrar.registerIfNotExists(
                    ctx.getEmail(),
                    ctx.getPassword(),
                    ctx.getName(),
                    tenantId,
                    role);

            // Store result in context
            ctx.setMetadata("cognitoResult", result);
            ctx.setAssignedRole(role);

            log.info("Cognito user action result for {}: {}", ctx.getEmail(), result);

        } catch (Exception e) {
            throw new SignupActionException(getName(),
                    "Failed to create Cognito user: " + e.getMessage(), e);
        }
    }

    @Override
    public void rollback(SignupContext ctx) {
        // Cognito user deletion is risky - log for manual cleanup
        log.warn("Rollback requested for Cognito user {}: manual cleanup may be needed",
                ctx.getEmail());
    }
}
