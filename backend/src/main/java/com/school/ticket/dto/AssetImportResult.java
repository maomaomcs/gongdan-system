package com.school.ticket.dto;

import java.util.List;

/** 资产批量导入结果 */
public record AssetImportResult(
        int total,     // 数据总行数
        int success,   // 成功导入
        int skipped,   // 跳过(编号已存在)
        int failed,    // 失败(数据错误)
        List<String> errors // 失败/跳过的明细(第几行为什么)
) {}
