package com.school.ticket.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 内存 token 存储:token -> 会话信息(用户名 + 角色)。重启后失效,足够内部使用 */
@Component
public class TokenStore {

    public record Session(String username, String role) {}

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    private final Map<String, Session> tokens = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String issue(String username, String role) {
        byte[] buf = new byte[24];
        random.nextBytes(buf);
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) sb.append(String.format("%02x", b));
        String token = sb.toString();
        tokens.put(token, new Session(username, role));
        return token;
    }

    public boolean valid(String token) {
        return token != null && tokens.containsKey(token);
    }

    public Session session(String token) {
        return token == null ? null : tokens.get(token);
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

    public void revoke(String token) {
        if (token != null) tokens.remove(token);
    }

    /** 踢掉某用户名(某角色)的所有 token */
    public void revokeUser(String username, String role) {
        tokens.values().removeIf(s -> s.username().equals(username) && s.role().equals(role));
    }

    public static String extractToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        return (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : null;
    }
}
