package com.learning.authservice.controller;

import com.learning.authservice.config.CognitoProperties;
import com.learning.authservice.signup.SignupService;
import com.learning.authservice.dto.SignupRequestDto;
import com.learning.authservice.dto.VerifyRequestDto;
import com.learning.common.dto.SignupResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SignupController.
 * Tests that the controller correctly delegates to SignupService
 * and maps responses appropriately.
 */
@ExtendWith(MockitoExtension.class)
class SignupControllerTest {

    @Mock
    private SignupService signupService;

    @Mock
    private CognitoIdentityProviderClient cognitoClient;

    @Mock
    private CognitoProperties cognitoProperties;

    private SignupController signupController;

    @BeforeEach
    void setUp() {
        signupController = new SignupController(signupService, cognitoClient, cognitoProperties);
    }

    @Test
    @DisplayName("Personal signup success - returns CREATED with unconfirmed status")
    void signup_Personal_Success() {
        // Arrange
        SignupRequestDto request = new SignupRequestDto(
                "test@gmail.com", "password123", "Test User", null, null, null);

        SignupResponse serviceResponse = SignupResponse.success(
                "Signup complete. Please verify your email.",
                "user-test-12345",
                false);
        when(signupService.signup(any(SignupRequestDto.class))).thenReturn(serviceResponse);

        // Act
        ResponseEntity<SignupResponse> response = signupController.signup(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().tenantId()).isEqualTo("user-test-12345");
        assertThat(response.getBody().userConfirmed()).isFalse();

        verify(signupService).signup(any(SignupRequestDto.class));
    }

    @Test
    @DisplayName("Organization signup success - returns CREATED")
    void signup_Organization_Success() {
        // Arrange
        SignupRequestDto request = new SignupRequestDto(
                "admin@acme.com", "password123", "Admin User", "Acme Corp", "STANDARD", null);

        SignupResponse serviceResponse = SignupResponse.success(
                "Signup complete. Please verify your email.",
                "acme-corp",
                false);
        when(signupService.signup(any(SignupRequestDto.class))).thenReturn(serviceResponse);

        // Act
        ResponseEntity<SignupResponse> response = signupController.signup(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().tenantId()).isEqualTo("acme-corp");

        verify(signupService).signup(any(SignupRequestDto.class));
    }

    @Test
    @DisplayName("Signup fails when service returns failure")
    void signup_Failure_ReturnsBadRequest() {
        // Arrange
        SignupRequestDto request = new SignupRequestDto(
                "existing@gmail.com", "password123", "Test User", null, null, null);
        SignupResponse failureResponse = SignupResponse.failure("User already exists");
        when(signupService.signup(any(SignupRequestDto.class))).thenReturn(failureResponse);

        // Act
        ResponseEntity<SignupResponse> response = signupController.signup(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).contains("already exists");
    }
}
