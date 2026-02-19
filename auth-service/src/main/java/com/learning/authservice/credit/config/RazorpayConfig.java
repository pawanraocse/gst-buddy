package com.learning.authservice.credit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Razorpay configuration properties.
 * Bound from environment variables / application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "razorpay")
@Getter
@Setter
public class RazorpayConfig {

    /** Razorpay Key ID (public, safe for frontend). */
    private String keyId;

    /** Razorpay Key Secret (private, server-side only). */
    private String keySecret;

    /** Webhook secret for verifying Razorpay webhook signatures. */
    private String webhookSecret;
}
