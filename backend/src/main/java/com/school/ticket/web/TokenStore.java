package com.school.ticket.web;

import com.school.ticket.config.AppProperties;
import com.school.ticket.entity.AuthToken;
import com.school.ticket.repository.AuthTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * 登录令牌存储:落库 + 过期时间。
 * 服务重启后不会掉线;令牌超过 TTL(默认 7 天)自动失效,每小时清理过期项。
 */
@Component
@RequiredArgsConstructor
public class TokenStore {

    private static final Logger log = LoggerFactory.getLogger(TokenStore.class);

    public record Session(String username, String role) {}

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    private final AuthTokenRepository repo;
    private final AppProperties props;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public String issue(String username, String role) {
        byte[] buf = new byte[24];
        random.nextBytes(buf);
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) sb.append(String.format("%02x", b));
        String token = sb.toString();

        AuthToken t = new AuthToken();
        t.setToken(token);
        t.setUsername(username);
        t.setRole(role);
        int ttl = props.getTokenTtlDays() > 0 ? props.getTokenTtlDays() : 7;
        t.setExpiresAt(LocalDateTime.now().plusDays(ttl));
        repo.save(t);
        return token;
    }

    public boolean valid(String token) {
        return session(token) != null;
    }

    /** 返回会话;token 不存在或已过期返回 null(过期项由定时任务清理) */
    public Session session(String token) {
        if (token == null) return null;
        AuthToken t = repo.findById(token).orElse(null);
        if (t == null || t.getExpiresAt() == null || t.getExpiresAt().isBefore(LocalDateTime.now())) {
            return null;
        }
        return new Session(t.getUsername(), t.getRole());
    }

    public String usernameOf(String token) {
        Session s = session(token);
        return s == null ? null : s.username();
    }

    public String roleOf(String token) {
        Session s = session(token);
        return s == null ? null : s.role();
    }

    public String currentUsername(HttpServletRequest request) {
        return usernameOf(extractToken(request));
    }

    @Transactional
    public void revoke(String token) {
        if (token != null) repo.deleteById(token);
    }

    /** 踢掉某用户名(某角色)的所有 token */
    @Transactional
    public void revokeUser(String username, String role) {
        repo.deleteByUsernameAndRole(username, role);
    }

    /** 每小时清理已过期的 token */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeExpired() {
        long n = repo.deleteByExpiresAtBefore(LocalDateTime.now());
        if (n > 0) log.info("清理过期登录令牌 {} 条", n);
    }

    public static String extractToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        return (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : null;
    }
}
