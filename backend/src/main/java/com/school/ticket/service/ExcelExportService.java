package com.school.ticket.service;

import com.school.ticket.dto.TicketResponse;
import com.school.ticket.web.ApiException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 工单导出 Excel(.xlsx) */
@Service
public class ExcelExportService {

    private static final String[] HEADERS = {
            "工单号", "状态", "紧急度", "报修人", "联系方式", "位置/教室",
            "故障类型", "问题简述", "详细描述", "处理人", "解决方案",
            "图片数", "提交时间", "解决时间"
    };
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] exportTickets(List<TicketResponse> list) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("报修工单");

            // 表头样式
            CellStyle headStyle = wb.createCellStyle();
            Font headFont = wb.createFont();
            headFont.setBold(true);
            headFont.setColor(IndexedColors.WHITE.getIndex());
            headStyle.setFont(headFont);
            headStyle.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headStyle.setAlignment(HorizontalAlignment.CENTER);

            Row head = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = head.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(headStyle);
            }

            int r = 1;
            for (TicketResponse t : list) {
                Row row = sheet.createRow(r++);
                int i = 0;
                row.createCell(i++).setCellValue(nz(t.code()));
                row.createCell(i++).setCellValue(nz(t.status()));
                row.createCell(i++).setCellValue(nz(t.urgency()));
                row.createCell(i++).setCellValue(nz(t.reporter()));
                row.createCell(i++).setCellValue(nz(t.contact()));
                row.createCell(i++).setCellValue(nz(t.location()));
                row.createCell(i++).setCellValue(nz(t.category()));
                row.createCell(i++).setCellValue(nz(t.title()));
                row.createCell(i++).setCellValue(nz(t.description()));
                row.createCell(i++).setCellValue(nz(t.handler()));
                row.createCell(i++).setCellValue(nz(t.resolution()));
                row.createCell(i++).setCellValue(t.images() == null ? 0 : t.images().size());
                row.createCell(i++).setCellValue(t.createdAt() == null ? "" : t.createdAt().format(FMT));
                row.createCell(i++).setCellValue(t.resolvedAt() == null ? "" : t.resolvedAt().format(FMT));
            }

            // 列宽
            int[] widths = {16, 8, 8, 10, 14, 20, 14, 22, 30, 10, 24, 7, 18, 18};
            for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
            sheet.createFreezePane(0, 1);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new ApiException(500, "导出失败: " + e.getMessage());
        }
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
