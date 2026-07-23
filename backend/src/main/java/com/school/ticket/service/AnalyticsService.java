package com.school.ticket.service;

import com.school.ticket.entity.VisitLog;
import com.school.ticket.repository.VisitLogRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.lionsoul.ip2region.xdb.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
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

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final VisitLogRepository repo;
    /** ip2region 离线库(线程不安全,search 时需加锁) */
    private Searcher ipSearcher;

    @PostConstruct
    void initIpSearcher() {
        try (var in = new ClassPathResource("ip2region.xdb").getInputStream()) {
            byte[] buff = in.readAllBytes();
            ipSearcher = Searcher.newWithBuffer(buff);
            log.info("ip2region 已加载,{} bytes", buff.length);
        } catch (Exception e) {
            log.warn("ip2region 加载失败,访问地区将显示未知:{}", e.getMessage());
        }
    }

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
        v.setRegion(resolveRegion(ip));
        repo.save(v);
    }

    /** 由 IP 解析地区(省·市 / 海外·国家 / 内网·未知),不落 IP 明文 */
    private String resolveRegion(String ip) {
        if (ip == null || ip.isBlank()) return "未知";
        if (ip.startsWith("127.") || ip.startsWith("192.168.") || ip.startsWith("10.")
                || ip.startsWith("172.16.") || ip.startsWith("::1") || ip.equalsIgnoreCase("localhost")) {
            return "内网";
        }
        if (ipSearcher == null) return "未知";
        try {
            String r;
            synchronized (this) { r = ipSearcher.search(ip); }
            if (r == null || r.isBlank()) return "未知";
            // 本 xdb 格式:国家|省份|城市|ISP|国家代码
            String[] p = r.split("\\|", -1);
            String country = geo(p, 0);
            String province = geo(p, 1);
            String city = geo(p, 2);
            if ("Reserved".equalsIgnoreCase(country) || "内网".equals(country)) return "内网";
            boolean cn = "中国".equals(country) || "0".equals(country) || country.isBlank();
            if (!cn) return "海外·" + country;
            boolean hasProv = has(province);
            boolean hasCity = has(city);
            if (!hasProv && !hasCity) return "未知";
            if (hasProv && hasCity) {
                String pr = geoRoot(province), cr = geoRoot(city);
                if (pr.equals(cr) || cr.contains(pr) || pr.contains(cr)) return city; // 直辖市等取城市
                return province + "·" + city;
            }
            return hasProv ? province : city;
        } catch (Exception e) {
            return "未知";
        }
    }

    private static String geo(String[] a, int i) { return i < a.length && a[i] != null ? a[i].trim() : ""; }
    private static boolean has(String s) { return s != null && !s.isBlank() && !"0".equals(s); }
    private static String geoRoot(String s) { return s.replaceAll("(省|市|自治区|特别行政区|地区|自治州)$", ""); }

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
        out.put("regions", buckets(repo.topRegions(from, top10)));
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
