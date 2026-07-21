package com.school.ticket.service;

import com.school.ticket.config.AppProperties;
import com.school.ticket.entity.SystemSetting;
import com.school.ticket.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** 系统设置读写(键值对) */
@Service
@RequiredArgsConstructor
public class SettingService {

    private final SystemSettingRepository repo;
    private final AppProperties props;

    // 钉钉通知相关的设置键
    public static final String DING_ENABLED = "ding.enabled";
    public static final String DING_WEBHOOK = "ding.webhook";
    public static final String DING_SECRET = "ding.secret";
    public static final String DING_KEYWORD = "ding.keyword";

    // 报修选项(故障类型 / 常用位置),按行存储
    public static final String OPTION_CATEGORIES = "option.categories";
    public static final String OPTION_LOCATIONS = "option.locations";

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

    // ---------- 报修选项:故障类型 / 常用位置 ----------

    /** 故障类型:后台未配置时回退到 application.yml 里的默认值 */
    @Transactional(readOnly = true)
    public List<String> getCategories() {
        List<String> list = splitLines(get(OPTION_CATEGORIES, null));
        if (!list.isEmpty()) return list;
        return props.getCategories() == null ? new ArrayList<>() : props.getCategories();
    }

    /** 常用位置:后台未配置则为空列表(报修页仍可自由填写) */
    @Transactional(readOnly = true)
    public List<String> getLocations() {
        return splitLines(get(OPTION_LOCATIONS, null));
    }

    @Transactional
    public void setCategories(List<String> items) {
        set(OPTION_CATEGORIES, joinLines(items));
    }

    @Transactional
    public void setLocations(List<String> items) {
        set(OPTION_LOCATIONS, joinLines(items));
    }

    private List<String> splitLines(String raw) {
        List<String> out = new ArrayList<>();
        if (!StringUtils.hasText(raw)) return out;
        for (String line : raw.split("\\r?\\n")) {
            String s = line.trim();
            if (!s.isEmpty() && !out.contains(s)) out.add(s);
        }
        return out;
    }

    private String joinLines(List<String> items) {
        if (items == null) return "";
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String s : items) {
            if (s != null && StringUtils.hasText(s)) set.add(s.trim());
        }
        return String.join("\n", set);
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
