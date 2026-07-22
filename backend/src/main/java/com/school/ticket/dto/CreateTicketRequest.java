package com.school.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank(message = "报修人不能为空") @Size(max = 64) String reporter,
        @Size(max = 64) String contact,
        @NotBlank(message = "位置不能为空") @Size(max = 128) String location,
        @Size(max = 64) String assetNo,
        @NotBlank(message = "故障类型不能为空") @Size(max = 32) String category,
        @NotBlank(message = "问题简述不能为空") @Size(max = 128) String title,
        @Size(max = 1000) String description,
        String urgency,
        java.util.List<String> images
) {}
