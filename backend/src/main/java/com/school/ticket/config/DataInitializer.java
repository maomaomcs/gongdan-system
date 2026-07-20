package com.school.ticket.config;

import com.school.ticket.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** 首次启动时创建初始管理员账号 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AdminUserService adminUserService;
    private final AppProperties props;

    @Override
    public void run(String... args) {
        adminUserService.seedIfEmpty(props.getInitAdminUsername(), props.getInitAdminPassword());
    }
}
