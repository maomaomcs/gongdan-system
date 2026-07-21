package com.school.ticket.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.ticket.config.AppProperties;
import com.school.ticket.dto.Faq;
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
    private final ObjectMapper objectMapper;

    // 钉钉通知相关的设置键
    public static final String DING_ENABLED = "ding.enabled";
    public static final String DING_WEBHOOK = "ding.webhook";
    public static final String DING_SECRET = "ding.secret";
    public static final String DING_KEYWORD = "ding.keyword";

    // 报修选项(故障类型 / 常用位置),按行存储
    public static final String OPTION_CATEGORIES = "option.categories";
    public static final String OPTION_LOCATIONS = "option.locations";
    // 常见问题自助(JSON) / 超时预警小时数
    public static final String OPTION_FAQS = "option.faqs";
    public static final String OPTION_OVERDUE_HOURS = "option.overdue-hours";
    public static final int DEFAULT_OVERDUE_HOURS = 24;

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

    /** 常见问题自助 */
    @Transactional(readOnly = true)
    public List<Faq> getFaqs() {
        String raw = get(OPTION_FAQS, null);
        if (!StringUtils.hasText(raw)) return new ArrayList<>();
        try {
            return objectMapper.readValue(raw, new TypeReference<List<Faq>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Transactional
    public void setFaqs(List<Faq> faqs) {
        List<Faq> clean = new ArrayList<>();
        if (faqs != null) {
            for (Faq f : faqs) {
                if (f != null && StringUtils.hasText(f.q())) {
                    clean.add(new Faq(f.q().trim(), f.a() == null ? "" : f.a().trim()));
                }
            }
        }
        try {
            set(OPTION_FAQS, objectMapper.writeValueAsString(clean));
        } catch (Exception e) {
            throw new RuntimeException("保存常见问题失败: " + e.getMessage(), e);
        }
    }

    /** 超时预警小时数(工单待处理/处理中超过该时长即视为超时);未配置用默认 24 */
    @Transactional(readOnly = true)
    public int getOverdueHours() {
        String raw = get(OPTION_OVERDUE_HOURS, null);
        if (!StringUtils.hasText(raw)) return DEFAULT_OVERDUE_HOURS;
        try {
            int v = Integer.parseInt(raw.trim());
            return v > 0 ? v : DEFAULT_OVERDUE_HOURS;
        } catch (NumberFormatException e) {
            return DEFAULT_OVERDUE_HOURS;
        }
    }

    @Transactional
    public void setOverdueHours(int hours) {
        set(OPTION_OVERDUE_HOURS, String.valueOf(hours > 0 ? hours : DEFAULT_OVERDUE_HOURS));
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
