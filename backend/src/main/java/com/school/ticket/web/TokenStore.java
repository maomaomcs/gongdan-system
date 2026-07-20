package com.school.ticket.web;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** 简易内存 token 存储(重启后失效,足够内部使用) */
@Component
public class TokenStore {
    private final Set<String> tokens = ConcurrentHashMap.newKeySet();
    private final SecureRandom random = new SecureRandom();

    public String issue() {
        byte[] buf = new byte[24];
        random.nextBytes(buf);
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) sb.append(String.format("%02x", b));
        String token = sb.toString();
        tokens.add(token);
        return token;
    }

    public boolean valid(String token) {
        return token != null && tokens.contains(token);
    }

    public void revoke(String token) {
        tokens.remove(token);
    }
}
