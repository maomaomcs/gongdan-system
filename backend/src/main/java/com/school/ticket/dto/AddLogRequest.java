package com.school.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddLogRequest(
        @NotBlank(message = "内容不能为空") @Size(max = 500) String content,
        @Size(max = 64) String author
) {}
