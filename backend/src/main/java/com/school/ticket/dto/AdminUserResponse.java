package com.school.ticket.dto;

import com.school.ticket.entity.AdminUser;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String username,
        String displayName,
        Boolean enabled,
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(AdminUser u) {
        return new AdminUserResponse(u.getId(), u.getUsername(), u.getDisplayName(), u.getEnabled(), u.getCreatedAt());
    }
}
