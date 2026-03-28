package com.learning.authservice.support.dto;

import jakarta.validation.constraints.NotBlank;

public record AddTicketReplyRequest(
        @NotBlank(message = "Message cannot be empty")
        String message
) {
}
