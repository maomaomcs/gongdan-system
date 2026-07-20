package com.school.ticket.service;

import com.school.ticket.entity.SystemSetting;
import com.school.ticket.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/** 系统设置读写(键值对) */
@Service
@RequiredArgsConstructor
public class SettingService {

    private final SystemSettingRepository repo;

    // 钉钉通知相关的设置键
    public static final String DING_ENABLED = "ding.enabled";
    public static final String DING_WEBHOOK = "ding.webhook";
    public static final String DING_SECRET = "ding.secret";
    public static final String DING_KEYWORD = "ding.keyword";

    @Transactional(readOnly = true)
    public String get(String key, String defaultValue) {
        return repo.findById(key).map(SystemSetting::getValue).orElse(defaultValue);
    }

    @Transactional(readOnly = true)
    public boolean getBool(String key) {
        return "true".equalsIgnoreCase(get(key, "false"));
    }

    @Transactional
    public void set(String key, String value) {
        SystemSetting s = repo.findById(key).orElseGet(() -> {
            SystemSetting n = new SystemSetting();
            n.setKey(key);
            return n;
        });
        s.setValue(value);
        repo.save(s);
    }

    /** 读取钉钉配置(secret 不回传前端明文,仅返回是否已设置) */
    @Transactional(readOnly = true)
    public Map<String, Object> getDingConfig() {
        Map<String, Object> m = new HashMap<>();
        m.put("enabled", getBool(DING_ENABLED));
        m.put("webhook", get(DING_WEBHOOK, ""));
        m.put("keyword", get(DING_KEYWORD, ""));
        m.put("secretSet", !get(DING_SECRET, "").isEmpty());
        return m;
    }
}
