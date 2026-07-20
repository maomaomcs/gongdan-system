package com.school.ticket.dto;

import java.util.List;

public record StatsResponse(
        long total,
        long open,
        Double avgHours,
        List<Bucket> byStatus,
        List<Bucket> byCategory,
        List<Bucket> byLocation,
        List<Bucket> byMonth
) {
    public record Bucket(String name, long count) {}
}
