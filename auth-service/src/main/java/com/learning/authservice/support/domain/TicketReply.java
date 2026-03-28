package com.learning.authservice.support.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_replies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketReply {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SupportTicket ticket;

    @Column(name = "user_id", length = 255)
    private String userId; // Nullable for public replies, but typically admin/user

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_admin_reply", nullable = false)
    @Builder.Default
    private boolean isAdminReply = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
