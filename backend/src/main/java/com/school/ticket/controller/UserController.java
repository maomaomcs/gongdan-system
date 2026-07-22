package com.school.ticket.controller;

import com.school.ticket.dto.CreateTicketRequest;
import com.school.ticket.dto.TicketResponse;
import com.school.ticket.dto.UserAuthRequest;
import com.school.ticket.entity.AppUser;
import com.school.ticket.service.AppUserService;
import com.school.ticket.service.TicketService;
import com.school.ticket.web.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户(老师)端接口
 * 公开:注册、登录;需登录:我的信息、提交报修、我的报修
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final AppUserService appUserService;
    private final TicketService ticketService;
    private final TokenStore tokenStore;

    // ---------- 公开:注册 / 登录 ----------
    @PostMapping("/user/register")
    public Map<String, Object> register(@Valid @RequestBody UserAuthRequest req) {
        AppUser u = appUserService.register(req);
        String token = tokenStore.issue(u.getUsername(), TokenStore.ROLE_USER);
        return sessionInfo(u, token);
    }

    @PostMapping("/user/login")
    public Map<String, Object> login(@RequestBody UserAuthRequest req) {
        AppUser u = appUserService.authenticate(req.username(), req.password());
        String token = tokenStore.issue(u.getUsername(), TokenStore.ROLE_USER);
        return sessionInfo(u, token);
    }

    // ---------- 需登录 ----------
    @GetMapping("/user/me")
    public Map<String, Object> me(HttpServletRequest request) {
        AppUser u = currentUser(request);
        return Map.of(
                "username", u.getUsername(),
                "displayName", u.getDisplayName() == null ? u.getUsername() : u.getDisplayName(),
                "phone", u.getPhone() == null ? "" : u.getPhone()
        );
    }

    @PostMapping("/user/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        tokenStore.revoke(TokenStore.extractToken(request));
        return Map.of("ok", true);
    }

    /** 提交报修(自动带上当前用户,报修人默认用账号姓名) */
    @PostMapping("/user/tickets")
    public Map<String, Object> submit(@Valid @RequestBody CreateTicketRequest req, HttpServletRequest request) {
        AppUser u = currentUser(request);
        // 若前端没填报修人/联系方式,用账号信息兜底
        CreateTicketRequest filled = new CreateTicketRequest(
                orElse(req.reporter(), u.getDisplayName() != null ? u.getDisplayName() : u.getUsername()),
                orElse(req.contact(), u.getPhone()),
                req.location(), req.category(), req.title(), req.description(), req.urgency(), req.images()
        );
        TicketResponse t = ticketService.create(filled, u.getId());
        return Map.of("code", t.code(), "ticket", t);
    }

    /** 我的报修列表(分页 + 按状态/关键词筛选) */
    @GetMapping("/user/tickets")
    public com.school.ticket.dto.PageResponse<TicketResponse> myTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        return ticketService.listByUser(currentUser(request).getId(), status, keyword, page, size);
    }

    /** 我的报修详情 */
    @GetMapping("/user/tickets/{id}")
    public TicketResponse myTicket(@PathVariable Long id, HttpServletRequest request) {
        return ticketService.getByIdForUser(id, currentUser(request).getId());
    }

    /** 催单 */
    @PostMapping("/user/tickets/{id}/urge")
    public TicketResponse urge(@PathVariable Long id, HttpServletRequest request) {
        return ticketService.urge(id, currentUser(request).getId());
    }

    /** 取消报修(报错了自己撤销,仅"待处理"时可取消) */
    @PostMapping("/user/tickets/{id}/cancel")
    public TicketResponse cancel(@PathVariable Long id, HttpServletRequest request) {
        return ticketService.cancelByUser(id, currentUser(request).getId());
    }

    // ---------- 辅助 ----------
    private AppUser currentUser(HttpServletRequest request) {
        return appUserService.getByUsername(tokenStore.currentUsername(request));
    }

    private Map<String, Object> sessionInfo(AppUser u, String token) {
        return Map.of(
                "token", token,
                "username", u.getUsername(),
                "displayName", u.getDisplayName() == null ? u.getUsername() : u.getDisplayName()
        );
    }

    private String orElse(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
