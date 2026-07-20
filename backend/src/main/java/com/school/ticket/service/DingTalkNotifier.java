package com.school.ticket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.ticket.entity.Ticket;
import com.school.ticket.web.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 钉钉群机器人通知 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DingTalkNotifier {

    private final SettingService settings;
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

    /** 新工单通知(异步,不阻塞、不抛错) */
    public void notifyNewTicketAsync(Ticket t) {
        if (!settings.getBool(SettingService.DING_ENABLED)) return;
        pool.submit(() -> {
            try {
                send(buildNewTicketText(t));
            } catch (Exception e) {
                log.warn("钉钉通知发送失败: {}", e.getMessage());
            }
        });
    }

    /** 发送测试消息(同步,返回结果,供后台"发送测试"按钮调用) */
    public void sendTest() {
        String kw = settings.get(SettingService.DING_KEYWORD, "");
        String text = (kw.isEmpty() ? "" : kw + " ") + "【测试】校园报障工单系统 钉钉通知配置成功 ✅";
        send(text);
    }

    private String buildNewTicketText(Ticket t) {
        String kw = settings.get(SettingService.DING_KEYWORD, "");
        String urgentTag = "紧急".equals(t.getUrgency()) ? "🔴【紧急】" : "🔧";
        StringBuilder sb = new StringBuilder();
        if (!kw.isEmpty()) sb.append(kw).append(' '); // 关键词安全设置需包含在正文中
        sb.append(urgentTag).append("新报修工单\n");
        sb.append("工单号:").append(t.getCode()).append('\n');
        sb.append("报修人:").append(t.getReporter());
        if (t.getContact() != null && !t.getContact().isEmpty()) sb.append(" (").append(t.getContact()).append(')');
        sb.append('\n');
        sb.append("位置:").append(t.getLocation()).append('\n');
        sb.append("类型:").append(t.getCategory()).append('\n');
        sb.append("问题:").append(t.getTitle()).append('\n');
        if (t.getDescription() != null && !t.getDescription().isEmpty()) {
            sb.append("详情:").append(t.getDescription()).append('\n');
        }
        sb.append("时间:").append(t.getCreatedAt().format(TS));
        return sb.toString();
    }

    /** 实际发送:构造签名 URL + POST text 消息 */
    private void send(String text) {
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
            String body = objectMapper.writeValueAsString(Map.of(
                    "msgtype", "text",
                    "text", Map.of("content", text)
            ));
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
