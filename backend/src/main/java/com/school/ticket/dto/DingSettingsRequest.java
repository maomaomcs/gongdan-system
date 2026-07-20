package com.school.ticket.dto;

public record DingSettingsRequest(
        Boolean enabled,
        String webhook,
        String keyword,
        String secret  // 留空表示不修改已有 secret;传特殊值 "__CLEAR__" 表示清空
) {}
