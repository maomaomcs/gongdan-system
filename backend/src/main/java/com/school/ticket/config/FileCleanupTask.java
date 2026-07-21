package com.school.ticket.config;

import com.school.ticket.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定时清理过期报修图片 */
@Component
@RequiredArgsConstructor
public class FileCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupTask.class);

    private final FileStorageService storage;
    private final AppProperties props;

    /** 每天凌晨 3:30 清理一次 */
    @Scheduled(cron = "0 30 3 * * *")
    public void cleanup() {
        int days = props.getUploadRetentionDays();
        if (days <= 0) return;
        int n = storage.cleanupOlderThan(days);
        if (n > 0) log.info("已清理过期报修图片 {} 张(保留最近 {} 天)", n, days);
    }
}
