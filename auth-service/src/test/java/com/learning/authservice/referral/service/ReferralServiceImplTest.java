package com.learning.authservice.referral.service;

import com.learning.authservice.credit.dto.WalletDto;
import com.learning.authservice.credit.entity.ReferenceType;
import com.learning.authservice.credit.service.CreditService;
import com.learning.authservice.referral.config.ReferralProperties;
import com.learning.authservice.referral.dto.ReferralCodeDto;
import com.learning.authservice.referral.dto.ReferralStatsDto;
import com.learning.authservice.referral.entity.Referral;
import com.learning.authservice.referral.entity.ReferralStatus;
import com.learning.authservice.referral.repository.ReferralRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReferralServiceImpl.
 * Follows the CreditServiceImplTest pattern with @Nested groups.
 */
@ExtendWith(MockitoExtension.class)
class ReferralServiceImplTest {

        @Mock
        private ReferralRepository referralRepository;
        @Mock
        private CreditService creditService;
        @Mock
        private ReferralProperties referralProperties;
        @InjectMocks
        private ReferralServiceImpl referralService;

        private static final String REFERRER_ID = "referrer@test.com";
        private static final String REFEREE_ID = "referee@test.com";
        private static final String REFERRAL_CODE = "ABC12345";

        // ---- getOrCreateReferralCode ----

        @Nested
        @DisplayName("getOrCreateReferralCode")
        class GetOrCreateReferralCodeTests {

                @Test
                @DisplayName("returns existing code when user already has one")
                void returnsExistingCode() {
                        var existing = Referral.builder()
                                        .referrerUserId(REFERRER_ID)
                                        .referralCode(REFERRAL_CODE)
                                        .build();
                        when(referralRepository.findFirstByReferrerUserIdAndRefereeUserIdIsNull(REFERRER_ID))
                                        .thenReturn(Optional.of(existing));
                        when(referralProperties.getBaseUrl()).thenReturn("http://localhost:4200/auth/signup");

                        ReferralCodeDto result = referralService.getOrCreateReferralCode(REFERRER_ID);

                        assertThat(result.referralCode()).isEqualTo(REFERRAL_CODE);
                        assertThat(result.referralLink()).contains("ref=" + REFERRAL_CODE);
                        verify(referralRepository, never()).save(any());
                }

                @Test
                @DisplayName("generates new code when user has none")
                void generatesNewCode() {
                        when(referralRepository.findFirstByReferrerUserIdAndRefereeUserIdIsNull(REFERRER_ID))
                                        .thenReturn(Optional.empty());
                        when(referralRepository.findFirstByReferralCodeAndRefereeUserIdIsNull(anyString()))
                                        .thenReturn(Optional.empty()); // no collision
                        when(referralRepository.save(any(Referral.class)))
                                        .thenAnswer(inv -> inv.getArgument(0));
                        when(referralProperties.getBaseUrl()).thenReturn("http://localhost:4200/auth/signup");

                        ReferralCodeDto result = referralService.getOrCreateReferralCode(REFERRER_ID);

                        assertThat(result.referralCode()).hasSize(8);
                        assertThat(result.referralLink()).contains("ref=");

                        ArgumentCaptor<Referral> captor = ArgumentCaptor.forClass(Referral.class);
                        verify(referralRepository).save(captor.capture());
                        assertThat(captor.getValue().getReferrerUserId()).isEqualTo(REFERRER_ID);
                        assertThat(captor.getValue().getStatus()).isEqualTo(ReferralStatus.ACTIVE);
                        assertThat(captor.getValue().getRefereeUserId()).isNull();
                }
        }

        // ---- getReferralStats ----

        @Nested
        @DisplayName("getReferralStats")
        class GetReferralStatsTests {

                @Test
                @DisplayName("returns correct stats for user with referrals")
                void returnsCorrectStats() {
                        var seed = Referral.builder()
                                        .referrerUserId(REFERRER_ID)
                                        .referralCode(REFERRAL_CODE)
                                        .build();
                        when(referralRepository.findFirstByReferrerUserIdAndRefereeUserIdIsNull(REFERRER_ID))
                                        .thenReturn(Optional.of(seed));
                        when(referralRepository.countByReferrerUserIdAndStatus(REFERRER_ID, ReferralStatus.CONVERTED))
                                        .thenReturn(3L);
                        when(referralProperties.getRewardCredits()).thenReturn(2);

                        ReferralStatsDto stats = referralService.getReferralStats(REFERRER_ID);

                        assertThat(stats.referralCode()).isEqualTo(REFERRAL_CODE);
                        assertThat(stats.totalReferrals()).isEqualTo(3L);
                        assertThat(stats.totalCreditsEarned()).isEqualTo(6L);
                        assertThat(stats.rewardPerReferral()).isEqualTo(2);
                }

