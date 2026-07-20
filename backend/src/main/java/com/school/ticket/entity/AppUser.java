package com.school.ticket.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 报修用户(老师)账号 */
@Data
@Entity
@Table(name = "app_user", indexes = {
        @Index(name = "idx_app_username", columnList = "username", unique = true)
})
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    /** 姓名(报修时作为报修人) */
    @Column(length = 64)
    private String displayName;

    /** 联系方式 */
    @Column(length = 64)
    private String phone;

    @Column(nullable = false)
    private Boolean enabled = true;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (enabled == null) enabled = true;
    }
}
