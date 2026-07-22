package com.school.ticket.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单实体
 */
@Data
@Entity
@Table(name = "ticket", indexes = {
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_category", columnList = "category"),
        @Index(name = "idx_location", columnList = "location"),
        @Index(name = "idx_code", columnList = "code", unique = true)
})
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 工单号 BX20260720-XXXX */
    @Column(nullable = false, unique = true, length = 32)
    private String code;

    /** 报修人 */
    @Column(nullable = false, length = 64)
    private String reporter;

    /** 联系方式 */
    @Column(length = 64)
    private String contact;

    /** 位置/教室 */
    @Column(nullable = false, length = 128)
    private String location;

    /** 故障类型 */
    @Column(nullable = false, length = 32)
    private String category;

    /** 问题简述 */
    @Column(nullable = false, length = 128)
    private String title;

    /** 详细描述 */
    @Column(length = 1000)
    private String description;

    /** 紧急程度:普通/紧急 */
    @Column(nullable = false, length = 8)
    private String urgency = "普通";

    /** 状态:待处理/处理中/已解决/已关闭 */
    @Column(nullable = false, length = 8)
    private String status = "待处理";

    /** 处理人 */
    @Column(length = 64)
    private String handler;

    /** 解决方案/备注 */
    @Column(length = 1000)
    private String resolution;

    /** 报修图片文件名,逗号分隔 */
    @Column(length = 1000)
    private String images;

    /** 报修用户ID(关联 app_user) */
    private Long userId;

    /** 催单次数 */
    @Column(nullable = false)
    private Integer urgeCount = 0;

    /** 最近一次催单时间 */
    private LocalDateTime lastUrgedAt;

    /** 报修人是否有未读的进展(后勤更新状态/加跟进后置 true,老师查看详情后置 false) */
    @Column(nullable = false)
    private Boolean userUnread = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (urgeCount == null) urgeCount = 0;
        if (userUnread == null) userUnread = false;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