                @Test
                @DisplayName("returns zeroes for user with no referrals")
                void returnsZeroesForNewUser() {
                        when(referralRepository.findFirstByReferrerUserIdAndRefereeUserIdIsNull(REFERRER_ID))
                                        .thenReturn(Optional.empty());
                        when(referralRepository.countByReferrerUserIdAndStatus(REFERRER_ID, ReferralStatus.CONVERTED))
                                        .thenReturn(0L);
                        when(referralProperties.getRewardCredits()).thenReturn(2);

                        ReferralStatsDto stats = referralService.getReferralStats(REFERRER_ID);

                        assertThat(stats.referralCode()).isNull();
                        assertThat(stats.totalReferrals()).isZero();
                        assertThat(stats.totalCreditsEarned()).isZero();
                }
        }

        // ---- processReferral ----

        @Nested
        @DisplayName("processReferral")
        class ProcessReferralTests {

                @BeforeEach
                void setUp() {
                        lenient().when(referralProperties.getRewardCredits()).thenReturn(2);
                }

                @Test
                @DisplayName("grants credits to both referrer and referee on valid referral")
                void grantsCreditsTosBothParties() {
                        var seed = Referral.builder()
                                        .referrerUserId(REFERRER_ID)
                                        .referralCode(REFERRAL_CODE)
                                        .status(ReferralStatus.ACTIVE)
                                        .build();
                        when(referralRepository.findFirstByReferralCodeAndRefereeUserIdIsNull(REFERRAL_CODE))
                                        .thenReturn(Optional.of(seed));
                        when(referralRepository.existsByReferralCodeAndRefereeUserId(REFERRAL_CODE, REFEREE_ID))
                                        .thenReturn(false);
                        when(referralRepository.save(any(Referral.class)))
                                        .thenAnswer(inv -> inv.getArgument(0));
                        when(creditService.grantCredits(anyString(), anyInt(), any(), anyString(), anyString(),
                                        anyString()))
                                        .thenReturn(new WalletDto(10, 0, 10));

                        referralService.processReferral(REFERRAL_CODE, REFEREE_ID);

                        // Verify conversion record saved
                        ArgumentCaptor<Referral> captor = ArgumentCaptor.forClass(Referral.class);
                        verify(referralRepository).save(captor.capture());
                        assertThat(captor.getValue().getStatus()).isEqualTo(ReferralStatus.CONVERTED);
                        assertThat(captor.getValue().getRefereeUserId()).isEqualTo(REFEREE_ID);
                        assertThat(captor.getValue().getConvertedAt()).isNotNull();

                        // Verify credits granted to referrer
                        verify(creditService).grantCredits(
                                        eq(REFERRER_ID), eq(2), eq(ReferenceType.REFERRAL),
                                        eq(REFERRAL_CODE),
                                        eq("referral-referrer-" + REFERRAL_CODE + "-" + REFEREE_ID),
                                        contains("referring"));

                        // Verify credits granted to referee
                        verify(creditService).grantCredits(
                                        eq(REFEREE_ID), eq(2), eq(ReferenceType.REFERRAL),
                                        eq(REFERRAL_CODE),
                                        eq("referral-referee-" + REFERRAL_CODE + "-" + REFEREE_ID),
                                        contains("Welcome"));
                }

                @Test
                @DisplayName("rejects self-referral")
                void rejectsSelfReferral() {
                        var seed = Referral.builder()
                                        .referrerUserId(REFERRER_ID)
                                        .referralCode(REFERRAL_CODE)
                                        .build();
                        when(referralRepository.findFirstByReferralCodeAndRefereeUserIdIsNull(REFERRAL_CODE))
                                        .thenReturn(Optional.of(seed));

                        assertThatThrownBy(() -> referralService.processReferral(REFERRAL_CODE, REFERRER_ID))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("own referral code");

                        verify(creditService, never()).grantCredits(any(), anyInt(), any(), any(), any(), any());
                }

                @Test
                @DisplayName("rejects invalid referral code")
                void rejectsInvalidCode() {
                        when(referralRepository.findFirstByReferralCodeAndRefereeUserIdIsNull("INVALID"))
                                        .thenReturn(Optional.empty());

                        assertThatThrownBy(() -> referralService.processReferral("INVALID", REFEREE_ID))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Invalid referral code");

                        verify(creditService, never()).grantCredits(any(), anyInt(), any(), any(), any(), any());
                }

                @Test
                @DisplayName("rejects blank referral code")
                void rejectsBlankCode() {
                        assertThatThrownBy(() -> referralService.processReferral("", REFEREE_ID))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("blank");
                }

