package com.learning.authservice.auth.service;

import com.learning.authservice.config.CognitoProperties;
import com.learning.authservice.auth.dto.AuthRequestDto;
import com.learning.authservice.auth.dto.AuthResponseDto;
import com.learning.authservice.dto.SignupRequestDto;
import com.learning.authservice.dto.SignupResponseDto;
import com.learning.authservice.dto.UserInfoDto;
import com.learning.authservice.exception.AuthLoginException;
import com.learning.authservice.exception.AuthSignupException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidPasswordException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

        private final CognitoProperties cognitoProperties;
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final CognitoIdentityProviderClient cognitoClient;

        @Override
        @Transactional(readOnly = true)
        public UserInfoDto getCurrentUser() {
                String userId = request.getHeader("X-User-Id");
                if (userId == null || userId.isBlank()) {
                        log.info("operation=getCurrentUser, userId=anonymous, requestId={}, status=unauthenticated",
                                        request.getAttribute("X-Request-Id"));
                        throw new RuntimeException("User not authenticated");
                }

                String email = request.getHeader("X-Email");
                String name = request.getHeader("X-Username");

                // Default to viewer for now as RBAC is removed
                String role = "viewer";

                log.info("operation=getCurrentUser, userId={}, role={}, requestId={}, status=success", userId, role,
                                request.getAttribute("X-Request-Id"));
                return UserInfoDto.builder()
                                .userId(userId)
                                .email(email)
                                .name(name)
                                .role(role)
                                .build();
        }

        @Override
        public AuthResponseDto login(AuthRequestDto requestDto) {
                try {
                        // Build auth parameters with SECRET_HASH if client secret is configured
                        Map<String, String> authParams = new HashMap<>();
                        authParams.put("USERNAME", requestDto.getEmail());
                        authParams.put("PASSWORD", requestDto.getPassword());

                        // Add SECRET_HASH if client secret is configured
                        String clientSecret = cognitoProperties.getClientSecret();
                        if (clientSecret != null && !clientSecret.isEmpty()) {
                                String secretHash = computeSecretHash(
                                                requestDto.getEmail(),
                                                cognitoProperties.getClientId(),
                                                clientSecret);
                                authParams.put("SECRET_HASH", secretHash);
                        }

                        AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                                        .userPoolId(cognitoProperties.getUserPoolId())
                                        .clientId(cognitoProperties.getClientId())
                                        .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                                        .authParameters(authParams)
                                        .build();
                        AdminInitiateAuthResponse cognitoResponse = cognitoClient.adminInitiateAuth(authRequest);
                        var result = cognitoResponse.authenticationResult();
                        log.info("operation=login status=success userId={} requestId={}", requestDto.getEmail(),
                                        request.getAttribute("X-Request-Id"));
                        return new AuthResponseDto(
                                        result.accessToken(),
                                        null,
                                        result.refreshToken(),
                                        result.tokenType(),
                                        result.expiresIn() != null ? result.expiresIn().longValue() : null,
                                        requestDto.getEmail(),
                                        requestDto.getEmail());
                } catch (NotAuthorizedException e) {
                        log.warn("operation=login status=failed code=INVALID_CREDENTIALS userId={} requestId={} error={}",
                                        requestDto.getEmail(), request.getAttribute("X-Request-Id"), e.getMessage());
                        throw new AuthLoginException("INVALID_CREDENTIALS", "Invalid username or password", e);
                } catch (UserNotFoundException e) {
                        log.warn("operation=login status=failed code=USER_NOT_FOUND userId={} requestId={} error={}",
                                        requestDto.getEmail(), request.getAttribute("X-Request-Id"), e.getMessage());
                        throw new AuthLoginException("USER_NOT_FOUND", "User not found", e);
                } catch (CognitoIdentityProviderException e) {
                        log.warn("operation=login status=failed code=LOGIN_FAILED userId={} requestId={} error={} awsCode={}",
                                        requestDto.getEmail(), request.getAttribute("X-Request-Id"), e.getMessage(),
                                        e.awsErrorDetails().errorCode());
                        throw new AuthLoginException("LOGIN_FAILED",
                                        "Login failed: " + e.awsErrorDetails().errorMessage(), e);
                }
        }

        /**
         * Compute SECRET_HASH for Cognito authentication.
         * Required when the app client is configured with a client secret.
         */
        private String computeSecretHash(String username, String clientId, String clientSecret) {
                try {
                        String message = username + clientId;
                        Mac mac = Mac.getInstance("HmacSHA256");
                        SecretKeySpec secretKeySpec = new SecretKeySpec(
                                        clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
                        mac.init(secretKeySpec);
                        byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
                        return Base64.getEncoder().encodeToString(rawHmac);
                } catch (Exception e) {
                        log.error("Failed to compute SECRET_HASH: {}", e.getMessage());
                        throw new RuntimeException("Failed to compute SECRET_HASH", e);
                }
        }

        @Override
        public SignupResponseDto signup(SignupRequestDto requestDto) {
                try {
                        // Use signUp API instead of adminCreateUser to enable email verification
                        SignUpRequest signUpRequest = SignUpRequest.builder()
                                        .clientId(cognitoProperties.getClientId())
                                        .username(requestDto.email())
                                        .password(requestDto.password())
                                        .userAttributes(
                                                        AttributeType.builder()
                                                                        .name("email")
                                                                        .value(requestDto.email())
                                                                        .build(),
                                                        AttributeType.builder()
                                                                        .name("name")
                                                                        .value(requestDto.name())
                                                                        .build())
                                        .clientMetadata(java.util.Map.of(
                                                        "tenantId",
                                                        requestDto.email().replace("@", "-").replace(".", "-"), // Simple
                                                                                                                // tenant
                                                                                                                // ID
                                                                                                                // generation
                                                                                                                // for
                                                                                                                // personal
                                                                                                                // users
                                                        "role", "admin"))
                                        .build();

                        SignUpResponse signUpResponse = cognitoClient.signUp(signUpRequest);

                        log.info("operation=signup status=success userId={} userConfirmed={} requestId={}",
                                        requestDto.email(),
                                        signUpResponse.userConfirmed(),
                                        request.getAttribute("X-Request-Id"));

                        // Return signup response with verification status
                        SignupResponseDto response = new SignupResponseDto();
                        response.setEmail(requestDto.email());
                        response.setUserConfirmed(signUpResponse.userConfirmed());
                        response.setUserSub(signUpResponse.userSub());
                        response.setMessage(signUpResponse.userConfirmed()
                                        ? "Signup successful. You can now login."
                                        : "Signup successful. Please check your email to verify your account.");

                        return response;

                } catch (UsernameExistsException e) {
                        log.warn("operation=signup status=failed code=USER_EXISTS userId={} requestId={} error={}",
                                        requestDto.email(), request.getAttribute("X-Request-Id"), e.getMessage());
                        throw new AuthSignupException("USER_EXISTS", "User already exists", e);
                } catch (InvalidPasswordException e) {
                        log.warn("operation=signup status=failed code=INVALID_PASSWORD userId={} requestId={} error={}",
                                        requestDto.email(), request.getAttribute("X-Request-Id"), e.getMessage());
                        throw new AuthSignupException("INVALID_PASSWORD",
                                        "Password does not meet requirements", e);
                } catch (CognitoIdentityProviderException e) {
                        log.warn("operation=signup status=failed code=SIGNUP_FAILED userId={} requestId={} error={} awsCode={}",
                                        requestDto.email(), request.getAttribute("X-Request-Id"), e.getMessage(),
                                        e.awsErrorDetails().errorCode());
                        throw new AuthSignupException("SIGNUP_FAILED",
                                        "Signup failed: " + e.awsErrorDetails().errorMessage(), e);
                }
        }

        @Override
        public void logout() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null) {
                        new SecurityContextLogoutHandler().logout(request, response, auth);
                        log.info("operation=logout, userId={}, requestId={}, status=success", auth.getName(),
                                        request.getAttribute("X-Request-Id"));
                } else {
                        log.info("operation=logout, userId=anonymous, requestId={}, status=not_authenticated",
                                        request.getAttribute("X-Request-Id"));
                }
        }

        @Override
        public void deleteAccount() {
                String userId = request.getHeader("X-User-Id");
                // In Lite version, we don't track tenants explicitly in the same way, but we
                // might receive headers.

                if (userId == null) {
                        throw new RuntimeException("Missing authentication headers");
                }

                log.info("operation=deleteAccount_init userId={}", userId);

                // Delete Cognito User
                try {
                        software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest deleteRequest = software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest
                                        .builder()
                                        .userPoolId(cognitoProperties.getUserPoolId())
                                        .username(userId) // OidcUser subject is the username/sub
                                        .build();

                        cognitoClient.adminDeleteUser(deleteRequest);
                        log.info("operation=deleteAccount_cognito_success userId={}", userId);
                } catch (Exception e) {
                        log.error("operation=deleteAccount_cognito_failed userId={} error={}", userId, e.getMessage());
                        throw new RuntimeException("Failed to delete user: " + e.getMessage());
                }
        }
}
