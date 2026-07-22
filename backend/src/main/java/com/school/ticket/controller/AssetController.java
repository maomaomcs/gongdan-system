package com.school.ticket.controller;

import com.school.ticket.dto.AssetImportResult;
import com.school.ticket.dto.AssetRequest;
import com.school.ticket.dto.AssetResponse;
import com.school.ticket.dto.PageResponse;
import com.school.ticket.repository.AssetRepository;
import com.school.ticket.service.AssetExcelExportService;
import com.school.ticket.service.AssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * IT 资产台账后台接口(需管理员登录):列表、详情、增删改、统计、导出
 */
@RestController
@RequestMapping("/api/admin/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final AssetExcelExportService excelExportService;
    private final AssetRepository assetRepo;

    @GetMapping
    public PageResponse<AssetResponse> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false, name = "q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return assetService.listPaged(type, status, location, keyword, page, size);
    }

    @GetMapping("/{id}")
    public AssetResponse detail(@PathVariable Long id) {
        return assetService.getById(id);
    }

    /** 某台设备的报障工单历史(按资产编号关联) */
    @GetMapping("/{id}/tickets")
    public Map<String, Object> relatedTickets(@PathVariable Long id) {
        return assetService.relatedTickets(id);
    }

    @PostMapping
    public AssetResponse create(@Valid @RequestBody AssetRequest req) {
        return assetService.create(req);
    }

    @PutMapping("/{id}")
    public AssetResponse update(@PathVariable Long id, @Valid @RequestBody AssetRequest req) {
        return assetService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        assetService.delete(id);
        return Map.of("ok", true);
    }

    /** 资产统计:总数 + 按类型/状态分布 */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("total", assetRepo.count());
        out.put("byType", toBuckets(assetRepo.countByType()));
        out.put("byStatus", toBuckets(assetRepo.countByStatus()));
        return out;
    }

    private List<Map<String, Object>> toBuckets(List<Object[]> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("name", r[0] == null ? "(未填)" : r[0].toString());
            m.put("count", ((Number) r[1]).longValue());
            out.add(m);
        }
        return out;
    }

    /** 下载批量导入模板 */
    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        byte[] data = excelExportService.buildImportTemplate();
        String filename = "资产导入模板.xlsx";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"asset-template.xlsx\"; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    /** 批量导入资产(上传按模板填好的 Excel) */
    @PostMapping("/import")
    public AssetImportResult importExcel(@RequestParam("file") MultipartFile file) {
        return assetService.importFromExcel(file);
    }

    /** 导出资产台账 Excel(支持与列表相同的筛选条件) */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false, name = "q") String keyword) {
        List<AssetResponse> list = assetService.listAll(type, status, location, keyword);
        byte[] data = excelExportService.exportAssets(list);
        String filename = "IT资产台账_" + LocalDate.now() + ".xlsx";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"assets.xlsx\"; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
