package com.school.ticket.controller;

import com.school.ticket.config.AppProperties;
import com.school.ticket.dto.CreateTicketRequest;
import com.school.ticket.dto.LoginRequest;
import com.school.ticket.dto.TicketResponse;
import com.school.ticket.entity.AdminUser;
import com.school.ticket.service.AdminUserService;
import com.school.ticket.service.TicketService;
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
    private final AdminUserService adminUserService;
    private final com.school.ticket.service.SettingService settingService;
    private final com.school.ticket.service.AnalyticsService analyticsService;

    /** 前台访问打点(SPA 每次换页上报),失败静默不影响用户 */
    @PostMapping("/track")
    public Map<String, Object> track(@RequestBody(required = false) Map<String, String> body,
                                     jakarta.servlet.http.HttpServletRequest request) {
        try {
            String path = body == null ? null : body.get("path");
            String referrer = body == null ? null : body.get("referrer");
            analyticsService.record(request, path, referrer);
        } catch (Exception ignored) {
        }
        return Map.of("ok", true);
    }

    /** 按工单号查询进度(公开保留,方便无账号临时查询) */
    @GetMapping("/tickets/code/{code}")
    public TicketResponse getByCode(@PathVariable String code) {
        return ticketService.getByCode(code);
    }

    /** 前端下拉配置 */
    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of(
                "categories", settingService.getCategories(),
                "locations", settingService.getLocations(),
                "faqs", settingService.getFaqs(),
                "statuses", props.getStatuses(),
                "urgencies", props.getUrgencies(),
                "registerNeedsInvite", true
        );
    }

    /** 管理员登录(用户名 + 密码) */
    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest req) {
        AdminUser u = adminUserService.authenticate(req.username().trim(), req.password());
        String token = tokenStore.issue(u.getUsername(), TokenStore.ROLE_ADMIN);
        return Map.of(
                "token", token,
                "username", u.getUsername(),
                "displayName", u.getDisplayName() == null ? u.getUsername() : u.getDisplayName()
        );
    }
}
