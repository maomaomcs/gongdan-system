package com.school.ticket.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** 分页响应 */
public record PageResponse<T>(
        List<T> list,
        long total,
        int page,
        int size,
        int pages
) {
    public static <T> PageResponse<T> of(Page<T> p) {
        return new PageResponse<>(p.getContent(), p.getTotalElements(), p.getNumber(), p.getSize(), p.getTotalPages());
    }
}
