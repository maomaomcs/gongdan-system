package com.school.ticket.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * IT 资产台账实体(CMDB):电脑/打印机/一体机/交换机等设备
 */
@Data
@Entity
@Table(name = "asset", indexes = {
        @Index(name = "idx_asset_no", columnList = "assetNo", unique = true),
        @Index(name = "idx_asset_type", columnList = "type"),
        @Index(name = "idx_asset_status", columnList = "status"),
        @Index(name = "idx_asset_location", columnList = "location")
})
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 资产编号(唯一),如 PC-教学楼A-001 */
    @Column(nullable = false, unique = true, length = 64)
    private String assetNo;

    /** 类型:台式电脑/笔记本/打印机/一体机/交换机/投影仪/服务器/其他 */
    @Column(nullable = false, length = 32)
    private String type;

    /** 品牌型号 */
    @Column(length = 128)
    private String brandModel;

    /** 序列号 SN */
    @Column(length = 128)
    private String serialNo;

    /** IP 地址 */
    @Column(length = 64)
    private String ip;

    /** MAC 地址 */
    @Column(length = 64)
    private String mac;

    /** 位置(楼栋-房间) */
    @Column(length = 128)
    private String location;

    /** 责任人 */
    @Column(length = 64)
    private String owner;

    /** 使用科室/部门 */
    @Column(length = 64)
    private String department;

    /** 购入日期 */
    private LocalDate purchaseDate;

    /** 保修到期日期 */
    private LocalDate warrantyEnd;

    /** 供应商 */
    @Column(length = 128)
    private String supplier;

    /** 采购单号 */
    @Column(length = 64)
    private String purchaseOrder;

    /** 状态:在用/闲置/维修中/报废 */
    @Column(nullable = false, length = 16)
    private String status = "在用";

    /** 备注(可写关联工单号等) */
    @Column(length = 1000)
    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (status == null || status.isBlank()) status = "在用";
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
