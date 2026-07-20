package com.school.ticket.controller;

import com.school.ticket.config.AppProperties;
import com.school.ticket.dto.CreateTicketRequest;
import com.school.ticket.dto.LoginRequest;
import com.school.ticket.dto.TicketResponse;
import com.school.ticket.service.TicketService;
import com.school.ticket.web.ApiException;
import com.school.ticket.web.TokenStore;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 公开接口(无需登录):提交报修、查询进度、获取配置、登录
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PublicController {

    private final TicketService ticketService;
    private final TokenStore tokenStore;
    private final AppProperties props;

    /** 提交报修 */
    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody CreateTicketRequest req) {
        TicketResponse t = ticketService.create(req);
        return Map.of("code", t.code(), "ticket", t);
    }

    /** 按工单号查询进度 */
    @GetMapping("/tickets/code/{code}")
    public TicketResponse getByCode(@PathVariable String code) {
        return ticketService.getByCode(code);
    }

    /** 前端下拉配置 */
    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of(
                "categories", props.getCategories(),
                "statuses", props.getStatuses(),
                "urgencies", props.getUrgencies()
        );
    }

    /** 管理员登录 */
    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest req) {
        if (!props.getAdminPassword().equals(req.password())) {
            throw new ApiException(401, "密码错误");
        }
        return Map.of("token", tokenStore.issue());
    }
}
