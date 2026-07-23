package com.school.ticket.service;

import com.school.ticket.entity.VisitLog;
import com.school.ticket.repository.VisitLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;

/** 访问统计:记录访问 + 聚合查询(自建、数据留本地、IP 脱敏) */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final VisitLogRepository repo;

    /** 记录一次前台页面访问 */
    public void record(HttpServletRequest req, String path, String referrer) {
        if (path == null || path.isBlank()) path = "/";
        if (path.startsWith("/admin") || path.startsWith("/api")) return;
        if (path.length() > 300) path = path.substring(0, 300);

        String ip = clientIp(req);
        String ua = nz(req.getHeader("User-Agent"), "");
        LocalDate today = LocalDate.now();

        VisitLog v = new VisitLog();
        v.setDay(today);
        v.setPath(path);
        v.setRefererDomain(refererDomain(referrer, req));
        v.setVisitorHash(sha256(ip + "|" + ua + "|" + today));
        v.setDevice(isMobile(ua) ? "mobile" : "desktop");
        repo.save(v);
    }

    /** 聚合统计,days=最近天数(含今天) */
    public Map<String, Object> stats(int days) {
        int d = Math.min(90, Math.max(1, days));
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(d - 1L);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("today", Map.of("pv", repo.countByDay(today), "uv", repo.uvOfDay(today)));
        out.put("yesterday", Map.of("pv", repo.countByDay(today.minusDays(1)), "uv", repo.uvOfDay(today.minusDays(1))));
        out.put("range", Map.of("days", d, "pv", repo.countByDayGreaterThanEqual(from), "uv", repo.uvSince(from)));

        Map<String, long[]> byDay = new HashMap<>();
        for (Object[] r : repo.trend(from)) {
            byDay.put(r[0].toString(), new long[]{ ((Number) r[1]).longValue(), ((Number) r[2]).longValue() });
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i < d; i++) {
            LocalDate day = from.plusDays(i);
            long[] pu = byDay.getOrDefault(day.toString(), new long[]{0, 0});
            trend.add(Map.of("day", day.toString(), "pv", pu[0], "uv", pu[1]));
        }
        out.put("trend", trend);

        PageRequest top10 = PageRequest.of(0, 10);
        out.put("referers", buckets(repo.topReferers(from, top10)));
        out.put("pages", buckets(repo.topPages(from, top10)));
        out.put("devices", buckets(repo.deviceSplit(from)));
        return out;
    }

    private List<Map<String, Object>> buckets(List<Object[]> rows) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] r : rows) {
            String name = r[0] == null ? "(未知)" : r[0].toString();
            list.add(Map.of("name", name, "count", ((Number) r[1]).longValue()));
        }
        return list;
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return nz(req.getRemoteAddr(), "0.0.0.0");
    }

    private boolean isMobile(String ua) {
        String s = ua.toLowerCase();
        return s.contains("mobile") || s.contains("android") || s.contains("iphone")
                || s.contains("ipad") || s.contains("ipod") || s.contains("micromessenger");
    }

    private String refererDomain(String referrer, HttpServletRequest req) {
        if (referrer == null || referrer.isBlank()) return "直接访问";
        try {
            String host = new java.net.URI(referrer).getHost();
            if (host == null || host.isBlank()) return "直接访问";
            host = host.startsWith("www.") ? host.substring(4) : host;
            String self = req.getServerName();
            if (self != null && (host.equalsIgnoreCase(self)
                    || (self.startsWith("www.") && host.equalsIgnoreCase(self.substring(4))))) {
                return "站内";
            }
            return host.length() > 120 ? host.substring(0, 120) : host;
        } catch (Exception e) {
            return "直接访问";
        }
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16 && i < b.length; i++) sb.append(String.format("%02x", b[i]));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }

    private static String nz(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }
}
