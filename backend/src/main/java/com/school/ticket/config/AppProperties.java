package com.school.ticket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务配置(对应 application.yml 中的 app.*)
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    /** 报修图片存储目录 */
    private String uploadDir = "./data/uploads";
    /** 报修图片保留天数:超过则每日定时清理;<=0 表示不清理(仅压缩) */
    private int uploadRetentionDays = 15;
    /** 首次启动自动创建的初始管理员账号 */
    private String initAdminUsername = "admin";
    private String initAdminPassword = "admin123";
    private List<String> categories;
    private List<String> statuses;
    private List<String> urgencies;
}
