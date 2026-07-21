package com.school.ticket.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 注册邀请码(校验码):由管理端生成,老师注册时需填写 */
@Data
@Entity
@Table(name = "invite_code", indexes = {
        @Index(name = "idx_invite_code", columnList = "code", unique = true)
})
public class InviteCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    /** 备注(如:发给哪个年级/批次) */
    @Column(length = 128)
    private String note;

    /** 最大可用次数;0 表示不限次数 */
    @Column(nullable = false)
    private Integer maxUses = 0;

    /** 已使用次数 */
    @Column(nullable = false)
    private Integer usedCount = 0;

    @Column(nullable = false)
    private Boolean enabled = true;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (maxUses == null) maxUses = 0;
        if (usedCount == null) usedCount = 0;
        if (enabled == null) enabled = true;
    }
}
