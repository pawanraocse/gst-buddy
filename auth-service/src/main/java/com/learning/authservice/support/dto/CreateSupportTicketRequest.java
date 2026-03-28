package com.learning.authservice.support.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSupportTicketRequest(
        @NotBlank(message = "Subject is required")
        String subject,
        
        @NotBlank(message = "Category is required")
        String category,
        
        @NotBlank(message = "Description is required")
        String description,
        
        String email // Optional for logged-in users, required for public
) {
}
