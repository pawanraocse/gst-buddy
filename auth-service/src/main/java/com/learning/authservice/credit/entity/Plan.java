package com.learning.authservice.credit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a purchasable pricing plan.
 * Plans are DB-driven — never hardcoded in frontend or backend.
 */
@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "price_inr", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal priceInr = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer credits;

    @Column(name = "is_trial", nullable = false)
    @Builder.Default
    private Boolean isTrial = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** NULL = no expiry. Future: set to e.g. 90 for 3-month validity. */
    @Column(name = "validity_days")
    private Integer validityDays;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
