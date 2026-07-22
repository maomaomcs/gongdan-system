package com.school.ticket.dto;

public record DingSettingsRequest(
        Boolean enabled,
        String webhook,
        String keyword,
        String secret,  // 留空表示不修改已有 secret;传特殊值 "__CLEAR__" 表示清空
        String actionBase  // 群内按钮回调用的公网地址,如 http://43.136.56.131:8082(留空则不显示按钮)
) {}
