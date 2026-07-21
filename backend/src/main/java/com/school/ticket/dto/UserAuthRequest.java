package com.school.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 用户注册/登录请求 */
public record UserAuthRequest(
        @NotBlank(message = "用户名不能为空") @Size(min = 3, max = 64, message = "用户名 3~64 位") String username,
        @NotBlank(message = "密码不能为空") @Size(min = 6, max = 64, message = "密码至少 6 位") String password,
        @Size(max = 64) String displayName,
        @Size(max = 64) String phone,
        // 注册时需要;登录时忽略
        @Size(max = 64) String inviteCode
) {}
