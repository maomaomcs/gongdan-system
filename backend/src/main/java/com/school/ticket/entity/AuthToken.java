package com.school.ticket.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 登录令牌(落库,支持过期,重启不掉线) */
@Data
@Entity
@Table(name = "auth_token", indexes = {
        @Index(name = "idx_token_user_role", columnList = "username,role")
})
public class AuthToken {

    @Id
    @Column(length = 64)
    private String token;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 16)
    private String role;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
