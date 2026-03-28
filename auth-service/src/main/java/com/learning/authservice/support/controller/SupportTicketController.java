package com.learning.authservice.support.controller;

import com.learning.authservice.support.dto.AddTicketReplyRequest;
import com.learning.authservice.support.dto.CreateSupportTicketRequest;
import com.learning.authservice.support.dto.SupportTicketResponse;
import com.learning.authservice.support.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/support")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    @PostMapping("/public/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public SupportTicketResponse createPublicTicket(
            @Valid @RequestBody CreateSupportTicketRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Email is required for public support tickets");
        }
        return supportTicketService.createTicket(null, request);
    }

    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public SupportTicketResponse createTicket(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateSupportTicketRequest request) {
        return supportTicketService.createTicket(userId, request);
    }

    @GetMapping("/tickets/me")
    public List<SupportTicketResponse> getMyTickets(@RequestHeader("X-User-Id") String userId) {
        return supportTicketService.getUserTickets(userId);
    }

    @PostMapping("/tickets/{ticketId}/reply")
    public SupportTicketResponse addReply(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID ticketId,
            @Valid @RequestBody AddTicketReplyRequest request) {
        // User adding a reply
        return supportTicketService.addReply(ticketId, userId, false, request);
    }
}
