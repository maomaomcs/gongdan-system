package com.school.ticket.controller;

import com.school.ticket.dto.PageResponse;
import com.school.ticket.entity.InviteCode;
import com.school.ticket.service.InviteCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 注册邀请码管理(需管理员登录) */
@RestController
@RequestMapping("/api/admin/invite-codes")
@RequiredArgsConstructor
public class InviteCodeController {

    private final InviteCodeService service;

    /** 邀请码列表(分页 + 关键词搜索:码/备注) */
    @GetMapping
    public PageResponse<InviteCode> list(
            @RequestParam(required = false, name = "q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.listPaged(keyword, page, size);
    }

    /** 生成新邀请码;body: { note, maxUses } */
    @PostMapping
    public InviteCode generate(@RequestBody(required = false) Map<String, Object> body) {
        String note = body == null ? null : (String) body.get("note");
        Integer maxUses = null;
        if (body != null && body.get("maxUses") != null) {
            maxUses = Integer.valueOf(String.valueOf(body.get("maxUses")));
        }
        return service.generate(note, maxUses);
    }

    /** 启用/停用 */
    @PatchMapping("/{id}")
    public Map<String, Object> setEnabled(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        service.setEnabled(id, Boolean.TRUE.equals(body.get("enabled")));
        return Map.of("ok", true);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        service.delete(id);
        return Map.of("ok", true);
    }
}
