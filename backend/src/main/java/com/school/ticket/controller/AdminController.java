package com.school.ticket.controller;

import com.school.ticket.dto.AddLogRequest;
import com.school.ticket.dto.StatsResponse;
import com.school.ticket.dto.TicketResponse;
import com.school.ticket.dto.UpdateTicketRequest;
import com.school.ticket.service.ExcelExportService;
import com.school.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * 后台接口(需登录):列表、详情、更新、跟进、统计、导出
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TicketService ticketService;
    private final ExcelExportService excelExportService;

    @GetMapping("/tickets")
    public com.school.ticket.dto.PageResponse<TicketResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String urgency,
            @RequestParam(required = false) String location,
            @RequestParam(required = false, name = "q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ticketService.listPaged(status, category, urgency, location, keyword, page, size);
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

    /** 导出工单为 Excel(支持与列表相同的筛选条件) */
    @GetMapping("/tickets/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String urgency,
            @RequestParam(required = false) String location,
            @RequestParam(required = false, name = "q") String keyword) {
        List<TicketResponse> list = ticketService.listAll(status, category, urgency, location, keyword);
        byte[] data = excelExportService.exportTickets(list);
        String filename = "报修工单_" + LocalDate.now() + ".xlsx";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"tickets.xlsx\"; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
