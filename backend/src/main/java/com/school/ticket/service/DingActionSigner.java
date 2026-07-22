package com.school.ticket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * 钉钉群按钮链接的签名器。
 * 用一个只存在服务端的密钥对 "ticketId:action" 做 HMAC,防止有人篡改/伪造链接操作别的工单。
 * (群机器人按钮拿不到点击人身份,签名仅保证链接是本系统生成的、且对应特定工单+动作)
 */
@Component
@RequiredArgsConstructor
public class DingActionSigner {

    private final SettingService settings;
    public static final String KEY = "ding.action-secret";

    /** 取密钥;没有则生成一个随机的并落库 */
    private synchronized String secret() {
        String s = settings.get(KEY, "");
        if (s == null || s.isEmpty()) {
            byte[] buf = new byte[24];
            new SecureRandom().nextBytes(buf);
            StringBuilder sb = new StringBuilder();
            for (byte b : buf) sb.append(String.format("%02x", b));
            s = sb.toString();
            settings.set(KEY, s);
        }
        return s;
    }

    public String sign(long ticketId, String action) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] d = mac.doFinal((ticketId + ":" + action).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.substring(0, 20); // 取前20位足够防猜
        } catch (Exception e) {
            throw new RuntimeException("签名失败: " + e.getMessage(), e);
        }
    }

    public boolean verify(long ticketId, String action, String sig) {
        if (sig == null) return false;
        String expect = sign(ticketId, action);
        return MessageDigest.isEqual(expect.getBytes(StandardCharsets.UTF_8), sig.getBytes(StandardCharsets.UTF_8));
    }
}
