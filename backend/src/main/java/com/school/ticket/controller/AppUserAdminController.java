package com.school.ticket.controller;

import com.school.ticket.dto.AppUserResponse;
import com.school.ticket.dto.PageResponse;
import com.school.ticket.service.AppUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 老师(报修用户)账号管理(需管理员登录) */
@RestController
@RequestMapping("/api/admin/app-users")
@RequiredArgsConstructor
public class AppUserAdminController {

    private final AppUserService appUserService;

    /** 老师账号列表(分页 + 关键词搜索:用户名/姓名/联系方式) */
    @GetMapping
    public PageResponse<AppUserResponse> list(
            @RequestParam(required = false, name = "q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return appUserService.listPaged(keyword, page, size);
    }

    /** 启用 / 停用 */
    @PatchMapping("/{id}")
    public Map<String, Object> setEnabled(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        appUserService.setEnabled(id, Boolean.TRUE.equals(body.get("enabled")));
        return Map.of("ok", true);
    }

    /** 重置密码;body: { newPassword } */
    @PostMapping("/{id}/reset-password")
    public Map<String, Object> resetPassword(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        appUserService.resetPassword(id, body == null ? null : (String) body.get("newPassword"));
        return Map.of("ok", true);
    }

    /** 提升为管理员 */
    @PostMapping("/{id}/promote")
    public Map<String, Object> promote(@PathVariable Long id) {
        appUserService.promoteToAdmin(id);
        return Map.of("ok", true);
    }

    /** 删除账号 */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        appUserService.delete(id);
        return Map.of("ok", true);
    }
}
