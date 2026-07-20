package com.school.ticket.dto;

import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
        String status,
        @Size(max = 64) String handler,
        @Size(max = 1000) String resolution
) {}
