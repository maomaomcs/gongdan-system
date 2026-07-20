package com.school.ticket.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 简易内存 token 存储:token -> 用户名(重启后失效,足够内部使用) */
@Component
public class TokenStore {
    private final Map<String, String> tokenToUser = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String issue(String username) {
        byte[] buf = new byte[24];
        random.nextBytes(buf);
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) sb.append(String.format("%02x", b));
        String token = sb.toString();
        tokenToUser.put(token, username);
        return token;
    }

    public boolean valid(String token) {
        return token != null && tokenToUser.containsKey(token);
    }

    public String usernameOf(String token) {
        return token == null ? null : tokenToUser.get(token);
    }

    /** 从请求头取出当前登录用户名(拦截器已保证 token 有效) */
    public String currentUsername(HttpServletRequest request) {
        return usernameOf(extractToken(request));
    }

    public void revoke(String token) {
        if (token != null) tokenToUser.remove(token);
    }

    /** 某用户改密码/被停用后,踢掉其所有已签发 token */
    public void revokeUser(String username) {
        tokenToUser.values().removeIf(u -> u.equals(username));
    }

    /** 从 Authorization: Bearer xxx 提取 token */
    public static String extractToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        return (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : null;
    }
}
