package com.school.ticket.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单跟进记录
 */
@Data
@Entity
@Table(name = "ticket_log", indexes = {
        @Index(name = "idx_ticket", columnList = "ticketId")
})
public class TicketLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ticketId;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(length = 64)
    private String author;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
