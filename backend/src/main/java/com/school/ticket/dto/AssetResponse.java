package com.school.ticket.dto;

import com.school.ticket.entity.Asset;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record AssetResponse(
        Long id,
        String assetNo,
        String type,
        String brandModel,
        String serialNo,
        String ip,
        String mac,
        String location,
        String owner,
        String department,
        LocalDate purchaseDate,
        LocalDate warrantyEnd,
        String supplier,
        String purchaseOrder,
        String status,
        String remark,
        /** 距保修到期天数(负数=已过保;null=未填保修日期) */
        Long warrantyDays,
        /** 保修状态:有效/即将到期/已过保/未填 */
        String warrantyState,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /** 即将到期阈值(天) */
    private static final long SOON_DAYS = 30;

    public static AssetResponse from(Asset a) {
        Long days = null;
        String state = "未填";
        if (a.getWarrantyEnd() != null) {
            days = ChronoUnit.DAYS.between(LocalDate.now(), a.getWarrantyEnd());
            if (days < 0) state = "已过保";
            else if (days <= SOON_DAYS) state = "即将到期";
            else state = "有效";
        }
        return new AssetResponse(
                a.getId(), a.getAssetNo(), a.getType(), a.getBrandModel(), a.getSerialNo(),
                a.getIp(), a.getMac(), a.getLocation(), a.getOwner(), a.getDepartment(),
                a.getPurchaseDate(), a.getWarrantyEnd(), a.getSupplier(), a.getPurchaseOrder(),
                a.getStatus(), a.getRemark(), days, state,
                a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}
