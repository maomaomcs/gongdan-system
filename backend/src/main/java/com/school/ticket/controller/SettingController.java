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
        if (req.actionBase() != null) settingService.set(SettingService.DING_ACTION_BASE, req.actionBase().trim().replaceAll("/+$", ""));
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

    // ---------- 报修选项:故障类型 / 常用位置 ----------

    /** 读取报修选项:故障类型 / 常用位置 / 常见问题 / 超时小时数 */
    @GetMapping("/options")
    public Map<String, Object> getOptions() {
        return Map.of(
                "categories", settingService.getCategories(),
                "locations", settingService.getLocations(),
                "faqs", settingService.getFaqs(),
                "overdueHours", settingService.getOverdueHours()
        );
    }

    /** 保存报修选项 */
    @PutMapping("/options")
    public Map<String, Object> saveOptions(@RequestBody OptionsRequest req) {
        if (req.categories() != null) settingService.setCategories(req.categories());
        if (req.locations() != null) settingService.setLocations(req.locations());
        if (req.faqs() != null) settingService.setFaqs(req.faqs());
        if (req.overdueHours() != null) settingService.setOverdueHours(req.overdueHours());
        return getOptions();
    }

    public record OptionsRequest(
            java.util.List<String> categories,
            java.util.List<String> locations,
            java.util.List<com.school.ticket.dto.Faq> faqs,
            Integer overdueHours) {}
}
