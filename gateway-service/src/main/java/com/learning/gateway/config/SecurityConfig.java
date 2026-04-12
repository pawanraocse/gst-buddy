package com.learning.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

        @Value("${cors.allowed-origins:http://localhost:4200}")
        private String allowedOrigins;

        // Injected from COGNITO_JWKS_URI env var (set by entrypoint.sh from SSM)
        @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
        private String jwkSetUri;

        // Injected from COGNITO_ISSUER_URI env var (set by entrypoint.sh from SSM).
        // Optional: if blank (e.g. SSM unreachable), issuer validation is skipped with a warning.
        @Value("${cognito.issuer-uri:}")
        private String issuerUri;

        /**
         * Explicit ReactiveJwtDecoder bean.
         *
         * <p>We define this explicitly to avoid Spring Security's auto-configuration registering
         * TWO beans (jwtDecoder + jwtDecoderByIssuerUri) when both {@code jwk-set-uri} and
         * {@code issuer-uri} are present in application.yml, which causes a startup crash:
         * "required a single bean, but 2 were found".
         *
         * <p>By defining a single bean here keyed on the JWKS URI, and attaching an issuer
         * validator programmatically, we get the same security guarantees without the conflict.
         */
        @Bean
        public ReactiveJwtDecoder reactiveJwtDecoder() {
                NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                                .withJwkSetUri(jwkSetUri)
                                .build();

                List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
                validators.add(new JwtTimestampValidator());

                if (issuerUri != null && !issuerUri.isBlank()) {
                        validators.add(new JwtIssuerValidator(issuerUri));
                        log.info("JWT issuer validation enabled for: {}", issuerUri);
                } else {
                        log.warn("COGNITO_ISSUER_URI is not set — JWT issuer claim validation is DISABLED. " +
                                        "Ensure SSM parameter /gstbuddies/dev/cognito/issuer_uri is reachable.");
                }

                decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
                return decoder;
        }

        @Bean
        public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
                log.info("Configuring security filter chain");

                http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeExchange(exchange -> exchange
                                                .pathMatchers(
                                                                // Public auth endpoints (no JWT required)
                                                                "/auth/api/v1/auth/login",
                                                                "/auth/api/v1/auth/signup/**",
                                                                "/auth/api/v1/auth/tokens", "/auth/api/v1/auth/logout",
                                                                "/auth/api/v1/auth/verify",
                                                                "/auth/api/v1/auth/resend-verification",
                                                                // Forgot password endpoints
                                                                "/auth/api/v1/auth/forgot-password",
                                                                "/auth/api/v1/auth/reset-password",
                                                                // Multi-tenant login endpoints
                                                                "/auth/api/v1/auth/lookup",
                                                                "/auth/api/v1/auth/last-accessed",

                                                                // System endpoints
                                                                "/actuator/**", "/fallback",

                                                                // Configuration endpoints (Public)
                                                                "/api/config/**",

                                                                // Credit system (plans are public, credits use gateway JWT)
                                                                "/auth/api/v1/plans",
                                                                "/auth/api/v1/credits/**",

                                                                // Support system
                                                                "/auth/api/v1/support/public/**",
                                                                "/auth/api/v1/support/tickets/**",
                                                                "/auth/api/v1/support/admin/tickets/**",

                                                                // Referral system (uses gateway JWT filter for X-User-Id)
                                                                "/auth/api/v1/referral/**",

                                                                // Payment system
                                                                "/auth/api/v1/payments/**",

                                                                // Bootstrap endpoint (protected by X-Internal-Api-Key, not JWT)
                                                                "/auth/api/v1/admin/bootstrap/**")
                                                .permitAll()
                                                // Allow API key authenticated requests (validated by ApiKeyAuthenticationFilter)
                                                .matchers(ex -> ex.getRequest().getHeaders().containsKey("X-API-Key")
                                                                ? org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult
                                                                                .match()
                                                                : org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult
                                                                                .notMatch())
                                                .permitAll()
                                                .anyExchange().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                                                new JwtAuthenticationConverter())))
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(authenticationEntryPoint())
                                                .accessDeniedHandler(accessDeniedHandler()));

                return http.build();
        }

        private Mono<Void> writeJsonResponse(ServerWebExchange exchange, HttpStatus status, String code,
                        String message) {
                String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
                String body = String.format(
                                "{\"timestamp\":\"%s\",\"status\":%d,\"code\":\"%s\",\"message\":\"%s\",\"requestId\":\"%s\"}",
                                Instant.now(), status.value(), code, message, requestId != null ? requestId : "none");
                exchange.getResponse().setStatusCode(status);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return exchange.getResponse().writeWith(
                                Mono.fromSupplier(() -> exchange.getResponse().bufferFactory()
                                                .wrap(body.getBytes(StandardCharsets.UTF_8))));
        }

        @Bean
        public ServerAuthenticationEntryPoint authenticationEntryPoint() {
                return (exchange, ex) -> {
                        log.error("Unauthorized access: uri={}, requestId={}. Error: {}",
                                        exchange.getRequest().getURI(),
                                        exchange.getRequest().getHeaders().getFirst("X-Request-Id"),
                                        ex.getMessage(), ex);
                        return writeJsonResponse(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                                        "Authentication required: " + ex.getMessage());
                };
        }

        @Bean
        public ServerAccessDeniedHandler accessDeniedHandler() {
                return (exchange, ex) -> {
                        log.error("Access denied: uri={}, requestId={}. Error: {}",
                                        exchange.getRequest().getURI(),
                                        exchange.getRequest().getHeaders().getFirst("X-Request-Id"),
                                        ex.getMessage(), ex);
                        return writeJsonResponse(exchange, HttpStatus.FORBIDDEN, "FORBIDDEN",
                                        "Access denied: " + ex.getMessage());
                };
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                log.info("CORS configured with allowed origins: {}", allowedOrigins);
                return source;
        }
}
