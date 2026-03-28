package com.learning.authservice.support.dto;

import com.learning.authservice.support.domain.SupportTicket;
import com.learning.authservice.support.domain.TicketReply;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SupportTicketResponse(
        UUID id,
        String subject,
        String category,
        String description,
        String status,
        String userId,
        String email,
        boolean isEnrolledUser,
        Instant createdAt,
        Instant updatedAt,
        List<TicketReplyResponse> replies
) {
    public static SupportTicketResponse fromEntity(SupportTicket ticket) {
        return new SupportTicketResponse(
                ticket.getId(),
                ticket.getSubject(),
                ticket.getCategory(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getUserId(),
                ticket.getEmail(),
                ticket.isEnrolled(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getReplies() != null
                    ? ticket.getReplies().stream().map(TicketReplyResponse::fromEntity).toList()
                    : List.of()
        );
    }
}

record TicketReplyResponse(
        UUID id,
        String message,
        String userId,
        boolean isAdminReply,
        Instant createdAt
) {
    public static TicketReplyResponse fromEntity(TicketReply reply) {
        return new TicketReplyResponse(
                reply.getId(),
                reply.getMessage(),
                reply.getUserId(),
                reply.isAdminReply(),
                reply.getCreatedAt()
        );
    }
}
