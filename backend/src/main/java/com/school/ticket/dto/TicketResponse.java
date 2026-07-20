package com.school.ticket.dto;

import com.school.ticket.entity.Ticket;

import java.time.LocalDateTime;
import java.util.List;

public record TicketResponse(
        Long id,
        String code,
        String reporter,
        String contact,
        String location,
        String category,
        String title,
        String description,
        String urgency,
        String status,
        String handler,
        String resolution,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt,
        List<TicketLogResponse> logs
) {
    public static TicketResponse from(Ticket t, List<TicketLogResponse> logs) {
        return new TicketResponse(
                t.getId(), t.getCode(), t.getReporter(), t.getContact(), t.getLocation(),
                t.getCategory(), t.getTitle(), t.getDescription(), t.getUrgency(), t.getStatus(),
                t.getHandler(), t.getResolution(), t.getCreatedAt(), t.getUpdatedAt(), t.getResolvedAt(),
                logs
        );
    }

    public static TicketResponse from(Ticket t) {
        return from(t, null);
    }
}
