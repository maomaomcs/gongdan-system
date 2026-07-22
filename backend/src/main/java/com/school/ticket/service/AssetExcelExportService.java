package com.school.ticket.service;

import com.school.ticket.dto.AssetResponse;
import com.school.ticket.web.ApiException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** IT 资产台账导出 Excel(.xlsx) */
@Service
public class AssetExcelExportService {

    private static final String[] HEADERS = {
            "资产编号", "类型", "品牌型号", "序列号SN", "IP", "MAC", "位置",
            "责任人", "使用科室", "状态", "购入日期", "保修到期", "保修状态",
            "供应商", "采购单号", "备注"
    };
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public byte[] exportAssets(List<AssetResponse> list) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("IT资产台账");

            CellStyle headStyle = wb.createCellStyle();
            Font headFont = wb.createFont();
            headFont.setBold(true);
            headFont.setColor(IndexedColors.WHITE.getIndex());
            headStyle.setFont(headFont);
            headStyle.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headStyle.setAlignment(HorizontalAlignment.CENTER);

            Row head = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = head.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(headStyle);
            }

            int r = 1;
            for (AssetResponse a : list) {
                Row row = sheet.createRow(r++);
                int i = 0;
                row.createCell(i++).setCellValue(nz(a.assetNo()));
                row.createCell(i++).setCellValue(nz(a.type()));
                row.createCell(i++).setCellValue(nz(a.brandModel()));
                row.createCell(i++).setCellValue(nz(a.serialNo()));
                row.createCell(i++).setCellValue(nz(a.ip()));
                row.createCell(i++).setCellValue(nz(a.mac()));
                row.createCell(i++).setCellValue(nz(a.location()));
                row.createCell(i++).setCellValue(nz(a.owner()));
                row.createCell(i++).setCellValue(nz(a.department()));
                row.createCell(i++).setCellValue(nz(a.status()));
                row.createCell(i++).setCellValue(a.purchaseDate() == null ? "" : a.purchaseDate().format(DATE));
                row.createCell(i++).setCellValue(a.warrantyEnd() == null ? "" : a.warrantyEnd().format(DATE));
                row.createCell(i++).setCellValue(nz(a.warrantyState()));
                row.createCell(i++).setCellValue(nz(a.supplier()));
                row.createCell(i++).setCellValue(nz(a.purchaseOrder()));
                row.createCell(i++).setCellValue(nz(a.remark()));
            }

            int[] widths = {18, 12, 20, 18, 15, 16, 18, 10, 12, 8, 12, 12, 10, 16, 14, 24};
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
