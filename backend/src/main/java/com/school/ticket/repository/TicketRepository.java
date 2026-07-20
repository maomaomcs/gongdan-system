package com.school.ticket.repository;

import com.school.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    Optional<Ticket> findByCode(String code);

    // ---- 统计用查询 ----
    @Query("select t.status, count(t) from Ticket t group by t.status")
    List<Object[]> countByStatus();

    @Query("select t.category, count(t) from Ticket t group by t.category order by count(t) desc")
    List<Object[]> countByCategory();

    @Query("select t.location, count(t) from Ticket t group by t.location order by count(t) desc")
    List<Object[]> countByLocation();

    @Query(value = "select date_format(created_at, '%Y-%m') ym, count(*) c from ticket " +
            "group by ym order by ym", nativeQuery = true)
    List<Object[]> countByMonth();

    long countByStatusIn(List<String> statuses);

    @Query(value = "select avg(timestampdiff(second, created_at, resolved_at)) from ticket " +
            "where resolved_at is not null", nativeQuery = true)
    Double avgResolveSeconds();
}
