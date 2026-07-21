package com.school.ticket.entity;

import jakarta.persistence.*;
import lombok.Data;

/** 系统设置(键值对) */
@Data
@Entity
@Table(name = "system_setting")
public class SystemSetting {

    @Id
    @Column(name = "setting_key", length = 64)
    private String key;

    @Column(name = "setting_value", length = 4000)
    private String value;
}
