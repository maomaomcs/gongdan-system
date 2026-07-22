package com.school.ticket.service;

import com.school.ticket.dto.AssetImportResult;
import com.school.ticket.dto.AssetRequest;
import com.school.ticket.dto.AssetResponse;
import com.school.ticket.dto.PageResponse;
import com.school.ticket.dto.TicketResponse;
import com.school.ticket.entity.Asset;
import com.school.ticket.repository.AssetRepository;
import com.school.ticket.repository.TicketRepository;
import com.school.ticket.web.ApiException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepo;
    private final TicketRepository ticketRepo;

    // ---------- 列表(分页 + 多条件筛选) ----------
    @Transactional(readOnly = true)
    public PageResponse<AssetResponse> listPaged(String type, String status, String location,
                                                 String keyword, int page, int size) {
        Specification<Asset> spec = buildSpec(type, status, location, keyword);
        Page<Asset> p = assetRepo.findAll(spec, PageRequest.of(Math.max(page, 0), clampSize(size)));
        return PageResponse.of(p.map(AssetResponse::from));
    }

    // ---------- 全部(导出用,不分页) ----------
    @Transactional(readOnly = true)
    public List<AssetResponse> listAll(String type, String status, String location, String keyword) {
        Specification<Asset> spec = buildSpec(type, status, location, keyword);
        return assetRepo.findAll(spec).stream().map(AssetResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AssetResponse getById(Long id) {
        return AssetResponse.from(find(id));
    }

    // ---------- 新增 ----------
    @Transactional
    public AssetResponse create(AssetRequest req) {
        String no = req.assetNo().trim();
        if (assetRepo.existsByAssetNo(no)) {
            throw new ApiException(400, "资产编号已存在:" + no);
        }
        Asset a = new Asset();
        a.setAssetNo(no);
        apply(a, req);
        assetRepo.save(a);
        return AssetResponse.from(a);
    }

    // ---------- 编辑 ----------
    @Transactional
    public AssetResponse update(Long id, AssetRequest req) {
        Asset a = find(id);
        String no = req.assetNo().trim();
        if (!no.equals(a.getAssetNo()) && assetRepo.existsByAssetNo(no)) {
            throw new ApiException(400, "资产编号已存在:" + no);
        }
        a.setAssetNo(no);
        apply(a, req);
        assetRepo.save(a);
        return AssetResponse.from(a);
    }

    // ---------- 删除 ----------
    @Transactional
    public void delete(Long id) {
        if (!assetRepo.existsById(id)) throw new ApiException(404, "资产不存在");
        assetRepo.deleteById(id);
    }

    private void apply(Asset a, AssetRequest req) {
        a.setType(req.type().trim());
        a.setBrandModel(trimOrNull(req.brandModel()));
        a.setSerialNo(trimOrNull(req.serialNo()));
        a.setIp(trimOrNull(req.ip()));
        a.setMac(trimOrNull(req.mac()));
        a.setLocation(trimOrNull(req.location()));
        a.setOwner(trimOrNull(req.owner()));
        a.setDepartment(trimOrNull(req.department()));
        a.setPurchaseDate(req.purchaseDate());
        a.setWarrantyEnd(req.warrantyEnd());
        a.setSupplier(trimOrNull(req.supplier()));
        a.setPurchaseOrder(trimOrNull(req.purchaseOrder()));
        String status = trimOrNull(req.status());
        a.setStatus(status == null ? "在用" : status);
        a.setRemark(trimOrNull(req.remark()));
    }

    private Asset find(Long id) {
        return assetRepo.findById(id).orElseThrow(() -> new ApiException(404, "资产不存在"));
    }

    // ---------- 设备关联的报障工单历史 ----------
    @Transactional(readOnly = true)
    public Map<String, Object> relatedTickets(Long id) {
        Asset a = find(id);
        List<TicketResponse> tickets = ticketRepo.findByAssetNoOrderByIdDesc(a.getAssetNo())
                .stream().map(TicketResponse::from).toList();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("assetNo", a.getAssetNo());
        out.put("count", tickets.size());
        out.put("tickets", tickets);
        return out;
    }

    // ---------- 批量导入(Excel) ----------
    @Transactional
    public AssetImportResult importFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ApiException(400, "请上传 Excel 文件");
        List<String> errors = new ArrayList<>();
        int total = 0, success = 0, skipped = 0, failed = 0;
        DataFormatter fmt = new DataFormatter();
        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();
            for (int r = 1; r <= lastRow; r++) { // 第 0 行为表头
                Row row = sheet.getRow(r);
                if (row == null || isEmptyRow(row, fmt)) continue;
                total++;
                int excelRow = r + 1;
                String assetNo = cellStr(row, 0, fmt);
                String type = cellStr(row, 1, fmt);
                if (assetNo == null) { failed++; errors.add("第" + excelRow + "行:资产编号为空,已跳过"); continue; }
                if (type == null) { failed++; errors.add("第" + excelRow + "行(" + assetNo + "):类型为空,已跳过"); continue; }
                if (assetRepo.existsByAssetNo(assetNo)) { skipped++; errors.add("第" + excelRow + "行(" + assetNo + "):编号已存在,已跳过"); continue; }
                try {
                    Asset a = new Asset();
                    a.setAssetNo(assetNo);
                    a.setType(type);
                    a.setBrandModel(cellStr(row, 2, fmt));
                    a.setSerialNo(cellStr(row, 3, fmt));
                    a.setIp(cellStr(row, 4, fmt));
                    a.setMac(cellStr(row, 5, fmt));
                    a.setLocation(cellStr(row, 6, fmt));
                    a.setOwner(cellStr(row, 7, fmt));
                    a.setDepartment(cellStr(row, 8, fmt));
                    String status = cellStr(row, 9, fmt);
                    a.setStatus(status == null ? "在用" : status);
                    a.setPurchaseDate(cellDate(row, 10, fmt));
                    a.setWarrantyEnd(cellDate(row, 11, fmt));
                    a.setSupplier(cellStr(row, 12, fmt));
                    a.setPurchaseOrder(cellStr(row, 13, fmt));
                    a.setRemark(cellStr(row, 14, fmt));
                    assetRepo.save(a);
                    success++;
                } catch (Exception ex) {
                    failed++;
                    errors.add("第" + excelRow + "行(" + assetNo + "):" + ex.getMessage());
                }
            }
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            throw new ApiException(400, "解析 Excel 失败,请使用下载的模板填写:" + e.getMessage());
        }
        return new AssetImportResult(total, success, skipped, failed, errors);
    }

    private boolean isEmptyRow(Row row, DataFormatter fmt) {
        for (int i = 0; i <= 14; i++) {
            if (cellStr(row, i, fmt) != null) return false;
        }
        return true;
    }

    private String cellStr(Row row, int i, DataFormatter fmt) {
        Cell c = row.getCell(i);
        if (c == null) return null;
        String s = fmt.formatCellValue(c).trim();
        return s.isEmpty() ? null : s;
    }

    private LocalDate cellDate(Row row, int i, DataFormatter fmt) {
        Cell c = row.getCell(i);
        if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
            return c.getLocalDateTimeCellValue().toLocalDate();
        }
        String s = fmt.formatCellValue(c).trim();
        if (s.isEmpty()) return null;
        s = s.replace('/', '-').replace('.', '-');
        try {
            return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-M-d"));
        } catch (Exception e) {
            return null; // 日期格式不对就留空,不因此整行失败
        }
    }

    private Specification<Asset> buildSpec(String type, String status, String location, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (StringUtils.hasText(type)) ps.add(cb.equal(root.get("type"), type));
            if (StringUtils.hasText(status)) ps.add(cb.equal(root.get("status"), status));
            if (StringUtils.hasText(location)) ps.add(cb.like(root.get("location"), "%" + location + "%"));
            if (StringUtils.hasText(keyword)) {
                String kw = "%" + keyword + "%";
                ps.add(cb.or(
                        cb.like(root.get("assetNo"), kw),
                        cb.like(root.get("brandModel"), kw),
                        cb.like(root.get("serialNo"), kw),
                        cb.like(root.get("ip"), kw),
                        cb.like(root.get("mac"), kw),
                        cb.like(root.get("owner"), kw),
                        cb.like(root.get("department"), kw),
                        cb.like(root.get("location"), kw)
                ));
            }
            if (Asset.class.equals(query.getResultType())) {
                query.orderBy(cb.desc(root.get("id")));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private int clampSize(int size) {
        if (size <= 0) return 20;
        return Math.min(size, 200);
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
