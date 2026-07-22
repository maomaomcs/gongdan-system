package com.school.ticket.repository;

import com.school.ticket.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {

    Optional<Asset> findByAssetNo(String assetNo);

    boolean existsByAssetNo(String assetNo);

    // ---- 统计用查询 ----
    @Query("select a.type, count(a) from Asset a group by a.type order by count(a) desc")
    List<Object[]> countByType();

    @Query("select a.status, count(a) from Asset a group by a.status")
    List<Object[]> countByStatus();
}
