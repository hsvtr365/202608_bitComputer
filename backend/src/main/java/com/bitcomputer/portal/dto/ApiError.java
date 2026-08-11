package com.bitcomputer.portal.dto;

import java.time.Instant;

public record ApiError(String code, String message, Integer retryAfter, Instant timestamp) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null, Instant.now());
    }

    public static ApiError retry(String code, String message, Integer retryAfter) {
        return new ApiError(code, message, retryAfter, Instant.now());
    }
}
