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

    /** 导入模板列(与导入解析顺序一致;不含"保修状态",那是自动算的) */
    public static final String[] IMPORT_HEADERS = {
            "资产编号*", "类型*", "品牌型号", "序列号SN", "IP", "MAC", "位置",
            "责任人", "使用科室", "状态(在用/闲置/维修中/报废)", "购入日期(如2023-09-01)",
            "保修到期(如2026-08-15)", "供应商", "采购单号", "备注"
    };

    /** 生成批量导入模板(表头 + 一行示例) */
    public byte[] buildImportTemplate() {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("资产导入");

            CellStyle headStyle = wb.createCellStyle();
            Font headFont = wb.createFont();
            headFont.setBold(true);
            headFont.setColor(IndexedColors.WHITE.getIndex());
            headStyle.setFont(headFont);
            headStyle.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headStyle.setAlignment(HorizontalAlignment.CENTER);

            Row head = sheet.createRow(0);
            for (int i = 0; i < IMPORT_HEADERS.length; i++) {
                Cell c = head.createCell(i);
                c.setCellValue(IMPORT_HEADERS[i]);
                c.setCellStyle(headStyle);
            }

            // 示例行(带*的两列必填,其余选填)
            String[] sample = {"PC-教A-001", "台式电脑", "联想 M720", "SN123456", "10.0.1.21",
                    "AA:BB:CC:11:22:33", "教学楼A-201", "张老师", "信息中心", "在用",
                    "2023-09-01", "2026-08-15", "XX科技", "CG20230901", "首批采购"};
            Row ex = sheet.createRow(1);
            for (int i = 0; i < sample.length; i++) ex.createCell(i).setCellValue(sample[i]);

            int[] widths = {16, 12, 18, 16, 14, 18, 16, 10, 12, 22, 20, 20, 14, 14, 20};
            for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
            sheet.createFreezePane(0, 1);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new ApiException(500, "生成模板失败: " + e.getMessage());
        }
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
