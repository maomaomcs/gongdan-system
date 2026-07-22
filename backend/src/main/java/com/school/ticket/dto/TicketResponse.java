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
        String assetNo,
        String category,
        String title,
        String description,
        String urgency,
        String status,
        String handler,
        String resolution,
        List<String> images,
        Integer urgeCount,
        LocalDateTime lastUrgedAt,
        boolean overdue,
        boolean userUnread,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt,
        List<TicketLogResponse> logs
) {
    public static TicketResponse from(Ticket t, List<TicketLogResponse> logs, int overdueHours) {
        List<String> imgs = (t.getImages() == null || t.getImages().isBlank())
                ? List.of()
                : List.of(t.getImages().split(","));
        boolean overdue = false;
        if (overdueHours > 0 && t.getCreatedAt() != null
                && ("待处理".equals(t.getStatus()) || "处理中".equals(t.getStatus()))) {
            overdue = t.getCreatedAt().isBefore(LocalDateTime.now().minusHours(overdueHours));
        }
        return new TicketResponse(
                t.getId(), t.getCode(), t.getReporter(), t.getContact(), t.getLocation(), t.getAssetNo(),
                t.getCategory(), t.getTitle(), t.getDescription(), t.getUrgency(), t.getStatus(),
                t.getHandler(), t.getResolution(), imgs,
                t.getUrgeCount() == null ? 0 : t.getUrgeCount(), t.getLastUrgedAt(), overdue,
                Boolean.TRUE.equals(t.getUserUnread()),
                t.getCreatedAt(), t.getUpdatedAt(), t.getResolvedAt(),
                logs
        );
    }

    public static TicketResponse from(Ticket t, List<TicketLogResponse> logs) {
        return from(t, logs, 0);
    }

    public static TicketResponse from(Ticket t) {
        return from(t, null, 0);
    }

    public static TicketResponse from(Ticket t, int overdueHours) {
        return from(t, null, overdueHours);
    }
}
