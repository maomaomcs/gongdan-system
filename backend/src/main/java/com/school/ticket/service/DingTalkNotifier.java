package com.school.ticket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.ticket.entity.Ticket;
import com.school.ticket.web.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 钉钉群机器人通知(markdown 卡片样式) */
@Slf4j
@Service
@RequiredArgsConstructor
public class DingTalkNotifier {

    private final SettingService settings;
    private final DingActionSigner signer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    // 单线程异步,避免通知失败/超时拖慢报修提交
    private final ExecutorService pool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ding-notify");
        t.setDaemon(true);
        return t;
    });

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // 品牌色
    private static final String C_RED = "#F5222D";
    private static final String C_BLUE = "#1677FF";
    private static final String C_ORANGE = "#FA8C16";
    private static final String C_GRAY = "#8C8C8C";

    /** 新工单通知(异步,不阻塞、不抛错) */
    public void notifyNewTicketAsync(Ticket t) {
        if (!settings.getBool(SettingService.DING_ENABLED)) return;
        pool.submit(() -> {
            try {
                boolean urgent = "紧急".equals(t.getUrgency());
                String title = (urgent ? "🔴 紧急报修" : "🔧 新报修工单") + " · " + t.getCode();
                String md = buildNewTicketMd(t);
                java.util.List<Map<String, String>> btns = buildActionButtons(t);
                if (btns != null) {
                    sendActionCard(title, md, btns);
                } else {
                    sendMarkdown(title, md);
                }
            } catch (Exception e) {
                log.warn("钉钉新工单通知发送失败: {}", e.getMessage());
            }
        });
    }

    /** 催单通知(异步) */
    public void notifyUrgeAsync(Ticket t) {
        if (!settings.getBool(SettingService.DING_ENABLED)) return;
        pool.submit(() -> {
            try {
                sendMarkdown("📣 催单提醒 · " + t.getCode(), buildUrgeMd(t));
            } catch (Exception e) {
                log.warn("钉钉催单通知发送失败: {}", e.getMessage());
            }
        });
    }

    /** 取消通知(异步) */
    public void notifyCancelAsync(Ticket t) {
        if (!settings.getBool(SettingService.DING_ENABLED)) return;
        pool.submit(() -> {
            try {
                sendMarkdown("🚫 报修已取消 · " + t.getCode(), buildCancelMd(t));
            } catch (Exception e) {
                log.warn("钉钉取消通知发送失败: {}", e.getMessage());
            }
        });
    }

    /** 发送测试消息(同步,返回结果,供后台"发送测试"按钮调用) */
    public void sendTest() {
        String md = header("✅ 通知测试", "#52C41A")
                + row("📌 系统", "石室联中 · 后勤报修")
                + row("📶 状态", "钉钉通知配置成功")
                + divider()
                + quote("看到本条消息说明配置正确,可以正常接收报修提醒了");
        sendMarkdown("✅ 钉钉通知测试", md);
    }

    // ---------- markdown 构建 ----------

    private String buildNewTicketMd(Ticket t) {
        boolean urgent = "紧急".equals(t.getUrgency());
        StringBuilder sb = new StringBuilder();
        sb.append(header(urgent ? "🔴 紧急报修工单" : "🔧 新报修工单", urgent ? C_RED : C_BLUE));
        sb.append(row("🎫 工单号", "`" + t.getCode() + "`"));
        sb.append(row("👤 报修人", reporterText(t)));
        sb.append(row("📍 位置", safe(t.getLocation())));
        sb.append(row("🏷️ 类型", safe(t.getCategory())));
        sb.append(row("🚦 紧急度", urgent ? "<font color=\"" + C_RED + "\">紧急</font>" : "普通"));
        sb.append(row("📝 问题", safe(t.getTitle())));
        if (StringUtils.hasText(t.getDescription())) {
            sb.append(row("💬 详情", safe(t.getDescription())));
        }
        sb.append(divider());
        sb.append(quote("🕐 " + t.getCreatedAt().format(TS) + " 提交 · 请及时处理"));
        return sb.toString();
    }

    private String buildUrgeMd(Ticket t) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("📣 催单提醒", C_ORANGE));
        sb.append(row("🎫 工单号", "`" + t.getCode() + "`"));
        sb.append(row("👤 报修人", reporterText(t)));
        sb.append(row("📍 位置", safe(t.getLocation())));
        sb.append(row("📝 问题", safe(t.getTitle())));
        sb.append(row("📊 当前状态", safe(t.getStatus())));
        sb.append(row("🔁 催单次数", "<font color=\"" + C_ORANGE + "\">第 " + (t.getUrgeCount() == null ? 1 : t.getUrgeCount()) + " 次</font>"));
        sb.append(divider());
        String since = t.getCreatedAt() == null ? "" : t.getCreatedAt().format(TS) + " 提交 · ";
        sb.append(quote("⚠️ " + since + "报修人正在催单,请尽快处理 🙏"));
        return sb.toString();
    }

    private String buildCancelMd(Ticket t) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("🚫 报修已取消", C_GRAY));
        sb.append(row("🎫 工单号", "`" + t.getCode() + "`"));
        sb.append(row("👤 报修人", reporterText(t)));
        sb.append(row("📍 位置", safe(t.getLocation())));
        sb.append(row("📝 问题", safe(t.getTitle())));
        sb.append(divider());
        sb.append(quote("报修人已取消该报修,无需再处理"));
        return sb.toString();
    }

    private String reporterText(Ticket t) {
        String r = safe(t.getReporter());
        if (StringUtils.hasText(t.getContact())) r += "（" + t.getContact() + "）";
        return r;
    }

    private String header(String title, String color) {
        return "### <font color=\"" + color + "\">" + title + "</font>\n\n";
    }

    private String row(String label, String value) {
        return "**" + label + "**：" + value + "\n\n";
    }

    private String divider() {
        return "\n---\n\n";
    }

    private String quote(String text) {
        return "> <font color=\"" + C_GRAY + "\">" + text + "</font>\n";
    }

    private String safe(String s) {
        if (s == null) return "";
        // 转义 markdown 里会干扰排版的字符
        return s.replace("\n", " ").replace("*", "\\*").replace("#", "\\#");
    }

    /** 生成群内操作按钮;未配置公网地址则返回 null(退回无按钮的普通卡片) */
    private java.util.List<Map<String, String>> buildActionButtons(Ticket t) {
        String base = settings.get(SettingService.DING_ACTION_BASE, "");
        if (!StringUtils.hasText(base)) return null;
        base = base.replaceAll("/+$", "");
        long id = t.getId();
        java.util.List<Map<String, String>> btns = new java.util.ArrayList<>();
        btns.add(btn("🙋 认领", base, id, "claim"));
        btns.add(btn("✅ 已解决", base, id, "resolve"));
        btns.add(btn("🚫 取消", base, id, "cancel"));
        return btns;
    }

    private Map<String, String> btn(String title, String base, long id, String action) {
        String url = base + "/api/ding/act?t=" + id + "&a=" + action + "&s=" + signer.sign(id, action);
        Map<String, String> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("actionURL", url);
        return m;
    }

    /** markdown 消息 */
    private void sendMarkdown(String title, String markdown) {
        String[] tt = withKeyword(title, markdown);
        Map<String, Object> md = new LinkedHashMap<>();
        md.put("title", tt[0]);
        md.put("text", tt[1]);
        post(Map.of("msgtype", "markdown", "markdown", md));
    }

    /** actionCard(带按钮)消息 */
    private void sendActionCard(String title, String markdown, java.util.List<Map<String, String>> btns) {
        String[] tt = withKeyword(title, markdown);
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("title", tt[0]);
        card.put("text", tt[1]);
        card.put("btnOrientation", "1"); // 按钮横排
        card.put("btns", btns);
        post(Map.of("msgtype", "actionCard", "actionCard", card));
    }

    /** 关键词安全设置:确保正文/标题包含关键词。返回 [title, text] */
    private String[] withKeyword(String title, String text) {
        String kw = settings.get(SettingService.DING_KEYWORD, "");
        if (StringUtils.hasText(kw)) {
            if (!text.contains(kw)) text = text + "\n\n<font color=\"#BFBFBF\">" + kw + "</font>";
            if (!title.contains(kw)) title = kw + " " + title;
        }
        return new String[]{title, text};
    }

    /** 实际发送:构造签名 URL + POST 消息体 */
    private void post(Map<String, Object> payload) {
        String webhook = settings.get(SettingService.DING_WEBHOOK, "");
        if (webhook.isEmpty()) throw new ApiException(400, "尚未配置钉钉 Webhook 地址");

        String url = webhook;
        String secret = settings.get(SettingService.DING_SECRET, "");
        if (!secret.isEmpty()) {
            long ts = System.currentTimeMillis();
            String sign = sign(ts, secret);
            String sep = webhook.contains("?") ? "&" : "?";
            url = webhook + sep + "timestamp=" + ts + "&sign=" + sign;
        }

        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            // 钉钉即使参数错误也返回 200,body 里 errcode!=0 表示失败
            String rb = resp.body();
            if (resp.statusCode() != 200 || (rb != null && rb.contains("\"errcode\":") && !rb.contains("\"errcode\":0"))) {
                throw new ApiException(400, "钉钉返回:" + rb);
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(400, "发送失败:" + e.getMessage());
        }
    }

    private String sign(long timestamp, String secret) {
        try {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return URLEncoder.encode(Base64.getEncoder().encodeToString(signData), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ApiException(500, "签名计算失败:" + e.getMessage());
        }
    }
}
