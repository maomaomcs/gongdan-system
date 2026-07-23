package com.school.ticket.repository;

import com.school.ticket.entity.VisitLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VisitLogRepository extends JpaRepository<VisitLog, Long> {

    long countByDay(LocalDate day);

    @Query("select count(distinct v.visitorHash) from VisitLog v where v.day = :day")
    long uvOfDay(@Param("day") LocalDate day);

    @Query("select v.day, count(v), count(distinct v.visitorHash) from VisitLog v " +
            "where v.day >= :from group by v.day order by v.day")
    List<Object[]> trend(@Param("from") LocalDate from);

    @Query("select v.refererDomain, count(v) from VisitLog v where v.day >= :from " +
            "group by v.refererDomain order by count(v) desc")
    List<Object[]> topReferers(@Param("from") LocalDate from, Pageable pageable);

    @Query("select v.path, count(v) from VisitLog v where v.day >= :from " +
            "group by v.path order by count(v) desc")
    List<Object[]> topPages(@Param("from") LocalDate from, Pageable pageable);

    @Query("select v.device, count(v) from VisitLog v where v.day >= :from group by v.device")
    List<Object[]> deviceSplit(@Param("from") LocalDate from);

    @Query("select v.region, count(v) from VisitLog v where v.day >= :from " +
            "group by v.region order by count(v) desc")
    List<Object[]> topRegions(@Param("from") LocalDate from, Pageable pageable);

    long countByDayGreaterThanEqual(LocalDate from);

    @Query("select count(distinct v.visitorHash) from VisitLog v where v.day >= :from")
    long uvSince(@Param("from") LocalDate from);
}
