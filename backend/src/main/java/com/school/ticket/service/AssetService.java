package com.school.ticket.service;

import com.school.ticket.dto.AssetRequest;
import com.school.ticket.dto.AssetResponse;
import com.school.ticket.dto.PageResponse;
import com.school.ticket.entity.Asset;
import com.school.ticket.repository.AssetRepository;
import com.school.ticket.web.ApiException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepo;

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
