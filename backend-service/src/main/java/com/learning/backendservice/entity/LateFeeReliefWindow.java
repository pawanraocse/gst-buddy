package com.learning.backendservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * JPA entity for CBIC notification-driven GST late fee waiver/reduction periods.
 *
 * <p>Seeded from CBIC notifications and referenced by the Late Fee Audit Rule
 * to determine whether a filing qualifies for fee relief.
 *
 * <p>Examples:
 * <ul>
 *   <li>Notification 19/2021-CT — GSTR-3B late fee waiver (Jun 2021 amnesty)</li>
 *   <li>Notification 07/2023-CT — GSTR-9/9C late fee cap ₹10,000 + ₹10,000</li>
 * </ul>
 */
@Entity
@Table(name = "late_fee_relief_windows")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LateFeeReliefWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** GSTR1 | GSTR3B | GSTR9 | GSTR9C */
    @Column(name = "return_type", nullable = false, length = 10)
    private String returnType;

    /** e.g. "19/2021-Central Tax" */
    @Column(name = "notification_no", nullable = false, length = 100)
    private String notificationNo;

    /** Window start date (filing date must be within this range to qualify) */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Window end date */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Inclusive start of the tax period this relief covers.
     * NULL = applies to all tax periods within the filing date window.
     */
    @Column(name = "tax_period_from")
    private LocalDate taxPeriodFrom;

    /**
     * Inclusive end of the tax period this relief covers.
     * NULL = applies to all tax periods within the filing date window.
     */
    @Column(name = "tax_period_to")
    private LocalDate taxPeriodTo;

    /** CGST late fee per day during this window (null = waived) */
    @Column(name = "fee_cgst_per_day", precision = 8, scale = 2)
    private BigDecimal feeCgstPerDay;

    /** SGST late fee per day during this window (null = waived) */
    @Column(name = "fee_sgst_per_day", precision = 8, scale = 2)
    private BigDecimal feeSgstPerDay;

    /** Maximum CGST late fee cap during this window (null = no cap) */
    @Column(name = "max_cap_cgst", precision = 12, scale = 2)
    private BigDecimal maxCapCgst;

    /** Maximum SGST late fee cap during this window (null = no cap) */
    @Column(name = "max_cap_sgst", precision = 12, scale = 2)
    private BigDecimal maxCapSgst;

    /** NIL = nil-return filers only; NON_NIL = taxable filers; ALL = both */
    @Column(name = "applies_to", nullable = false, length = 20)
    @Builder.Default
    private String appliesTo = "ALL";

    /** Additional notes / legal context */
    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
