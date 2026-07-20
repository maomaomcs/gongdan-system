package com.school.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "密码不能为空") String password
) {}
