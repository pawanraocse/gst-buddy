package com.learning.authservice.controller;

import com.learning.authservice.config.CognitoProperties;
import com.learning.authservice.dto.VerifyRequestDto;
import com.learning.authservice.dto.SignupRequestDto;
import com.learning.authservice.signup.SignupService;
import com.learning.common.dto.SignupResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CodeMismatchException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ExpiredCodeException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Controller for handling user signup flows (B2C and B2B).
 * Thin controller that delegates to SignupService.
 */
@RestController
@RequestMapping("/api/v1/auth/signup")
@Slf4j
public class SignupController {

        private final SignupService signupService;
        private final CognitoIdentityProviderClient cognitoClient;
        private final CognitoProperties cognitoProperties;

        public SignupController(
                        SignupService signupService,
                        CognitoIdentityProviderClient cognitoClient,
                        CognitoProperties cognitoProperties) {
                this.signupService = signupService;
                this.cognitoClient = cognitoClient;
                this.cognitoProperties = cognitoProperties;
        }

        /**
         * Unified Signup Flow.
         * Handles both Personal and Organization signups based on request data.
         */
        @PostMapping("/")
        public ResponseEntity<SignupResponse> signup(
                        @RequestBody @Valid com.learning.authservice.dto.SignupRequestDto request) {
                log.info("Signup initiated: email={}", request.email());

                SignupResponse response = signupService.signup(request);

                HttpStatus status = response.success() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
                return ResponseEntity.status(status).body(response);
        }

        /**
         * Verify Email with Code.
         * Confirms user signup after receiving verification code via email.
         */
        @PostMapping("/verify")
        public ResponseEntity<SignupResponse> verifyEmail(@RequestBody @Valid VerifyRequestDto request) {
                log.info("Email verification initiated: email={}", request.getEmail());

                try {
                        String secretHash = calculateSecretHash(request.getEmail());
                        String role = request.getRole() != null ? request.getRole() : "admin";

                        // NOTE: tenantType is NOT stored in Cognito - frontend looks it up from
                        // platform DB
                        ConfirmSignUpRequest confirmRequest = ConfirmSignUpRequest.builder()
                                        .clientId(cognitoProperties.getClientId())
                                        .username(request.getEmail())
                                        .confirmationCode(request.getCode())
                                        .secretHash(secretHash)
                                        .clientMetadata(Map.of(
                                                        "tenantId", "default",
                                                        "role", role))
                                        .build();

                        cognitoClient.confirmSignUp(confirmRequest);

                        // After confirmation, get user's sub from Cognito
                        AdminGetUserRequest getUserRequest = AdminGetUserRequest.builder()
                                        .userPoolId(cognitoProperties.getUserPoolId())
                                        .username(request.getEmail())
                                        .build();

                        AdminGetUserResponse userResponse = cognitoClient.adminGetUser(getUserRequest);
                        String userId = userResponse.userAttributes().stream()
                                        .filter(attr -> "sub".equals(attr.name()))
                                        .map(AttributeType::value)
                                        .findFirst()
                                        .orElseThrow(() -> new RuntimeException("Failed to get user sub from Cognito"));

                        log.info("✅ Email verified: email={} userId={}", request.getEmail(), userId);

                        log.info("✅ Email verified and role assigned: email={} userId={} role={}",
                                        request.getEmail(), userId, role);

                        return ResponseEntity.ok(
                                        SignupResponse.success("Email verified successfully. You can now log in.", null,
                                                        true));

                } catch (ExpiredCodeException e) {
                        log.error("Verification code expired: email={}", request.getEmail());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(SignupResponse.failure(
                                                        "Verification code has expired. Please request a new one."));
                } catch (CodeMismatchException e) {
                        log.error("Invalid verification code: email={}", request.getEmail());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(SignupResponse.failure("Invalid verification code. Please try again."));
                } catch (CognitoIdentityProviderException e) {
                        String errorMessage = e.awsErrorDetails().errorMessage();
                        if (errorMessage != null && errorMessage.contains("Current status is CONFIRMED")) {
                                log.info("User already confirmed, treating as success: {}", request.getEmail());
                                return ResponseEntity.ok(
                                                SignupResponse.success("Email verified successfully. You can now log in.", null,
                                                                true));
                        }
                        log.error("Cognito verification error: email={} error={}", request.getEmail(), e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(SignupResponse.failure(
                                                        "Verification failed: " + errorMessage));
                }
        }

        private String calculateSecretHash(String username) {
                try {
                        String message = username + cognitoProperties.getClientId();
                        Mac mac = Mac.getInstance("HmacSHA256");
                        SecretKeySpec secretKeySpec = new SecretKeySpec(
                                        cognitoProperties.getClientSecret().getBytes(StandardCharsets.UTF_8),
                                        "HmacSHA256");
                        mac.init(secretKeySpec);
                        byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
                        return Base64.getEncoder().encodeToString(rawHmac);
                } catch (Exception e) {
                        throw new RuntimeException("Error calculating secret hash", e);
                }
        }
}