                @Test
                @DisplayName("skips duplicate referral (idempotent)")
                void skipsDuplicateReferral() {
                        var seed = Referral.builder()
                                        .referrerUserId(REFERRER_ID)
                                        .referralCode(REFERRAL_CODE)
                                        .build();
                        when(referralRepository.findFirstByReferralCodeAndRefereeUserIdIsNull(REFERRAL_CODE))
                                        .thenReturn(Optional.of(seed));
                        when(referralRepository.existsByReferralCodeAndRefereeUserId(REFERRAL_CODE, REFEREE_ID))
                                        .thenReturn(true); // already processed

                        referralService.processReferral(REFERRAL_CODE, REFEREE_ID);

                        verify(referralRepository, never()).save(any());
                        verify(creditService, never()).grantCredits(any(), anyInt(), any(), any(), any(), any());
                }

                @Test
                @DisplayName("rejects null referral code")
                void rejectsNullCode() {
                        assertThatThrownBy(() -> referralService.processReferral(null, REFEREE_ID))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("blank");

                        verify(creditService, never()).grantCredits(any(), anyInt(), any(), any(), any(), any());
                }

                @Test
                @DisplayName("rejects whitespace-only referral code")
                void rejectsWhitespaceOnlyCode() {
                        assertThatThrownBy(() -> referralService.processReferral("   \t  ", REFEREE_ID))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("blank");

                        verify(creditService, never()).grantCredits(any(), anyInt(), any(), any(), any(), any());
                }

                @Test
                @DisplayName("rejects numeric-only code that doesn't exist")
                void rejectsNumericOnlyCode() {
                        when(referralRepository.findFirstByReferralCodeAndRefereeUserIdIsNull("123456"))
                                        .thenReturn(Optional.empty());

                        assertThatThrownBy(() -> referralService.processReferral("123456", REFEREE_ID))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Invalid referral code");

                        verify(creditService, never()).grantCredits(any(), anyInt(), any(), any(), any(), any());
                }

                @Test
                @DisplayName("rejects code with special characters that doesn't exist")
                void rejectsSpecialCharCode() {
                        when(referralRepository.findFirstByReferralCodeAndRefereeUserIdIsNull("!@#$%^&*"))
                                        .thenReturn(Optional.empty());

                        assertThatThrownBy(() -> referralService.processReferral("!@#$%^&*", REFEREE_ID))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Invalid referral code");

                        verify(creditService, never()).grantCredits(any(), anyInt(), any(), any(), any(), any());
                }

                @Test
                @DisplayName("rejects negative number as code")
                void rejectsNegativeNumberCode() {
                        when(referralRepository.findFirstByReferralCodeAndRefereeUserIdIsNull("-12345"))
                                        .thenReturn(Optional.empty());

                        assertThatThrownBy(() -> referralService.processReferral("-12345", REFEREE_ID))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Invalid referral code");

                        verify(creditService, never()).grantCredits(any(), anyInt(), any(), any(), any(), any());
                }

                @Test
                @DisplayName("rejects very long string as code")
                void rejectsVeryLongCode() {
                        String longCode = "A".repeat(500);
                        when(referralRepository.findFirstByReferralCodeAndRefereeUserIdIsNull(longCode))
                                        .thenReturn(Optional.empty());

                        assertThatThrownBy(() -> referralService.processReferral(longCode, REFEREE_ID))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Invalid referral code");

                        verify(creditService, never()).grantCredits(any(), anyInt(), any(), any(), any(), any());
                }

                @Test
                @DisplayName("rejects SQL injection attempt as code")
                void rejectsSqlInjection() {
                        String sqlInjection = "'; DROP TABLE referrals; --";
                        when(referralRepository.findFirstByReferralCodeAndRefereeUserIdIsNull(sqlInjection))
                                        .thenReturn(Optional.empty());

                        assertThatThrownBy(() -> referralService.processReferral(sqlInjection, REFEREE_ID))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Invalid referral code");

                        verify(creditService, never()).grantCredits(any(), anyInt(), any(), any(), any(), any());
                }

                @Test
                @DisplayName("rejects URL as code")
                void rejectsUrlAsCode() {
                        String url = "http://localhost:4200/auth/signup?ref=ABC12345";
                        when(referralRepository.findFirstByReferralCodeAndRefereeUserIdIsNull(url))
                                        .thenReturn(Optional.empty());

                        assertThatThrownBy(() -> referralService.processReferral(url, REFEREE_ID))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Invalid referral code");

                        verify(creditService, never()).grantCredits(any(), anyInt(), any(), any(), any(), any());
                }
        }
}
