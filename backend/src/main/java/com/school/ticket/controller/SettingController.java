package com.school.ticket.controller;

import com.school.ticket.dto.DingSettingsRequest;
import com.school.ticket.service.DingTalkNotifier;
import com.school.ticket.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统设置(需登录)—— 目前含钉钉新工单通知配置
 */
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;
    private final DingTalkNotifier dingTalkNotifier;

    /** 读取钉钉通知配置(不返回 secret 明文) */
    @GetMapping("/ding")
    public Map<String, Object> getDing() {
        return settingService.getDingConfig();
    }

    /** 保存钉钉通知配置 */
    @PutMapping("/ding")
    public Map<String, Object> saveDing(@RequestBody DingSettingsRequest req) {
        settingService.set(SettingService.DING_ENABLED, String.valueOf(Boolean.TRUE.equals(req.enabled())));
        if (req.webhook() != null) settingService.set(SettingService.DING_WEBHOOK, req.webhook().trim());
        if (req.keyword() != null) settingService.set(SettingService.DING_KEYWORD, req.keyword().trim());
        // secret:null=不动;"__CLEAR__"=清空;其他=更新
        if (req.secret() != null) {
            if ("__CLEAR__".equals(req.secret())) {
                settingService.set(SettingService.DING_SECRET, "");
            } else if (!req.secret().isBlank()) {
                settingService.set(SettingService.DING_SECRET, req.secret().trim());
            }
        }
        return settingService.getDingConfig();
    }

    /** 发送测试通知(先保存再测更方便,这里直接用当前已存配置) */
    @PostMapping("/ding/test")
    public Map<String, Object> testDing() {
        dingTalkNotifier.sendTest();
        return Map.of("ok", true, "message", "测试通知已发送,请查看钉钉群");
    }
}
