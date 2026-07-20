package com.school.ticket.dto;

import com.school.ticket.entity.TicketLog;

import java.time.LocalDateTime;

public record TicketLogResponse(
        Long id,
        String content,
        String author,
        LocalDateTime createdAt
) {
    public static TicketLogResponse from(TicketLog l) {
        return new TicketLogResponse(l.getId(), l.getContent(), l.getAuthor(), l.getCreatedAt());
    }
}
