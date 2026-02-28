package com.learning.authservice.signup.actions;

import com.learning.authservice.referral.service.ReferralService;
import com.learning.authservice.signup.pipeline.SignupContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProcessReferralAction.
 */
@ExtendWith(MockitoExtension.class)
class ProcessReferralActionTest {

    @Mock
    private ReferralService referralService;

    @InjectMocks
    private ProcessReferralAction action;

    @Test
    @DisplayName("returns correct name and order")
    void nameAndOrder() {
        assertThat(action.getName()).isEqualTo("ProcessReferral");
        assertThat(action.getOrder()).isEqualTo(75);
    }

    @Test
    @DisplayName("supports context with referral code")
    void supportsWithReferralCode() {
        var ctx = SignupContext.builder()
                .email("user@test.com")
                .referralCode("ABC123")
                .signupType(SignupContext.SignupType.PERSONAL)
                .build();

        assertThat(action.supports(ctx)).isTrue();
    }

    @Test
    @DisplayName("does not support context without referral code")
    void doesNotSupportWithoutReferralCode() {
        var ctx = SignupContext.builder()
                .email("user@test.com")
                .signupType(SignupContext.SignupType.PERSONAL)
                .build();

        assertThat(action.supports(ctx)).isFalse();
    }

    @Test
    @DisplayName("does not support context with blank referral code")
    void doesNotSupportBlankCode() {
        var ctx = SignupContext.builder()
                .email("user@test.com")
                .referralCode("  ")
                .signupType(SignupContext.SignupType.PERSONAL)
                .build();

        assertThat(action.supports(ctx)).isFalse();
    }

    @Test
    @DisplayName("processes valid referral code during signup")
    void processesValidReferralCode() {
        var ctx = SignupContext.builder()
                .email("newuser@test.com")
                .referralCode("VALID123")
                .signupType(SignupContext.SignupType.PERSONAL)
                .build();

        assertThatCode(() -> action.execute(ctx)).doesNotThrowAnyException();

        verify(referralService).processReferral("VALID123", "newuser@test.com");
        assertThat(ctx.getMetadata("referralProcessed", Boolean.class)).isTrue();
    }

    @Test
    @DisplayName("does not fail signup when referral processing throws exception")
    void doesNotFailSignupOnError() {
        var ctx = SignupContext.builder()
                .email("newuser@test.com")
                .referralCode("BAD_CODE")
                .signupType(SignupContext.SignupType.PERSONAL)
                .build();

        doThrow(new IllegalArgumentException("Invalid referral code"))
                .when(referralService).processReferral("BAD_CODE", "newuser@test.com");

        // Should NOT throw — non-blocking action
        assertThatCode(() -> action.execute(ctx)).doesNotThrowAnyException();

        // Metadata should NOT be set since it failed
        assertThat(ctx.getMetadata("referralProcessed", Boolean.class)).isNull();
    }

    @Test
    @DisplayName("rollback is no-op")
    void rollbackIsNoOp() {
        var ctx = SignupContext.builder()
                .email("user@test.com")
                .build();

        assertThatCode(() -> action.rollback(ctx)).doesNotThrowAnyException();
        verifyNoInteractions(referralService);
    }
}
