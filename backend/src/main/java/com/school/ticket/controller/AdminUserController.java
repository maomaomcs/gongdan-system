package com.school.ticket.controller;

import com.school.ticket.dto.AdminUserResponse;
import com.school.ticket.dto.ChangePasswordRequest;
import com.school.ticket.dto.CreateAdminRequest;
import com.school.ticket.entity.AdminUser;
import com.school.ticket.service.AdminUserService;
import com.school.ticket.web.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员账号管理(需登录)
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final TokenStore tokenStore;

    /** 当前登录用户信息 */
    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        String username = tokenStore.currentUsername(request);
        AdminUser u = adminUserService.getByUsername(username);
        return Map.of(
                "username", u.getUsername(),
                "displayName", u.getDisplayName() == null ? u.getUsername() : u.getDisplayName()
        );
    }

    /** 退出登录(使当前 token 失效) */
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        tokenStore.revoke(TokenStore.extractToken(request));
        return Map.of("ok", true);
    }

    /** 修改自己的密码 */
    @PostMapping("/change-password")
    public Map<String, Object> changePassword(@Valid @RequestBody ChangePasswordRequest req, HttpServletRequest request) {
        String username = tokenStore.currentUsername(request);
        adminUserService.changePassword(username, req.oldPassword(), req.newPassword());
        return Map.of("ok", true);
    }

    /** 账号列表 */
    @GetMapping("/users")
    public List<AdminUserResponse> list() {
        return adminUserService.list();
    }

    /** 新增账号 */
    @PostMapping("/users")
    public AdminUserResponse create(@Valid @RequestBody CreateAdminRequest req) {
        return adminUserService.create(req);
    }

    /** 启用/停用账号 */
    @PatchMapping("/users/{id}")
    public Map<String, Object> setEnabled(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                          HttpServletRequest request) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        adminUserService.setEnabled(id, enabled, tokenStore.currentUsername(request));
        return Map.of("ok", true);
    }

    /** 删除账号 */
    @DeleteMapping("/users/{id}")
    public Map<String, Object> delete(@PathVariable Long id, HttpServletRequest request) {
        adminUserService.delete(id, tokenStore.currentUsername(request));
        return Map.of("ok", true);
    }
}
