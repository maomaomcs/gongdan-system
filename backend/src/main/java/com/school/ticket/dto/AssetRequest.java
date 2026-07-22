package com.school.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** 资产新增/编辑请求 */
public record AssetRequest(
        @NotBlank(message = "资产编号不能为空") @Size(max = 64) String assetNo,
        @NotBlank(message = "类型不能为空") @Size(max = 32) String type,
        @Size(max = 128) String brandModel,
        @Size(max = 128) String serialNo,
        @Size(max = 64) String ip,
        @Size(max = 64) String mac,
        @Size(max = 128) String location,
        @Size(max = 64) String owner,
        @Size(max = 64) String department,
        LocalDate purchaseDate,
        LocalDate warrantyEnd,
        @Size(max = 128) String supplier,
        @Size(max = 64) String purchaseOrder,
        @Size(max = 16) String status,
        @Size(max = 1000) String remark
) {}
