package com.learning.authservice.support.controller;

import com.learning.authservice.support.dto.AddTicketReplyRequest;
import com.learning.authservice.support.dto.SupportTicketResponse;
import com.learning.authservice.support.service.SupportTicketService;
import com.learning.common.infra.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/support/admin/tickets")
@RequiredArgsConstructor
public class AdminSupportTicketController {

    private final SupportTicketService supportTicketService;

    @GetMapping
    @RequirePermission(resource = "support", action = "manage")
    public Page<SupportTicketResponse> getAllTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean isEnrolled,
            Pageable pageable) {
        return supportTicketService.getAdminTickets(status, email, category, isEnrolled, pageable);
    }

    @PostMapping("/{ticketId}/reply")
    @RequirePermission(resource = "support", action = "manage")
    public SupportTicketResponse addAdminReply(
            @RequestHeader("X-User-Id") String adminId,
            @PathVariable UUID ticketId,
            @Valid @RequestBody AddTicketReplyRequest request) {
        return supportTicketService.addReply(ticketId, adminId, true, request);
    }

    @PutMapping("/{ticketId}/status")
    @RequirePermission(resource = "support", action = "manage")
    public SupportTicketResponse updateStatus(
            @PathVariable UUID ticketId,
            @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status cannot be empty");
        }
        return supportTicketService.updateStatus(ticketId, status);
    }
}
