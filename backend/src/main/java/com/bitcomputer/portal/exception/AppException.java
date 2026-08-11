package com.bitcomputer.portal.exception;

import org.springframework.http.HttpStatus;

public class AppException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Integer retryAfter;

    public AppException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public AppException(HttpStatus status, String code, String message, Integer retryAfter) {
        super(message);
        this.status = status;
        this.code = code;
        this.retryAfter = retryAfter;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
    public Integer retryAfter() { return retryAfter; }
}
