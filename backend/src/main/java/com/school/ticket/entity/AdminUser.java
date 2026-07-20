package com.school.ticket.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 管理员账号 */
@Data
@Entity
@Table(name = "admin_user", indexes = {
        @Index(name = "idx_username", columnList = "username", unique = true)
})
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 登录用户名 */
    @Column(nullable = false, unique = true, length = 64)
    private String username;

    /** BCrypt 密码哈希 */
    @Column(nullable = false, length = 100)
    private String passwordHash;

    /** 显示名(用于记录处理人) */
    @Column(length = 64)
    private String displayName;

    /** 是否启用 */
    @Column(nullable = false)
    private Boolean enabled = true;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (enabled == null) enabled = true;
    }
}
