package com.learning.authservice.referral.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized configuration for the referral system.
 *
 * <p>Defaults can be overridden via application.yml or
 * the {@code APP_REFERRAL_REWARD_CREDITS} environment variable.</p>
 */
@Component
@ConfigurationProperties(prefix = "app.referral")
public class ReferralProperties {

    /** Number of credits granted to both referrer and referee on successful referral. */
    private int rewardCredits = 2;

    /** Base URL used to construct shareable referral links. */
    private String baseUrl = "http://localhost:4200/auth/signup";

    public int getRewardCredits() {
        return rewardCredits;
    }

    public void setRewardCredits(int rewardCredits) {
        this.rewardCredits = rewardCredits;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
