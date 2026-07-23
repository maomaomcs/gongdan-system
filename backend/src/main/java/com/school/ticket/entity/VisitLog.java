package com.school.ticket.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 前台访问日志(用于访问统计:UV/PV、来源、热门页、设备) */
@Data
@Entity
@Table(name = "visit_log", indexes = {
        @Index(name = "idx_visit_day", columnList = "day"),
        @Index(name = "idx_visit_visitor", columnList = "visitorHash")
})
public class VisitLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 访问日期(便于按天聚合) */
    @Column(nullable = false)
    private LocalDate day;

    /** 访问路径(前端路由,如 /report) */
    @Column(length = 300)
    private String path;

    /** 来源渠道:直接访问 / 站内 / 具体外部域名 */
    @Column(length = 120)
    private String refererDomain;

    /** 访问地区(由 IP 解析,如 四川省·成都市 / 北京市 / 海外;IP 本身不落库) */
    @Column(length = 80)
    private String region;

    /** 访客指纹:sha256(IP|UA|day) 前 32 位,当天去重用,IP 不落明文 */
    @Column(length = 40)
    private String visitorHash;

    /** 设备类型:mobile / desktop */
    @Column(length = 10)
    private String device;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
