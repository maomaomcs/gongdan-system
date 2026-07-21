package com.school.ticket.dto;

import com.school.ticket.entity.AppUser;

import java.time.LocalDateTime;

/** 老师账号(管理端查看) */
public record AppUserResponse(
        Long id,
        String username,
        String displayName,
        String phone,
        Boolean enabled,
        Boolean isAdmin,
        LocalDateTime createdAt
) {
    public static AppUserResponse from(AppUser u, boolean isAdmin) {
        return new AppUserResponse(
                u.getId(), u.getUsername(), u.getDisplayName(), u.getPhone(),
                u.getEnabled(), isAdmin, u.getCreatedAt());
    }
}
