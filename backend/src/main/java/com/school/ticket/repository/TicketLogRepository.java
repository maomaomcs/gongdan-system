package com.school.ticket.repository;

import com.school.ticket.entity.TicketLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketLogRepository extends JpaRepository<TicketLog, Long> {
    List<TicketLog> findByTicketIdOrderByIdAsc(Long ticketId);
}
