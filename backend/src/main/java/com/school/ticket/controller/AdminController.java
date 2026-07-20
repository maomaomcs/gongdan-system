package com.school.ticket.controller;

import com.school.ticket.dto.AddLogRequest;
import com.school.ticket.dto.StatsResponse;
import com.school.ticket.dto.TicketResponse;
import com.school.ticket.dto.UpdateTicketRequest;
import com.school.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台接口(需登录):列表、详情、更新、跟进、统计
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TicketService ticketService;

    @GetMapping("/tickets")
    public List<TicketResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String urgency,
            @RequestParam(required = false) String location,
            @RequestParam(required = false, name = "q") String keyword) {
        return ticketService.list(status, category, urgency, location, keyword);
    }

    @GetMapping("/tickets/{id}")
    public TicketResponse detail(@PathVariable Long id) {
        return ticketService.getById(id);
    }

    @PatchMapping("/tickets/{id}")
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTicketRequest req) {
        return ticketService.update(id, req);
    }

    @PostMapping("/tickets/{id}/logs")
    public TicketResponse addLog(@PathVariable Long id, @Valid @RequestBody AddLogRequest req) {
        return ticketService.addLog(id, req);
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return ticketService.stats();
    }
}
