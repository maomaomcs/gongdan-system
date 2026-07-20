package com.school.ticket.web;

import lombok.Getter;

/** 业务异常,携带 HTTP 状态码 */
@Getter
public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }
}
