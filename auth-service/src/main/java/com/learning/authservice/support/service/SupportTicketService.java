package com.learning.authservice.support.service;

import com.learning.authservice.support.domain.SupportTicket;
import com.learning.authservice.support.domain.TicketReply;
import com.learning.authservice.support.dto.AddTicketReplyRequest;
import com.learning.authservice.support.dto.CreateSupportTicketRequest;
import com.learning.authservice.support.dto.SupportTicketResponse;
import com.learning.authservice.support.repository.SupportTicketRepository;
import com.learning.authservice.support.repository.SupportTicketSpecification;
import com.learning.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;

    @Transactional
    public SupportTicketResponse createTicket(String userId, CreateSupportTicketRequest request) {
        boolean enrolled = userId != null && !userId.isBlank();
        SupportTicket ticket = SupportTicket.builder()
                .userId(userId)
                .email(request.email())
                .subject(request.subject())
                .category(request.category())
                .description(request.description())
                .isEnrolled(enrolled)
                .status("OPEN")
                .tenantId(TenantContext.getCurrentTenant())
                .build();

        ticket = ticketRepository.save(ticket);
        log.info("Created support ticket {} for {} (enrolled={})", ticket.getId(),
                enrolled ? userId : request.email(), enrolled);
        return SupportTicketResponse.fromEntity(ticket);
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> getUserTickets(String userId) {
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(SupportTicketResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getAdminTickets(
            String status, String email, String category, Boolean isEnrolled, Pageable pageable) {
        String tenantId = TenantContext.getCurrentTenant();
        Page<SupportTicket> page = ticketRepository.findAll(
                SupportTicketSpecification.filterBy(tenantId, status, email, category, isEnrolled),
                pageable
        );
        return page.map(SupportTicketResponse::fromEntity);
    }

    @Transactional
    public SupportTicketResponse addReply(UUID ticketId, String userId, boolean isAdmin, AddTicketReplyRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        TicketReply reply = TicketReply.builder()
                .ticket(ticket)
                .userId(userId)
                .message(request.message())
                .isAdminReply(isAdmin)
                .build();

        ticket.getReplies().add(reply);
        
        // If an admin replies, it's typically "In Progress"
        if (isAdmin && "OPEN".equals(ticket.getStatus())) {
            ticket.setStatus("IN_PROGRESS");
        }

        ticket = ticketRepository.save(ticket);
        log.info("Added reply to ticket {} by user {}", ticketId, userId);
        return SupportTicketResponse.fromEntity(ticket);
    }

    @Transactional
    public SupportTicketResponse updateStatus(UUID ticketId, String status) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        ticket.setStatus(status);
        ticket = ticketRepository.save(ticket);
        log.info("Updated ticket {} status to {}", ticketId, status);
        return SupportTicketResponse.fromEntity(ticket);
    }
}
